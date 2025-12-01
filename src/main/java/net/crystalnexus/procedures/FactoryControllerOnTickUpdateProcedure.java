package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class FactoryControllerOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double energy = 0;
		double slotcheck = 0;
		if (canReceiveEnergy(world,
				BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
						(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
						(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
				null)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), 16038, null);
			energy = receiveEnergySimulate(world,
					BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
					(int) energy, Direction.DOWN);
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy((int) energy, false);
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK,
						BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
								(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
								(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
						Direction.DOWN);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) energy, false);
			}
		}
		if (canReceiveEnergy(world,
				BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 10).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
						(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 10).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
						(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 10).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
				null)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), 16038, null);
			energy = receiveEnergySimulate(world,
					BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 10).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 10).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 10).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
					(int) energy, Direction.DOWN);
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy((int) energy, false);
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK,
						BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 10).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
								(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 10).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
								(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 10).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
						Direction.DOWN);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) energy, false);
			}
		}
		if (canReceiveEnergy(world,
				BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 11).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
						(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 11).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
						(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 11).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
				null)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), 16038, null);
			energy = receiveEnergySimulate(world,
					BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 11).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 11).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 11).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
					(int) energy, Direction.DOWN);
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy((int) energy, false);
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK,
						BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 11).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
								(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 11).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
								(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 11).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
						Direction.DOWN);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) energy, false);
			}
		}
		if (canReceiveEnergy(world,
				BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 12).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
						(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 12).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
						(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 12).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
				null)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), 16038, null);
			energy = receiveEnergySimulate(world,
					BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 12).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 12).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 12).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
					(int) energy, Direction.DOWN);
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy((int) energy, false);
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK,
						BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 12).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
								(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 12).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
								(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 12).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
						Direction.DOWN);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) energy, false);
			}
		}
		if (canReceiveEnergy(world,
				BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 13).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
						(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 13).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
						(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 13).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
				null)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), 16038, null);
			energy = receiveEnergySimulate(world,
					BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 13).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 13).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 13).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
					(int) energy, Direction.DOWN);
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy((int) energy, false);
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK,
						BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 13).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkX"),
								(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 13).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkY"),
								(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 13).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("linkZ")),
						Direction.DOWN);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) energy, false);
			}
		}
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
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