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
import net.crystalnexus.block.entity.InvertPistonGeneratorBlockEntity;
import net.crystalnexus.energy.GeneratorEnergyStorage;
import net.crystalnexus.util.MachineUpgradeHelper;

public class InvertPistonGeneratorOnTickUpdateProcedure {

    private static final int FUEL_CELL_AMOUNT = 250; 

    public static void execute(LevelAccessor worldAccess, double x, double y, double z) {
        if (!(worldAccess instanceof Level level)) return;
        if (level.isClientSide()) return;
        BlockPos pos = BlockPos.containing(x, y, z);

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof InvertPistonGeneratorBlockEntity generator)) return;

        GeneratorEnergyStorage energyStorage = generator.getEnergyStorage();

        double progress = be.getPersistentData().getDouble("progress");

        ItemStack upgradeStack = getItemFromSlot(level, pos, 2);

        int ENERGY_PER_TICK;
        int COOK_TIME;

        ENERGY_PER_TICK = (int) (1024 * MachineUpgradeHelper.generatorEfficiency(upgradeStack, 1.25, 1.5) * MachineUpgradeHelper.generatorSpeed(upgradeStack));

        COOK_TIME = (int) Math.ceil(MachineUpgradeHelper.generatorCycleTime(upgradeStack, 350, 450, 500));
        generator.machineSync.setDouble("maxProgress", COOK_TIME);

        ItemStack fuelStack = getItemFromSlot(level, pos, 0);

        IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
		if (!fuelStack.isEmpty() && fluidHandler != null) {
		
		    FluidStack fluidToInsert = FluidStack.EMPTY;
		
		    if (fuelStack.getItem() == CrystalnexusModItems.GAS_FUEL_CELL.get()) {
		        fluidToInsert = new FluidStack(CrystalnexusModFluids.GASOLINE.get(), FUEL_CELL_AMOUNT);
		    }
		
		    else if (fuelStack.getItem() == CrystalnexusModItems.OVERFUEL_CELL.get()) {
		        fluidToInsert = new FluidStack(CrystalnexusModFluids.OVERFUEL.get(), FUEL_CELL_AMOUNT);
		    }
		
		    if (!fluidToInsert.isEmpty()) {
		        int filledSim = fluidHandler.fill(fluidToInsert, IFluidHandler.FluidAction.SIMULATE);
		
		        if (filledSim == FUEL_CELL_AMOUNT) {
		            fluidHandler.fill(fluidToInsert, IFluidHandler.FluidAction.EXECUTE);
		            consumeItem(level, pos, 0, 1);
		            insertIntoSlot(level, pos, 1, new ItemStack(CrystalnexusModItems.EMPTY_FUEL_CELL.get()));
		        }
		    }
		}

		boolean canRun = false;
		boolean isOverfuel = false;
		FluidStack tankFluid = FluidStack.EMPTY;
		
		if (fluidHandler != null) {
		    tankFluid = fluidHandler.drain(FUEL_CELL_AMOUNT, IFluidHandler.FluidAction.SIMULATE);
		
		    if (tankFluid.getAmount() >= FUEL_CELL_AMOUNT) {
		        canRun = true;
		
		        if (tankFluid.getFluid() == CrystalnexusModFluids.OVERFUEL.get()) {
		            isOverfuel = true;
		        }
		    }
		}

        setBlockStateInteger(level, pos, "blockstate", canRun ? 2 : 1);

        if (canRun && energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
            progress += 1;
            generator.machineSync.setDouble("progress", progress);

            
            int energyOutput = ENERGY_PER_TICK;

				if (isOverfuel) {
				    energyOutput *= 2; 
				}
				
				energyStorage.generateEnergy(energyOutput, false);

            if (progress >= COOK_TIME) {
                if (fluidHandler != null) {
                    fluidHandler.drain(FUEL_CELL_AMOUNT, IFluidHandler.FluidAction.EXECUTE);
                }
                progress = 0;
                generator.machineSync.setDouble("progress", progress);
            }
        }

        if (energyStorage.getEnergyStored() > 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
                if (neighbor == null) continue;

                int energyToSend = Math.min(energyStorage.getEnergyStored(), 4096); 
                int accepted = neighbor.receiveEnergy(energyToSend, false);
                if (accepted > 0) {
                    if (energyStorage instanceof net.neoforged.neoforge.energy.EnergyStorage modifiable) {
                        modifiable.extractEnergy(accepted, false);
                    }
                }
            }
        }
    }


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
                && intProp.getPossibleValues().contains(value) && bs.getValue(intProp) != value) {
            world.setBlock(pos, bs.setValue(intProp, value), 3);
        }
    }
}
