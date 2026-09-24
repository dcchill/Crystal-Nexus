package net.crystalnexus.gametest;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class EnergyCableGameTests {
    private EnergyCableGameTests() {}

    @GameTest(template = "zero_point")
    public static void allTiersRouteWithoutStorage(GameTestHelper helper) {
        assertPointToPoint(helper, CrystalnexusModBlocks.BASIC_ENERGY_CABLE.get(), 1, 2_048);
        assertPointToPoint(helper, CrystalnexusModBlocks.ENERGY_CABLE_MK_2.get(), 4, 131_072);
        assertPointToPoint(helper, CrystalnexusModBlocks.HYPER_ENERGY_CABLE.get(), 7, 1_048_576);
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void routesToClosestReceiverFirst(GameTestHelper helper) {
        BlockPos cablePos = new BlockPos(2, 4, 2);
        BlockPos nearSinkPos = cablePos.north();
        BlockPos secondCablePos = cablePos.east();
        BlockPos farSinkPos = secondCablePos.east();
        helper.setBlock(cablePos, CrystalnexusModBlocks.ENERGY_CABLE_MK_2.get());
        helper.setBlock(nearSinkPos, CrystalnexusModBlocks.TESSERACT.get());
        helper.setBlock(secondCablePos, CrystalnexusModBlocks.ENERGY_CABLE_MK_2.get());
        helper.setBlock(farSinkPos, CrystalnexusModBlocks.TESSERACT.get());

        IEnergyStorage link = energy(helper, cablePos, Direction.WEST);
        IEnergyStorage nearSink = energy(helper, nearSinkPos, Direction.SOUTH);
        IEnergyStorage farSink = energy(helper, farSinkPos, Direction.WEST);
        int accepted = link.receiveEnergy(1_000, false);
        helper.assertTrue(accepted == 1_000
                && nearSink.getEnergyStored() == 1_000 && farSink.getEnergyStored() == 0,
            "The closest accepting endpoint must receive energy first");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void mixedNetworkUsesLowestTierRate(GameTestHelper helper) {
        BlockPos hyperPos = new BlockPos(1, 4, 1);
        BlockPos basicPos = hyperPos.east();
        BlockPos sinkPos = basicPos.east();
        helper.setBlock(hyperPos, CrystalnexusModBlocks.HYPER_ENERGY_CABLE.get());
        helper.setBlock(basicPos, CrystalnexusModBlocks.BASIC_ENERGY_CABLE.get());
        helper.setBlock(sinkPos, CrystalnexusModBlocks.TESSERACT.get());

        IEnergyStorage link = energy(helper, hyperPos, Direction.WEST);
        IEnergyStorage sink = energy(helper, sinkPos, Direction.WEST);
        int accepted = link.receiveEnergy(100_000, false);
        helper.assertTrue(accepted == 2_048 && sink.getEnergyStored() == 2_048,
            "A mixed cable route must be limited by its slowest tier");
        helper.succeed();
    }

    private static void assertPointToPoint(GameTestHelper helper, Block cableBlock, int x, int amount) {
        BlockPos cablePos = new BlockPos(x, 4, 6);
        BlockPos sinkPos = cablePos.east();
        helper.setBlock(cablePos, cableBlock);
        helper.setBlock(sinkPos, CrystalnexusModBlocks.TESSERACT.get());
        IEnergyStorage link = energy(helper, cablePos, Direction.WEST);
        IEnergyStorage sink = energy(helper, sinkPos, Direction.WEST);

        int accepted = link.receiveEnergy(amount, false);
        helper.assertTrue(accepted == amount && link.getEnergyStored() == 0
                && link.getMaxEnergyStored() == 0 && sink.getEnergyStored() == amount,
            "A cable must deliver instantly at its tier rate without buffering energy");
    }

    private static IEnergyStorage energy(GameTestHelper helper, BlockPos pos, Direction side) {
        IEnergyStorage storage = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
            helper.absolutePos(pos), side);
        helper.assertTrue(storage != null, "Expected an energy capability at " + pos);
        return storage;
    }
}
