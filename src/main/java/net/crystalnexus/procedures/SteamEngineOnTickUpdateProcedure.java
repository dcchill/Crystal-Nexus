package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModItems;

public class SteamEngineOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double outputAmount = 0;
		double cookTime = 0;
		double nrgstrt = 0;
		double T = 0;
		double energy = 0;
		if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") == 0) {
			{
				int _value = 1;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		} else {
			{
				int _value = 2;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		}
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.EFFICIENCY_UPGRADE.get()) {
			outputAmount = 1.25;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.CARBON_EFFICIENCY_UPGRADE.get()) {
			outputAmount = 1.5;
		} else {
			outputAmount = 1;
		}
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.ACCELERATION_UPGRADE.get()) {
			cookTime = 325;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) {
			cookTime = 500;
		} else {
			cookTime = 250;
		}
		if (!world.isClientSide()) {
			BlockPos _bp = BlockPos.containing(x, y, z);
			BlockEntity _blockEntity = world.getBlockEntity(_bp);
			BlockState _bs = world.getBlockState(_bp);
			if (_blockEntity != null)
				_blockEntity.getPersistentData().putDouble("maxProgress", cookTime);
			if (world instanceof Level _level)
				_level.sendBlockUpdated(_bp, _bs, _bs, 3);
		}
		if (getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null) >= 500) {
			if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") < cookTime) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putDouble("progress", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") + 1));
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CLOUD, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.25, 0, 0.25, 0);
				if (world instanceof ILevelExtension _ext) {
					IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
					if (_entityStorage != null)
						_entityStorage.receiveEnergy((int) (512 * outputAmount), false);
				}
			}
			if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= cookTime) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putDouble("progress", 0);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
				if (world instanceof ILevelExtension _ext) {
					IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
					if (_fluidHandler != null)
						_fluidHandler.drain(500, IFluidHandler.FluidAction.EXECUTE);
				}
			}
		}
		if (canReceiveEnergy(world, BlockPos.containing(x, y + 1, z), Direction.DOWN)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), (int) (512 * outputAmount), null);
			energy = receiveEnergySimulate(world, BlockPos.containing(x, y + 1, z), (int) energy, Direction.DOWN);
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy((int) energy, false);
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y + 1, z), Direction.DOWN);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) energy, false);
			}
		}
		if (canReceiveEnergy(world, BlockPos.containing(x + 1, y, z), Direction.WEST)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), (int) (512 * outputAmount), null);
			energy = receiveEnergySimulate(world, BlockPos.containing(x + 1, y, z), (int) energy, Direction.WEST);
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy((int) energy, false);
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + 1, y, z), Direction.WEST);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) energy, false);
			}
		}
		if (canReceiveEnergy(world, BlockPos.containing(x - 1, y, z), Direction.EAST)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), (int) (512 * outputAmount), null);
			energy = receiveEnergySimulate(world, BlockPos.containing(x - 1, y, z), (int) energy, Direction.EAST);
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy((int) energy, false);
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x - 1, y, z), Direction.DOWN);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) energy, false);
			}
		}
		if (canReceiveEnergy(world, BlockPos.containing(x, y, z - 1), Direction.SOUTH)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), (int) (512 * outputAmount), null);
			energy = receiveEnergySimulate(world, BlockPos.containing(x, y, z - 1), (int) energy, Direction.SOUTH);
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy((int) energy, false);
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z - 1), Direction.SOUTH);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) energy, false);
			}
		}
		if (canReceiveEnergy(world, BlockPos.containing(x, y, z + 1), Direction.NORTH)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), (int) (512 * outputAmount), null);
			energy = receiveEnergySimulate(world, BlockPos.containing(x, y, z + 1), (int) energy, Direction.NORTH);
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy((int) energy, false);
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z + 1), Direction.NORTH);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) energy, false);
			}
		}
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}

	private static int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.getFluidInTank(tank).getAmount();
		}
		return 0;
	}

	private static boolean canReceiveEnergy(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.canReceive();
		}
		return false;
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
}