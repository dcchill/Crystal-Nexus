package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.RefineryBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.processing.MaterialProcessingCatalog;
import net.crystalnexus.procedures.RefineryOnTickUpdateProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class MaterialProcessingGameTests {
    private MaterialProcessingGameTests() {}

    @GameTest(template = "zero_point")
    public static void refineryRequiresFullSlurryAmount(GameTestHelper helper) {
        var material = MaterialProcessingCatalog.get(helper.getLevel()).materials().values().stream()
            .findFirst().orElseThrow();
        BlockPos refineryPos = new BlockPos(1, 1, 1);
        helper.setBlock(refineryPos, CrystalnexusModBlocks.REFINERY.get());
        RefineryBlockEntity refinery = helper.getBlockEntity(refineryPos);
        refinery.getTank(0).fill(MaterialProcessingCatalog.slurry(material.id(), 1),
            IFluidHandler.FluidAction.EXECUTE);
        for (int i = 0; i < 4; i++) refinery.getEnergyStorage().receiveEnergy(1024, false);

        BlockPos absoluteRefinery = helper.absolutePos(refineryPos);
        for (int tick = 0; tick < 100; tick++)
            RefineryOnTickUpdateProcedure.execute(helper.getLevel(), absoluteRefinery);

        helper.assertTrue(refinery.getTank(0).getFluidAmount() == 1,
            "An incomplete slurry batch must not be consumed");
        helper.assertTrue(refinery.getItem(1).isEmpty(),
            "One millibucket of slurry must not produce dust");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void slurryComponentsSurviveStorageAndPreventMerging(GameTestHelper helper) {
        FluidStack copper = MaterialProcessingCatalog.slurry(net.minecraft.resources.ResourceLocation.parse("c:copper"), 1000);
        FluidStack tin = MaterialProcessingCatalog.slurry(net.minecraft.resources.ResourceLocation.parse("c:tin"), 1000);
        FluidTank tank = new FluidTank(4000);
        tank.fill(copper, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(tank.fill(tin, IFluidHandler.FluidAction.SIMULATE) == 0,
            "Copper and tin slurry must not merge");
        var saved = tank.writeToNBT(helper.getLevel().registryAccess(), new net.minecraft.nbt.CompoundTag());
        FluidTank restored = new FluidTank(4000);
        restored.readFromNBT(helper.getLevel().registryAccess(), saved);
        helper.assertTrue(MaterialProcessingCatalog.slurryMaterial(restored.getFluid())
                .filter(net.minecraft.resources.ResourceLocation.parse("c:copper")::equals).isPresent(),
            "Slurry material identity must survive tank NBT persistence");
        helper.succeed();
    }
}
