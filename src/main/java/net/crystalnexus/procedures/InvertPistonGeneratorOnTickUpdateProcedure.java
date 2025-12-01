package net.crystalnexus.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModFluids;

public class InvertPistonGeneratorOnTickUpdateProcedure {

    private static final int FUEL_CELL_AMOUNT = 250; // mB per fuel cell

    public static void execute(LevelAccessor worldAccess, double x, double y, double z) {
        if (!(worldAccess instanceof Level level)) return;
        if (level.isClientSide()) return;
        BlockPos pos = BlockPos.containing(x, y, z);

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        IEnergyStorage energyStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (energyStorage == null) return;

        // --- Progress / GUI ---
        double progress = be.getPersistentData().getDouble("progress");

        // --- Upgrade stacks ---
        ItemStack upgradeStack = getItemFromSlot(level, pos, 2);

        int ENERGY_PER_TICK;
        int COOK_TIME;

        // Energy upgrade
        if (upgradeStack.getItem() == CrystalnexusModItems.EFFICIENCY_UPGRADE.get()) {
            ENERGY_PER_TICK = 640;
        } else if (upgradeStack.getItem() == CrystalnexusModItems.CARBON_EFFICIENCY_UPGRADE.get()) {
            ENERGY_PER_TICK = 768;
        } else {
            ENERGY_PER_TICK = 512;
        }

        // Acceleration upgrade
        if (upgradeStack.getItem() == CrystalnexusModItems.ACCELERATION_UPGRADE.get()) {
            COOK_TIME = 450;
        } else if (upgradeStack.getItem() == CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) {
            COOK_TIME = 500;
        } else {
            COOK_TIME = 499;
        }

        be.getPersistentData().putDouble("maxProgress", COOK_TIME);

        // --- Slots ---
        ItemStack fuelStack = getItemFromSlot(level, pos, 0);

        // --- Fluid handler ---
        IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);

        // --- Fill tank from fuel cells ---
        if (!fuelStack.isEmpty() && fuelStack.getItem() == CrystalnexusModItems.GAS_FUEL_CELL.get() && fluidHandler != null) {
            int filledSim = fluidHandler.fill(new FluidStack(CrystalnexusModFluids.GASOLINE.get(), FUEL_CELL_AMOUNT), IFluidHandler.FluidAction.SIMULATE);
            if (filledSim == FUEL_CELL_AMOUNT) {
                fluidHandler.fill(new FluidStack(CrystalnexusModFluids.GASOLINE.get(), FUEL_CELL_AMOUNT), IFluidHandler.FluidAction.EXECUTE);
                consumeItem(level, pos, 0, 1);
                insertIntoSlot(level, pos, 1, new ItemStack(CrystalnexusModItems.EMPTY_FUEL_CELL.get()));
            }
        }

        // --- Check if enough fuel to run ---
        boolean canRun = false;
        if (fluidHandler != null) {
            FluidStack simulated = fluidHandler.drain(FUEL_CELL_AMOUNT, IFluidHandler.FluidAction.SIMULATE);
            canRun = simulated.getAmount() >= FUEL_CELL_AMOUNT;
        }

        // --- Update blockstate ---
        setBlockStateInteger(level, pos, "blockstate", canRun ? 2 : 1);

        if (canRun && energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
            // Increment progress
            progress += 1;
            be.getPersistentData().putDouble("progress", progress);

            // Try to insert real energy into the block’s buffer
            energyStorage.receiveEnergy(ENERGY_PER_TICK, false);

            // Consume fuel on full cook cycle
            if (progress >= COOK_TIME) {
                if (fluidHandler != null) {
                    fluidHandler.drain(FUEL_CELL_AMOUNT, IFluidHandler.FluidAction.EXECUTE);
                }
                progress = 0;
                be.getPersistentData().putDouble("progress", progress);
            }
        }

        // --- Push energy to neighbors ---
        if (energyStorage.getEnergyStored() > 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
                if (neighbor == null) continue;

                int energyToSend = Math.min(energyStorage.getEnergyStored(), 1000); // push up to 1000 FE/tick
                int accepted = neighbor.receiveEnergy(energyToSend, false);
                if (accepted > 0) {
                    // drain from own storage
                    if (energyStorage instanceof net.neoforged.neoforge.energy.EnergyStorage modifiable) {
                        modifiable.extractEnergy(accepted, false);
                    }
                }
            }
        }
    }

    // ----- Helper functions -----

    private static ItemStack getItemFromSlot(Level level, BlockPos pos, int slot) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler == null) return ItemStack.EMPTY;
        return handler.getStackInSlot(slot).copy();
    }

    private static void consumeItem(Level level, BlockPos pos, int slot, int amount) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler instanceof IItemHandlerModifiable mod) {
            ItemStack cur = mod.getStackInSlot(slot).copy();
            if (!cur.isEmpty()) {
                cur.shrink(amount);
                mod.setStackInSlot(slot, cur);
            }
        }
    }

    private static void insertIntoSlot(Level level, BlockPos pos, int slot, ItemStack toInsert) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (!(handler instanceof IItemHandlerModifiable mod)) return;

        ItemStack current = mod.getStackInSlot(slot).copy();
        if (current.isEmpty()) {
            mod.setStackInSlot(slot, toInsert.copy());
            return;
        }
        if (current.getItem() == toInsert.getItem()) {
            int space = current.getMaxStackSize() - current.getCount();
            int add = Math.min(space, toInsert.getCount());
            current.grow(add);
            mod.setStackInSlot(slot, current);
        }
    }

    private static void setBlockStateInteger(LevelAccessor world, BlockPos pos, String propertyName, int value) {
        BlockState bs = world.getBlockState(pos);
        if (bs.getBlock().getStateDefinition().getProperty(propertyName) instanceof IntegerProperty intProp
                && intProp.getPossibleValues().contains(value)) {
            world.setBlock(pos, bs.setValue(intProp, value), 3);
        }
    }
}
