package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModBlocks;

public class ReactorCoreOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double computerX = 0;
		double computerZ = 0;
		double mbPt = 0;
		double fePt = 0;
		double energy = 0;
		double fluid = 0;
		BlockState computer = Blocks.AIR.defaultBlockState();
		BlockState input = Blocks.AIR.defaultBlockState();
		BlockState output = Blocks.AIR.defaultBlockState();
		BlockState waste = Blocks.AIR.defaultBlockState();
		computer = CrystalnexusModBlocks.REACTOR_COMPUTER.get().defaultBlockState();
		input = CrystalnexusModBlocks.REACTOR_FLUID_INPUT.get().defaultBlockState();
		output = CrystalnexusModBlocks.REACTOR_ENERGY_OUTPUT.get().defaultBlockState();
		waste = CrystalnexusModBlocks.REACTOR_WASTE_OUTPUT.get().defaultBlockState();
		mbPt = 100;
		if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).getBlock() == computer.getBlock()) {
			computerX = 1;
		} else if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).getBlock() == computer.getBlock()) {
			computerX = -1;
		} else if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).getBlock() == computer.getBlock()) {
			computerZ = 1;
		} else if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).getBlock() == computer.getBlock()) {
			computerZ = -1;
		}
		if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).getBlock() == input.getBlock()) {
			fluid = fillTankSimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), (int) mbPt, null, Fluids.WATER);
			fluid = drainTankSimulate(world, BlockPos.containing(x + 1, y, z), (int) fluid, null);
			if (getFluidTankCapacity(world, BlockPos.containing(x + computerX, y, z + computerZ), 1, null) != getFluidTankLevel(world, BlockPos.containing(x + computerX, y, z + computerZ), 1, null)) {
				if (fluid < getFluidTankLevel(world, BlockPos.containing(x + 1, y, z), 1, null)) {
					if (world instanceof ILevelExtension _ext) {
						IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x + 1, y, z), null);
						if (_fluidHandler != null)
							_fluidHandler.drain((int) fluid, IFluidHandler.FluidAction.EXECUTE);
					}
					if (world instanceof ILevelExtension _ext) {
						IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_fluidHandler != null)
							_fluidHandler.fill(new FluidStack(Fluids.WATER, (int) fluid), IFluidHandler.FluidAction.EXECUTE);
					}
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).getBlock() == input.getBlock()) {
			fluid = fillTankSimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), (int) mbPt, null, Fluids.WATER);
			fluid = drainTankSimulate(world, BlockPos.containing(x - 1, y, z), (int) fluid, null);
			if (getFluidTankCapacity(world, BlockPos.containing(x + computerX, y, z + computerZ), 1, null) != getFluidTankLevel(world, BlockPos.containing(x + computerX, y, z + computerZ), 1, null)) {
				if (fluid < getFluidTankLevel(world, BlockPos.containing(x - 1, y, z), 1, null)) {
					if (world instanceof ILevelExtension _ext) {
						IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x - 1, y, z), null);
						if (_fluidHandler != null)
							_fluidHandler.drain((int) fluid, IFluidHandler.FluidAction.EXECUTE);
					}
					if (world instanceof ILevelExtension _ext) {
						IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_fluidHandler != null)
							_fluidHandler.fill(new FluidStack(Fluids.WATER, (int) fluid), IFluidHandler.FluidAction.EXECUTE);
					}
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).getBlock() == input.getBlock()) {
			fluid = fillTankSimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), (int) mbPt, null, Fluids.WATER);
			fluid = drainTankSimulate(world, BlockPos.containing(x, y, z + 1), (int) fluid, null);
			if (getFluidTankCapacity(world, BlockPos.containing(x + computerX, y, z + computerZ), 1, null) != getFluidTankLevel(world, BlockPos.containing(x + computerX, y, z + computerZ), 1, null)) {
				if (fluid < getFluidTankLevel(world, BlockPos.containing(x, y, z + 1), 1, null)) {
					if (world instanceof ILevelExtension _ext) {
						IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z + 1), null);
						if (_fluidHandler != null)
							_fluidHandler.drain((int) fluid, IFluidHandler.FluidAction.EXECUTE);
					}
					if (world instanceof ILevelExtension _ext) {
						IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_fluidHandler != null)
							_fluidHandler.fill(new FluidStack(Fluids.WATER, (int) fluid), IFluidHandler.FluidAction.EXECUTE);
					}
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).getBlock() == input.getBlock()) {
			fluid = fillTankSimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), (int) mbPt, null, Fluids.WATER);
			fluid = drainTankSimulate(world, BlockPos.containing(x, y, z - 1), (int) fluid, null);
			if (getFluidTankCapacity(world, BlockPos.containing(x + computerX, y, z + computerZ), 1, null) != getFluidTankLevel(world, BlockPos.containing(x + computerX, y, z + computerZ), 1, null)) {
				if (fluid < getFluidTankLevel(world, BlockPos.containing(x, y, z - 1), 1, null)) {
					if (world instanceof ILevelExtension _ext) {
						IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z - 1), null);
						if (_fluidHandler != null)
							_fluidHandler.drain((int) fluid, IFluidHandler.FluidAction.EXECUTE);
					}
					if (world instanceof ILevelExtension _ext) {
						IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_fluidHandler != null)
							_fluidHandler.fill(new FluidStack(Fluids.WATER, (int) fluid), IFluidHandler.FluidAction.EXECUTE);
					}
				}
			}
		}
		if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).getBlock() == output.getBlock()) {
			energy = extractEnergySimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), 1048576, null);
			energy = receiveEnergySimulate(world, BlockPos.containing(x + 1, y, z), (int) energy, null);
			if (getMaxEnergyStored(world, BlockPos.containing(x + 1, y, z), null) != getEnergyStored(world, BlockPos.containing(x + 1, y, z), null)) {
				if (energy <= getEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null)) {
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy((int) energy, false);
					}
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + 1, y, z), null);
						if (_entityStorage != null)
							_entityStorage.receiveEnergy((int) energy, false);
					}
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).getBlock() == output.getBlock()) {
			energy = extractEnergySimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), 1048576, null);
			energy = receiveEnergySimulate(world, BlockPos.containing(x - 1, y, z), (int) energy, null);
			if (getMaxEnergyStored(world, BlockPos.containing(x - 1, y, z), null) != getEnergyStored(world, BlockPos.containing(x - 1, y, z), null)) {
				if (65536 <= getEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null)) {
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy(65536, false);
					}
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x - 1, y, z), null);
						if (_entityStorage != null)
							_entityStorage.receiveEnergy(65536, false);
					}
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).getBlock() == output.getBlock()) {
			energy = extractEnergySimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), 1048576, null);
			energy = receiveEnergySimulate(world, BlockPos.containing(x, y, z + 1), (int) energy, null);
			if (getMaxEnergyStored(world, BlockPos.containing(x, y, z + 1), null) != getEnergyStored(world, BlockPos.containing(x, y, z + 1), null)) {
				if (65536 <= getEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null)) {
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy(65536, false);
					}
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z + 1), null);
						if (_entityStorage != null)
							_entityStorage.receiveEnergy(65536, false);
					}
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).getBlock() == output.getBlock()) {
			energy = extractEnergySimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), 1048576, null);
			energy = receiveEnergySimulate(world, BlockPos.containing(x, y, z - 1), (int) energy, null);
			if (getMaxEnergyStored(world, BlockPos.containing(x, y, z - 1), null) != getEnergyStored(world, BlockPos.containing(x, y, z - 1), null)) {
				if (65536 <= getEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null)) {
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy(65536, false);
					}
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z - 1), null);
						if (_entityStorage != null)
							_entityStorage.receiveEnergy(65536, false);
					}
				}
			}
		}
		if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).getBlock() == waste.getBlock()
				&& (itemFromBlockInventory(world, BlockPos.containing(x + computerX, y, z + computerZ), 2).copy()).getItem() == CrystalnexusModItems.BLUTONIUM_WASTE.get()) {
			if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + 1, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(0, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + 1, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(1, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + 1, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(2, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + 1, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(3, _setstack);
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).getBlock() == waste.getBlock()
				&& (itemFromBlockInventory(world, BlockPos.containing(x + computerX, y, z + computerZ), 2).copy()).getItem() == CrystalnexusModItems.BLUTONIUM_WASTE.get()) {
			if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x - 1, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(0, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x - 1, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(1, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x - 1, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(2, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x - 1, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(3, _setstack);
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).getBlock() == waste.getBlock()
				&& (itemFromBlockInventory(world, BlockPos.containing(x + computerX, y, z + computerZ), 2).copy()).getItem() == CrystalnexusModItems.BLUTONIUM_WASTE.get()) {
			if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z + 1), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(0, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z + 1), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(1, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z + 1), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(2, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z + 1), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(3, _setstack);
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).getBlock() == waste.getBlock()
				&& (itemFromBlockInventory(world, BlockPos.containing(x + computerX, y, z + computerZ), 2).copy()).getItem() == CrystalnexusModItems.BLUTONIUM_WASTE.get()) {
			if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z - 1), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(0, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z - 1), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(1, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z - 1), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(2, _setstack);
				}
			} else if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).getCount() != 64) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 2;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z - 1), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(3, _setstack);
				}
			}
		}
	}

	private static int fillTankSimulate(LevelAccessor level, BlockPos pos, int amount, Direction direction, Fluid fluid) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.SIMULATE);
		}
		return 0;
	}

	private static int drainTankSimulate(LevelAccessor level, BlockPos pos, int amount, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.drain(amount, IFluidHandler.FluidAction.SIMULATE).getAmount();
		}
		return 0;
	}

	private static int getFluidTankCapacity(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.getTankCapacity(tank);
		}
		return 0;
	}

	private static int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.getFluidInTank(tank).getAmount();
		}
		return 0;
	}

	private static int extractEnergySimulate(LevelAccessor level, BlockPos pos, int amount, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.extractEnergy(amount, true);
		}
		return 0;
	}

	private static int receiveEnergySimulate(LevelAccessor level, BlockPos pos, int amount, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.receiveEnergy(amount, true);
		}
		return 0;
	}

	public static int getMaxEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.getMaxEnergyStored();
		}
		return 0;
	}

	public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.getEnergyStored();
		}
		return 0;
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}
}