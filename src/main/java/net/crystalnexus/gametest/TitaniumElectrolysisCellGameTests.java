package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.TitaniumElectrolysisCellBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class TitaniumElectrolysisCellGameTests {
    private TitaniumElectrolysisCellGameTests() {}

    @GameTest(template = "zero_point")
    public static void waterProducesOxygenAndReturnsBucket(GameTestHelper helper) {
        TitaniumElectrolysisCellBlockEntity cell = place(helper);
        cell.setItem(0, new ItemStack(Items.WATER_BUCKET));
        charge(cell);
        run(cell, 40);

        helper.assertTrue(cell.getItem(0).isEmpty() && cell.getItem(1).is(Items.BUCKET),
            "A water bucket must move its empty container to the return slot");
        helper.assertTrue(cell.getTank(0).isEmpty(), "Electrolysis must consume 1000 mB of water");
        helper.assertTrue(cell.getTank(1).getFluid().is(CrystalnexusModFluids.OXYGEN.get())
                && cell.getTank(1).getFluidAmount() == 250,
            "Water electrolysis must produce 250 mB of oxygen");
        helper.assertTrue(cell.getEnergyStorage().getEnergyStored() == 0,
            "Titanium electrolysis must consume exactly 32768 FE");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void biomassProducesNitrogen(GameTestHelper helper) {
        TitaniumElectrolysisCellBlockEntity cell = place(helper);
        cell.setItem(0, new ItemStack(CrystalnexusModItems.BIOMASS.get()));
        charge(cell);
        run(cell, 40);

        helper.assertTrue(cell.getItem(0).isEmpty(), "Biomass must be consumed");
        helper.assertTrue(cell.getTank(1).getFluid().is(CrystalnexusModFluids.NITROGEN.get())
                && cell.getTank(1).getFluidAmount() == 250,
            "Biomass electrolysis must produce 250 mB of nitrogen");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void waitsWithoutPowerOrOutputSpace(GameTestHelper helper) {
        TitaniumElectrolysisCellBlockEntity cell = place(helper);
        cell.getTank(0).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        run(cell, 40);
        helper.assertTrue(cell.getTank(0).getFluidAmount() == 1000 && cell.getTank(1).isEmpty(),
            "An unpowered cell must not consume inputs");

        charge(cell);
        cell.getTank(1).fill(new FluidStack(CrystalnexusModFluids.NITROGEN.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
        run(cell, 40);
        helper.assertTrue(cell.getTank(0).getFluidAmount() == 1000
                && cell.getTank(1).getFluidAmount() == 4000
                && cell.getEnergyStorage().getEnergyStored() == 32768,
            "A full or incompatible output tank must preserve input and energy");
        helper.succeed();
    }

    private static TitaniumElectrolysisCellBlockEntity place(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.TITANIUM_ELECTROLYSIS_CELL.get());
        return helper.getBlockEntity(pos);
    }

    private static void charge(TitaniumElectrolysisCellBlockEntity cell) {
        for (int i = 0; i < 32; i++) cell.getEnergyStorage().receiveEnergy(1024, false);
    }

    private static void run(TitaniumElectrolysisCellBlockEntity cell, int ticks) {
        for (int i = 0; i < ticks; i++) cell.serverTick();
    }
}
