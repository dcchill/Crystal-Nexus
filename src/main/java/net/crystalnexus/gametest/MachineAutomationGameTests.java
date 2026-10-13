package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.*;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_parts")
@PrefixGameTestTemplate(false)
public final class MachineAutomationGameTests {
    private static final Direction[] SIDES = {null, Direction.DOWN, Direction.UP, Direction.NORTH,
        Direction.SOUTH, Direction.WEST, Direction.EAST};

    @GameTest(template = "empty")
    public static void acceleratorExposesAllOutputsButNeverExtractsInputs(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.PARTICLE_ACCELERATOR_CONTROLLER.get());
        ParticleAcceleratorControllerBlockEntity machine = helper.getBlockEntity(pos);
        for (Direction side : SIDES) {
            var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, machine.getBlockPos(), side);
            helper.assertTrue(handler != null && handler.getSlots() == 8, "Every accelerator slot must be visible: " + side);
            for (int slot = 0; slot < 8; slot++) {
                boolean output = slot == 1 || slot >= 5;
                machine.setItem(slot, ItemStack.EMPTY);
                ItemStack offered = new ItemStack(Items.IRON_INGOT, 4);
                helper.assertTrue(handler.insertItem(slot, offered, false).isEmpty() != output,
                    "Only input slots accept items: " + side + "/" + slot);
                machine.setItem(slot, offered.copy());
                helper.assertTrue(handler.extractItem(slot, 2, true).getCount() == (output ? 2 : 0)
                        && machine.getItem(slot).getCount() == 4, "Simulating extraction must not change storage");
                helper.assertTrue(handler.extractItem(slot, 2, false).getCount() == (output ? 2 : 0)
                        && machine.getItem(slot).getCount() == (output ? 2 : 4),
                    "Only output slots may be extracted: " + side + "/" + slot);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chemicalReactorsOnlyExportTheirOutputFluid(GameTestHelper helper) {
        Block[] blocks = {CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get(),
            CrystalnexusModBlocks.MEGA_CHEMICAL_REACTION_CHAMBER.get()};
        BlockPos pos = new BlockPos(1, 1, 1);
        for (Block block : blocks) {
            helper.setBlock(pos, block);
            FluidChemicalReactionChamberBlockEntity machine = helper.getBlockEntity(pos);
            for (Direction side : SIDES) {
                var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, machine.getBlockPos(), side);
                machine.getTank(0).setFluid(new FluidStack(Fluids.WATER, 1000));
                machine.getTank(1).setFluid(new FluidStack(Fluids.LAVA, 1000));
                machine.getTank(2).setFluid(FluidStack.EMPTY);
                for (var action : IFluidHandler.FluidAction.values()) {
                    helper.assertTrue(handler.drain(100, action).isEmpty()
                            && handler.drain(new FluidStack(Fluids.WATER, 100), action).isEmpty(),
                        "Empty output must never fall back to draining inputs: " + block + "/" + side);
                }
                machine.getTank(2).setFluid(new FluidStack(Fluids.WATER, 300));
                helper.assertTrue(handler.drain(100, IFluidHandler.FluidAction.SIMULATE).getAmount() == 100
                        && machine.getTank(2).getFluidAmount() == 300, "Simulation must preserve the output");
                handler.drain(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
                handler.drain(100, IFluidHandler.FluidAction.EXECUTE);
                helper.assertTrue(machine.getTank(0).getFluidAmount() == 1000
                        && machine.getTank(1).getFluidAmount() == 1000 && machine.getTank(2).getFluidAmount() == 100,
                    "Both drain overloads must use output even when an input contains the same fluid");
                helper.assertTrue(!handler.isFluidValid(2, new FluidStack(Fluids.WATER, 100)),
                    "Output tanks must reject imports");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inputTanksAcceptIngredientsButNeverExportThem(GameTestHelper helper) {
        Block[] blocks = {CrystalnexusModBlocks.MASTICATOR.get(), CrystalnexusModBlocks.HEMOCHANTER.get(),
            CrystalnexusModBlocks.REACTOR_COMPUTER.get(), CrystalnexusModBlocks.CIRCUIT_PRESS.get(),
            CrystalnexusModBlocks.PISTON_GENERATOR.get(), CrystalnexusModBlocks.INVERT_PISTON_GENERATOR.get(),
            CrystalnexusModBlocks.STEAM_ENGINE.get(), CrystalnexusModBlocks.STEAM_ENGINE_UPGRADE.get(),
            CrystalnexusModBlocks.FLUID_PACKAGER.get()};
        Fluid[] fluids = {CrystalnexusModFluids.BLOOD.get(), CrystalnexusModFluids.BLOOD.get(), Fluids.WATER,
            CrystalnexusModFluids.NITROGEN.get(), CrystalnexusModFluids.GASOLINE.get(), CrystalnexusModFluids.GASOLINE.get(),
            CrystalnexusModFluids.STEAM.get(), CrystalnexusModFluids.STEAM.get(), CrystalnexusModFluids.CRUDE_OIL.get()};
        BlockPos pos = new BlockPos(1, 1, 1);
        for (int i = 0; i < blocks.length; i++) {
            helper.setBlock(pos, blocks[i]);
            BlockEntity machine = helper.getBlockEntity(pos);
            var first = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, machine.getBlockPos(), null);
            helper.assertTrue(first.fill(new FluidStack(fluids[i], 100), IFluidHandler.FluidAction.EXECUTE) == 100,
                "Ingredient imports must work: " + blocks[i]);
            for (Direction side : SIDES) {
                var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, machine.getBlockPos(), side);
                helper.assertTrue(handler == first, "All sides must share one automation view");
                for (var action : IFluidHandler.FluidAction.values()) {
                    helper.assertTrue(handler.drain(100, action).isEmpty()
                            && handler.drain(new FluidStack(fluids[i], 100), action).isEmpty(),
                        "Automation cannot drain ingredients: " + blocks[i] + "/" + side);
                }
                helper.assertTrue(handler.getFluidInTank(0).getAmount() == 100, "Rejected drains preserve ingredients");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void outputTanksRejectImportsAndAllowExtraction(GameTestHelper helper) {
        Block[] blocks = {CrystalnexusModBlocks.HEMOLYZER.get(), CrystalnexusModBlocks.STEAM_COLLECTOR.get(),
            CrystalnexusModBlocks.NODE_EXTRACTOR.get(), CrystalnexusModBlocks.OXYGEN_COLLECTOR.get(),
            CrystalnexusModBlocks.NEUTRON_FLUX_CHAMBER_HATCH.get()};
        BlockPos pos = new BlockPos(1, 1, 1);
        for (Block block : blocks) {
            helper.setBlock(pos, block);
            BlockEntity machine = helper.getBlockEntity(pos);
            FluidTank tank = switch (machine) {
                case HemolyzerBlockEntity m -> m.getBloodTank();
                case SteamCollectorBlockEntity m -> m.getFluidTank();
                case NodeExtractorBlockEntity m -> m.getFluidTank();
                case OxygenCollectorBlockEntity m -> m.getFluidTank();
                case NeutronFluxChamberHatchBlockEntity m -> m.getFluidOutputTank();
                default -> throw new IllegalStateException();
            };
            for (Direction side : SIDES) {
                tank.setFluid(new FluidStack(Fluids.WATER, 300));
                var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, machine.getBlockPos(), side);
                for (var action : IFluidHandler.FluidAction.values())
                    helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 100), action) == 0,
                        "Output tanks reject filling: " + block + "/" + side);
                helper.assertTrue(handler.drain(100, IFluidHandler.FluidAction.SIMULATE).getAmount() == 100
                        && tank.getFluidAmount() == 300, "Simulation must preserve produced fluid");
                handler.drain(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
                handler.drain(100, IFluidHandler.FluidAction.EXECUTE);
                helper.assertTrue(tank.getFluidAmount() == 100, "Both drain overloads extract produced fluid");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void energyExtractorExportsByproductsInsteadOfItsUpgrade(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.ENERGY_EXTRACTOR.get());
        EnergyExtractorBlockEntity machine = helper.getBlockEntity(pos);
        for (Direction side : SIDES) {
            machine.setItem(0, new ItemStack(Items.COAL));
            machine.setItem(1, new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get()));
            machine.setItem(2, new ItemStack(Items.IRON_NUGGET));
            var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, machine.getBlockPos(), side);
            helper.assertTrue(handler.extractItem(0, 1, false).isEmpty()
                    && handler.extractItem(1, 1, false).isEmpty()
                    && handler.extractItem(2, 1, false).is(Items.IRON_NUGGET),
                "Only the energy extractor's byproduct should be extractable: " + side);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void automationCannotExtractInstalledUpgrades(GameTestHelper helper) {
        Block[] blocks = {CrystalnexusModBlocks.PARTS_ASSEMBLER.get(), CrystalnexusModBlocks.ITEM_COLLECTOR.get(),
            CrystalnexusModBlocks.ENERGY_EXTRACTOR.get(), CrystalnexusModBlocks.TURBINE.get()};
        int[] upgradeSlots = {3, 1, 1, 0};
        BlockPos pos = new BlockPos(1, 1, 1);
        for (int i = 0; i < blocks.length; i++) {
            helper.setBlock(pos, blocks[i]);
            Container machine = helper.getBlockEntity(pos);
            machine.setItem(upgradeSlots[i], new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get()));
            for (Direction side : SIDES) {
                var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), side);
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    if (!handler.getStackInSlot(slot).isEmpty())
                        helper.assertTrue(handler.extractItem(slot, 64, true).isEmpty()
                                && handler.extractItem(slot, 64, false).isEmpty(),
                            "Installed upgrades must remain in the machine: " + blocks[i] + "/" + side);
                }
            }
        }
        helper.succeed();
    }
}
