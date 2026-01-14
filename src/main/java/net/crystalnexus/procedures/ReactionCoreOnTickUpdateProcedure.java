package net.crystalnexus.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class ReactionCoreOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockState computer = Blocks.AIR.defaultBlockState();
		BlockState input = Blocks.AIR.defaultBlockState();
		BlockState output = Blocks.AIR.defaultBlockState();
		double computerX = 0;
		double computerZ = 0;
		double mbPt = 0;
		double fePt = 0;
		double energy = 0;
		computer = CrystalnexusModBlocks.REACTION_CHAMBER_COMPUTER.get().defaultBlockState();
		output = CrystalnexusModBlocks.REACTION_ENERGY_INPUT.get().defaultBlockState();
		if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).getBlock() == computer.getBlock()) {
			computerX = 1;
		} else if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).getBlock() == computer.getBlock()) {
			computerX = -1;
		} else if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).getBlock() == computer.getBlock()) {
			computerZ = 1;
		} else if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).getBlock() == computer.getBlock()) {
			computerZ = -1;
		}
		if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).getBlock() == output.getBlock()) {
			if (getMaxEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null) != getEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null)) {
				energy = extractEnergySimulate(world, BlockPos.containing(x + 1, y, z), 2048000, null);
				energy = receiveEnergySimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), (int) energy, null);
				if (energy <= getEnergyStored(world, BlockPos.containing(x + 1, y, z), null)) {
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + 1, y, z), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy((int) energy, false);
					}
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_entityStorage != null)
							_entityStorage.receiveEnergy((int) energy, false);
					}
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).getBlock() == output.getBlock()) {
			if (getMaxEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null) != getEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null)) {
				energy = extractEnergySimulate(world, BlockPos.containing(x - 1, y, z), 2048000, null);
				energy = receiveEnergySimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), (int) energy, null);
				if (energy <= getEnergyStored(world, BlockPos.containing(x - 1, y, z), null)) {
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x - 1, y, z), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy((int) energy, false);
					}
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_entityStorage != null)
							_entityStorage.receiveEnergy((int) energy, false);
					}
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).getBlock() == output.getBlock()) {
			if (getMaxEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null) != getEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null)) {
				energy = extractEnergySimulate(world, BlockPos.containing(x, y, z + 1), 2048000, null);
				energy = receiveEnergySimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), (int) energy, null);
				if (energy <= getEnergyStored(world, BlockPos.containing(x, y, z + 1), null)) {
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z + 1), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy((int) energy, false);
					}
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_entityStorage != null)
							_entityStorage.receiveEnergy((int) energy, false);
					}
				}
			}
		} else if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).getBlock() == output.getBlock()) {
			if (getMaxEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null) != getEnergyStored(world, BlockPos.containing(x + computerX, y, z + computerZ), null)) {
				energy = extractEnergySimulate(world, BlockPos.containing(x, y, z - 1), 2048000, null);
				energy = receiveEnergySimulate(world, BlockPos.containing(x + computerX, y, z + computerZ), (int) energy, null);
				if (energy <= getEnergyStored(world, BlockPos.containing(x, y, z - 1), null)) {
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z - 1), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy((int) energy, false);
					}
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + computerX, y, z + computerZ), null);
						if (_entityStorage != null)
							_entityStorage.receiveEnergy((int) energy, false);
					}
				}
			}
		}
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