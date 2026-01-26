package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModBlocks;

public class NodeExtractorOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		String item = "";
		double outputAmount = 0;
		double cookTime = 0;
		double slotnumbercheck = 0;
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
			outputAmount = 150;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.CARBON_EFFICIENCY_UPGRADE.get()) {
			outputAmount = 200;
		} else {
			outputAmount = 100;
		}
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.ACCELERATION_UPGRADE.get()) {
			cookTime = 20;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) {
			cookTime = 10;
		} else {
			cookTime = 25;
		}
		double _cn_cookMult = 1.0;
		double _cn_outputMult = 1.0;
		boolean _cn_hasKeys = false;
		ItemStack _cn_upg = itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy();
		CompoundTag _cn_data = null;
		if (!_cn_upg.isEmpty() && _cn_upg.has(DataComponents.CUSTOM_DATA)) {
			CustomData _cn_cd = _cn_upg.get(DataComponents.CUSTOM_DATA);
			if (_cn_cd != null)
				_cn_data = _cn_cd.copyTag();
		}
		if (_cn_data != null && (_cn_data.contains("cook_mult") || _cn_data.contains("output_mult"))) {
			_cn_hasKeys = true;
			if (_cn_data.contains("cook_mult"))
				_cn_cookMult = _cn_data.getDouble("cook_mult");
			if (_cn_data.contains("output_mult"))
				_cn_outputMult = _cn_data.getDouble("output_mult");
		}
		// 2) Apply multipliers (STACK onto existing values)
		if (_cn_hasKeys) {
			_cn_cookMult = Math.max(0.05, Math.min(_cn_cookMult, 10.0));
			_cn_outputMult = Math.max(0.0, Math.min(_cn_outputMult, 10.0));
			cookTime = cookTime * _cn_cookMult;
			outputAmount = outputAmount * _cn_outputMult;
		}
		// 3) Output caps (machine cap + slot space cap)
		double MACHINE_MAX_OUTPUT = 4000; // set per machine
		if (outputAmount > MACHINE_MAX_OUTPUT)
			outputAmount = MACHINE_MAX_OUTPUT;
		double _cn_currentCount = itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount();
		double _cn_spaceLeft = 64 - _cn_currentCount; // assuming stack size 64
		if (outputAmount > _cn_spaceLeft)
			outputAmount = _cn_spaceLeft;
		if (outputAmount < 0)
			outputAmount = 0;
		if (cookTime < 1)
			cookTime = 1;
		if (!world.isClientSide()) {
			BlockPos _bp = BlockPos.containing(x, y, z);
			BlockEntity _blockEntity = world.getBlockEntity(_bp);
			BlockState _bs = world.getBlockState(_bp);
			if (_blockEntity != null)
				_blockEntity.getPersistentData().putDouble("maxProgress", cookTime);
			if (world instanceof Level _level)
				_level.sendBlockUpdated(_bp, _bs, _bs, 3);
		}
		slotnumbercheck = 0;
		if ((world.getBlockState(BlockPos.containing(x, y - 1, z))).getBlock() == CrystalnexusModBlocks.OIL_NODE.get() && 1024 <= extractEnergySimulate(world, BlockPos.containing(x, y, z), 1024, null)) {
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
					_level.sendParticles(ParticleTypes.DUST_PLUME, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.25, 0, 0.25, 0);
			}
			if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= cookTime) {
				if (getFluidTankCapacity(world, BlockPos.containing(x, y, z), 1, null) > getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null) + outputAmount) {
					if (world instanceof ILevelExtension _ext) {
						IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
						if (_fluidHandler != null)
							_fluidHandler.fill(new FluidStack(CrystalnexusModFluids.CRUDE_OIL.get(), 100), IFluidHandler.FluidAction.EXECUTE);
					}
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy(2048, false);
					}
				}
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putDouble("progress", 0);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x, y - 1, z))).getBlock() == CrystalnexusModBlocks.LAVA_NODE.get() && 1024 <= extractEnergySimulate(world, BlockPos.containing(x, y, z), 1024, null)) {
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
					_level.sendParticles(ParticleTypes.DUST_PLUME, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.25, 0, 0.25, 0);
			}
			if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= cookTime) {
				if (getFluidTankCapacity(world, BlockPos.containing(x, y, z), 1, null) > getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null) + outputAmount) {
					if (world instanceof ILevelExtension _ext) {
						IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
						if (_fluidHandler != null)
							_fluidHandler.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE);
					}
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy(2048, false);
					}
				}
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
					IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
					if (_entityStorage != null)
						_entityStorage.extractEnergy(1024, false);
				}
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
		IItemHandler handler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
		if (handler != null && slot >= 0 && slot < handler.getSlots()) {
			return handler.getStackInSlot(slot);
		}
	}
	return ItemStack.EMPTY;
}


	private static int extractEnergySimulate(LevelAccessor level, BlockPos pos, int amount, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.extractEnergy(amount, true);
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
}