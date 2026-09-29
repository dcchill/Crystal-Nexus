package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.ChlorophyteAcceleratorBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class ChlorophyteAcceleratorGameTests {
    private ChlorophyteAcceleratorGameTests() {}

    @GameTest(template = "zero_point")
    public static void discoversNewCropWithinSixtyTicks(GameTestHelper helper) {
        BlockPos acceleratorPos = new BlockPos(6, 4, 6);
        BlockPos cropPos = acceleratorPos.offset(-5, -1, -5);
        helper.setBlock(acceleratorPos, CrystalnexusModBlocks.CHLOROPHYTE_ACCELERATOR.get());
        helper.setBlock(cropPos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 0));
        ChlorophyteAcceleratorBlockEntity accelerator = helper.getBlockEntity(acceleratorPos);
        for (int cycle = 0; cycle < 20; cycle++) accelerator.runAccelerationCycle(helper.getLevel());
        helper.assertTrue(accelerator.cachedTargetCount() > 0,
            "A crop anywhere in the scan volume must be cached within twenty three-tick cycles");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void insufficientEnergyIsNotExtracted(GameTestHelper helper) {
        BlockPos acceleratorPos = new BlockPos(2, 4, 2);
        helper.setBlock(acceleratorPos, CrystalnexusModBlocks.CHLOROPHYTE_ACCELERATOR.get());
        ChlorophyteAcceleratorBlockEntity accelerator = helper.getBlockEntity(acceleratorPos);
        accelerator.getEnergyStorage().receiveEnergy(255, false);
        accelerator.runAccelerationCycle(helper.getLevel());
        helper.assertTrue(accelerator.getEnergyStorage().getEnergyStored() == 255,
            "An accelerator must not drain energy when it cannot afford a cycle");
        helper.succeed();
    }
}
