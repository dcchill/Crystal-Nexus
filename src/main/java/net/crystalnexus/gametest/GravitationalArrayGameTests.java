package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.GravitationalArrayControllerBlockEntity;
import net.crystalnexus.block.entity.MachineEnergyInputBlockEntity;
import net.crystalnexus.block.entity.MachineFluidInputBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.recipe.GravitationalArrayRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class GravitationalArrayGameTests {
    // GameTest's relative Y=0 is its structure block; template Y=0 begins one block above it.
    private static final BlockPos CONTROLLER = new BlockPos(9, 3, 14);
    private static final BlockPos TITANIUM = new BlockPos(11, 3, 10);
    private static final BlockPos CENTER_BLOCK = new BlockPos(14, 16, 14);

    private GravitationalArrayGameTests() {
    }

    @GameTest(template = "gravitational_array")
    public static void validatesNbtWithEnergyInputAtTitaniumPosition(GameTestHelper helper) {
        verify(helper);
    }

    @GameTest(template = "gravitational_array", rotationSteps = 1)
    public static void validatesRotatedNbtFromControllerFacing(GameTestHelper helper) {
        verify(helper);
    }

    @GameTest(template = "zero_point")
    public static void yellowDwarfRecipeLoadsSpecifiedInputsAndCosts(GameTestHelper helper) {
        GravitationalArrayRecipe recipe = helper.getLevel().getRecipeManager()
            .getAllRecipesFor(GravitationalArrayRecipe.Type.INSTANCE).stream()
            .filter(holder -> holder.id().getPath().equals("yellow_dwarf_star"))
            .findFirst().orElseThrow().value();
        int[] plan = recipe.consumptionPlan(List.of(
            new ItemStack(CrystalnexusModItems.ENERGY_SINGULARITY.get()),
            new ItemStack(CrystalnexusModItems.COAL_SINGULARITY.get()),
            new ItemStack(CrystalnexusModItems.IRON_SINGULARITY.get(), 2),
            new ItemStack(CrystalnexusModItems.GOLD_SINGULARITY.get())));
        helper.assertTrue(plan.length == 4 && plan[0] == 1 && plan[1] == 1 && plan[2] == 2 && plan[3] == 1,
            "Yellow Dwarf inputs must work in any slot order");
        helper.assertTrue(recipe.temporalFluid() == 250_000 && recipe.energy() == 50_000_000L
                && recipe.duration() == 1200 && recipe.output().is(CrystalnexusModItems.YELLOW_DWARF_STAR.get()),
            "Yellow Dwarf fluid, FE, duration, and output must match the balance data");
        helper.succeed();
    }

    private static void verify(GameTestHelper helper) {
        BlockState controllerState = helper.getBlockState(CONTROLLER);
        helper.assertTrue(controllerState.is(CrystalnexusModBlocks.GRAVITATIONAL_ARRAY_CONTROLLER.get()),
            "The Structure NBT must provide the controller anchor");
        // GameTest does not instantiate block entities saved inside its own template at tick zero.
        helper.setBlock(CONTROLLER, Blocks.AIR);
        helper.setBlock(CONTROLLER, controllerState);
        helper.setBlock(TITANIUM, CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
        GravitationalArrayControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER);
        controller.serverTick();
        helper.assertTrue(!controller.isFormed(),
            "The array must remain invalid until both an Energy Input and a Fluid Input are installed");
        BlockPos fluidPos = findTitanium(helper);
        helper.setBlock(fluidPos, CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
        MachineEnergyInputBlockEntity energyInput = helper.getBlockEntity(TITANIUM);
        MachineFluidInputBlockEntity fluidInput = helper.getBlockEntity(fluidPos);
        fluidInput.getFluidInput().fill(new FluidStack(CrystalnexusModFluids.TEMPORAL_ESSENCE.get(), 1_000),
            IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(controller.validateStructureNow(),
            "The array must validate as soon as both port types are present");
        controller.serverTick();
        helper.assertTrue(controller.isFormed(),
            "The Structure NBT must accept an Energy Input at any Titanium position");
        helper.assertTrue(energyInput.isBoundTo(helper.absolutePos(CONTROLLER)),
            "A validated Energy Input must bind to its active controller");
        helper.assertTrue(fluidInput.isBoundTo(helper.absolutePos(CONTROLLER)),
            "A validated Fluid Input must bind to its active controller");
        helper.assertTrue(controller.getFormationCenter() != null
                && BlockPos.containing(controller.getFormationCenter()).equals(helper.absolutePos(CENTER_BLOCK)),
            "A validated array must expose its chamber center for the client star renderer");
        helper.assertTrue(controller.getTemporalFluidTank().getFluidAmount() == 1_000,
            "A validated Fluid Input must relay Temporal Essence to its controller");
        controller.setItem(0, new ItemStack(CrystalnexusModItems.ENERGY_SINGULARITY.get()));
        controller.setItem(1, new ItemStack(CrystalnexusModItems.COAL_SINGULARITY.get()));
        controller.setItem(2, new ItemStack(CrystalnexusModItems.IRON_SINGULARITY.get(), 2));
        controller.setItem(3, new ItemStack(CrystalnexusModItems.GOLD_SINGULARITY.get()));
        energyInput.getEnergyStorage().receiveEnergy(20_480, false);
        energyInput.getEnergyStorage().receiveEnergy(20_480, false);
        controller.serverTick();
        helper.assertTrue(controller.getProgress() == 0 && energyInput.getEnergyStorage().getEnergyStored() == 0,
            "The controller must import a partial FE installment even when one port cannot fund a full progress tick");
        energyInput.getEnergyStorage().receiveEnergy(20_480, false);
        controller.serverTick();
        helper.assertTrue(controller.getProgress() == 1,
            "Accumulated FE installments must advance crafting once the scheduled cost is met");
        helper.assertTrue(helper.getBlockState(CENTER_BLOCK).isAir(),
            "The exact template center must remain air");
        helper.succeed();
    }

    private static BlockPos findTitanium(GameTestHelper helper) {
        for (int y = 1; y <= 30; y++) for (int x = 0; x < 29; x++) for (int z = 0; z < 29; z++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!pos.equals(TITANIUM) && helper.getBlockState(pos).is(CrystalnexusModBlocks.TITANIUM_BLOCK.get())) return pos;
        }
        throw new IllegalStateException("Structure template has no second Titanium position");
    }
}
