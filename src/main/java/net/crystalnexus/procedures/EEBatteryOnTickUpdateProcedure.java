package net.crystalnexus.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.crystalnexus.CrystalnexusMod;

import net.crystalnexus.init.CrystalnexusModBlocks;

import java.util.ArrayList;
import java.util.List;

public class EEBatteryOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (!(world instanceof ILevelExtension level)) return;

		BlockPos pos = BlockPos.containing(x, y, z);
		Block batteryBlock = CrystalnexusModBlocks.EE_BATTERY.get();
		int maxTransfer = 2048000; // FE per tick per connection
		
		// ---- Get this battery's energy ----
		IEnergyStorage selfStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
		if (selfStorage == null)
			return;

		// ---- Find connected batteries ----
		List<BlockPos> connectedBatteries = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			BlockPos neighborPos = pos.relative(dir);
			if (world.getBlockState(neighborPos).getBlock() == batteryBlock)
				connectedBatteries.add(neighborPos);
		}

		// ---- Balance energy between connected batteries ----
		if (!connectedBatteries.isEmpty()) {
			List<IEnergyStorage> storages = new ArrayList<>();
			storages.add(selfStorage);

			for (BlockPos bp : connectedBatteries) {
				IEnergyStorage neighborStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, bp, null);
				if (neighborStorage != null)
					storages.add(neighborStorage);
			}

			// Compute total and average energy
			int totalEnergy = 0;
			for (IEnergyStorage storage : storages)
				totalEnergy += storage.getEnergyStored();

			int average = totalEnergy / storages.size();

			// Balance toward average
			for (IEnergyStorage storage : storages) {
				int diff = average - storage.getEnergyStored();
				if (diff > 0) {
					// Needs energy: pull from others
					for (IEnergyStorage donor : storages) {
						if (donor == storage) continue;
						int toTransfer = diff;
						if (toTransfer > 0) {
							donor.extractEnergy(toTransfer, false);
							storage.receiveEnergy(toTransfer, false);
							diff -= toTransfer;
							if (diff <= 0) break;
						}
					}
				}
			}
		}

		// ---- Push excess energy to non-battery blocks ----
		for (Direction dir : Direction.values()) {
			BlockPos neighbor = pos.relative(dir);
			if (world.getBlockState(neighbor).getBlock() == batteryBlock)
				continue; // skip connected batteries

			IEnergyStorage neighborStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, dir.getOpposite());
			if (neighborStorage == null || !neighborStorage.canReceive()) continue;

			int extractSim = selfStorage.extractEnergy(maxTransfer, true);
			int receiveSim = neighborStorage.receiveEnergy(extractSim, true);
			int actual = Math.min(extractSim, receiveSim);

			if (actual > 0) {
				selfStorage.extractEnergy(actual, false);
				neighborStorage.receiveEnergy(actual, false);
			}
		}

		{
			int _value = (int) EnergyBlockDisplayProcedure.execute(world, x, y, z);
			BlockPos _pos = BlockPos.containing(x, y, z);
			BlockState _bs = world.getBlockState(_pos);
			if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
				world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
		}
	}
}
