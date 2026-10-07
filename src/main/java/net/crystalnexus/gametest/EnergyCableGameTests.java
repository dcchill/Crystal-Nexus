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
import net.crystalnexus.block.entity.EnergyCableNetworkManager;

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
    public static void distributesEvenlyAcrossNearAndFarReceivers(GameTestHelper helper) {
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
        helper.assertTrue(link.receiveEnergy(1_001, true) == 1_001
                && nearSink.getEnergyStored() == 0 && farSink.getEnergyStored() == 0,
            "Simulation must report the combined share without moving energy");
        int accepted = link.receiveEnergy(1_001, false);
        helper.assertTrue(accepted == 1_001
                && Math.abs(nearSink.getEnergyStored() - farSink.getEnergyStored()) == 1,
            "Near and far receivers must share a transfer evenly");
        IEnergyStorage otherLink = energy(helper, secondCablePos, Direction.UP);
        helper.assertTrue(otherLink.receiveEnergy(1_001, false) == 1_001
                && nearSink.getEnergyStored() == 1_001 && farSink.getEnergyStored() == 1_001,
            "Remainders must rotate across the network even when the entry cable changes");
        for (int i = 0; i < 10; i++) {
            helper.assertTrue(link.receiveEnergy(1, true) == 1 && link.receiveEnergy(1, false) == 1,
                "A one-unit transfer must remain routable after simulation");
        }
        helper.assertTrue(nearSink.getEnergyStored() == 1_006 && farSink.getEnergyStored() == 1_006,
            "One-unit transfers must alternate without simulation advancing the cursor");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void multipleConnectionsGiveOneMachineOneShare(GameTestHelper helper) {
        BlockPos cablePos = new BlockPos(3, 4, 3);
        BlockPos sinkPos = cablePos.east();
        BlockPos otherSinkPos = cablePos.west();
        helper.setBlock(cablePos, CrystalnexusModBlocks.ENERGY_CABLE_MK_2.get());
        helper.setBlock(cablePos.north(), CrystalnexusModBlocks.ENERGY_CABLE_MK_2.get());
        helper.setBlock(cablePos.north().east(), CrystalnexusModBlocks.ENERGY_CABLE_MK_2.get());
        helper.setBlock(sinkPos, CrystalnexusModBlocks.TESSERACT.get());
        helper.setBlock(otherSinkPos, CrystalnexusModBlocks.TESSERACT.get());
        IEnergyStorage link = energy(helper, cablePos, Direction.UP);
        IEnergyStorage sink = energy(helper, sinkPos, Direction.WEST);
        IEnergyStorage otherSink = energy(helper, otherSinkPos, Direction.EAST);
        helper.assertTrue(link.receiveEnergy(1_000, false) == 1_000
                && sink.getEnergyStored() == 500 && otherSink.getEnergyStored() == 500,
            "A machine touching multiple cables must receive only one share");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void redistributesUnusedSharesAndSkipsFullReceivers(GameTestHelper helper) {
        BlockPos cablePos = new BlockPos(2, 4, 2);
        BlockPos nearlyFullPos = cablePos.north();
        BlockPos sinkPos = cablePos.east();
        helper.setBlock(cablePos, CrystalnexusModBlocks.BASIC_ENERGY_CABLE.get());
        helper.setBlock(nearlyFullPos, CrystalnexusModBlocks.TESSERACT.get());
        helper.setBlock(sinkPos, CrystalnexusModBlocks.TESSERACT.get());
        IEnergyStorage link = energy(helper, cablePos, Direction.WEST);
        IEnergyStorage nearlyFull = energy(helper, nearlyFullPos, Direction.SOUTH);
        IEnergyStorage sink = energy(helper, sinkPos, Direction.WEST);
        int fill = nearlyFull.getMaxEnergyStored() - 100;
        while (fill > 0) {
            int added = nearlyFull.receiveEnergy(fill, false);
            helper.assertTrue(added > 0, "Receiver must accept energy while being filled");
            fill -= added;
        }
        helper.assertTrue(link.receiveEnergy(100_000, true) == 2_048 && sink.getEnergyStored() == 0,
            "Simulation must include redistribution and preserve the cable tier limit");
        helper.assertTrue(link.receiveEnergy(100_000, false) == 2_048
                && nearlyFull.getEnergyStored() == nearlyFull.getMaxEnergyStored()
                && sink.getEnergyStored() == 1_948,
            "Unused shares must go to receivers with space within the total tier limit");
        helper.assertTrue(link.receiveEnergy(1_000, false) == 1_000 && sink.getEnergyStored() == 2_948,
            "Full receivers must be skipped");
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

    @GameTest(template = "zero_point")
    public static void simulationDoesNotMoveEnergy(GameTestHelper helper) {
        BlockPos cablePos = new BlockPos(2, 4, 2);
        BlockPos sinkPos = cablePos.east();
        helper.setBlock(cablePos, CrystalnexusModBlocks.ENERGY_CABLE_MK_2.get());
        helper.setBlock(sinkPos, CrystalnexusModBlocks.TESSERACT.get());
        IEnergyStorage link = energy(helper, cablePos, Direction.WEST);
        IEnergyStorage sink = energy(helper, sinkPos, Direction.WEST);
        helper.assertTrue(link.receiveEnergy(1_000, true) == 1_000 && sink.getEnergyStored() == 0,
            "A simulated cable transfer must report capacity without mutating the sink");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void removalSplitsCachedNetwork(GameTestHelper helper) {
        BlockPos first = new BlockPos(1, 4, 1);
        BlockPos second = first.east();
        BlockPos sinkPos = second.east();
        helper.setBlock(first, CrystalnexusModBlocks.ENERGY_CABLE_MK_2.get());
        helper.setBlock(second, CrystalnexusModBlocks.ENERGY_CABLE_MK_2.get());
        helper.setBlock(sinkPos, CrystalnexusModBlocks.TESSERACT.get());
        IEnergyStorage link = energy(helper, first, Direction.WEST);
        helper.destroyBlock(second);
        helper.assertTrue(link.receiveEnergy(1_000, false) == 0,
            "Removing a cable must invalidate and split the cached component immediately");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void networkTickPullsFromExternalSource(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 4, 1);
        BlockPos cablePos = sourcePos.east();
        BlockPos sinkPos = cablePos.east();
        helper.setBlock(sourcePos, CrystalnexusModBlocks.TESSERACT.get());
        helper.setBlock(cablePos, CrystalnexusModBlocks.ENERGY_CABLE_MK_2.get());
        helper.setBlock(sinkPos, CrystalnexusModBlocks.TESSERACT.get());
        BlockPos otherSinkPos = cablePos.north();
        helper.setBlock(otherSinkPos, CrystalnexusModBlocks.TESSERACT.get());
        IEnergyStorage source = energy(helper, sourcePos, Direction.EAST);
        IEnergyStorage sink = energy(helper, sinkPos, Direction.WEST);
        IEnergyStorage otherSink = energy(helper, otherSinkPos, Direction.SOUTH);
        source.receiveEnergy(4_000, false);
        EnergyCableNetworkManager.tick(helper.getLevel());
        helper.assertTrue(source.getEnergyStored() == 0 && sink.getEnergyStored() == 2_000
                && otherSink.getEnergyStored() == 2_000,
            "The level network tick must pull and split source energy without sending it back");
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
