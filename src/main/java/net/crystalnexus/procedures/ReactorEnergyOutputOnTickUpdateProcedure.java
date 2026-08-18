package net.crystalnexus.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.block.entity.ReactorEnergyOutputBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;

public class ReactorEnergyOutputOnTickUpdateProcedure {
	private static final int CONTROLLER_TRANSFER_PER_TICK = 65536;

	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockPos portPos = BlockPos.containing(x, y, z);
		CenteredMultiblockValidator.Link controller = CenteredMultiblockValidator.validateFromPort(world, portPos,
				CrystalnexusModBlocks.REACTOR_CORE.get(), CrystalnexusModBlocks.REACTOR_COMPUTER.get(), BlocksCheckerProcedure.CASING);
		if (controller != null) {
			pullFromController(world, controller.pos(), portPos, CenteredMultiblockDimensions.sizeMultiplier(controller.radius()));
		}
		double energy = 0;
		if (canReceiveEnergy(world, BlockPos.containing(x + 1, y, z), Direction.WEST)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), 10024000, null);
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
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), 10024000, null);
			energy = receiveEnergySimulate(world, BlockPos.containing(x - 1, y, z), (int) energy, Direction.EAST);
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy((int) energy, false);
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x - 1, y, z), Direction.EAST);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) energy, false);
			}
		}
		if (canReceiveEnergy(world, BlockPos.containing(x, y, z + 1), Direction.NORTH)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), 10024000, null);
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
		if (canReceiveEnergy(world, BlockPos.containing(x, y, z - 1), Direction.SOUTH)) {
			energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), 10024000, null);
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
	}

	private static void pullFromController(LevelAccessor world, BlockPos controllerPos, BlockPos portPos, int sizeMultiplier) {
		if (!(world instanceof ILevelExtension ext)
				|| !(world.getBlockEntity(portPos) instanceof ReactorEnergyOutputBlockEntity output)) {
			return;
		}
		IEnergyStorage controller = ext.getCapability(Capabilities.EnergyStorage.BLOCK, controllerPos, null);
		if (controller == null) {
			return;
		}
		int energy = controller.extractEnergy(CONTROLLER_TRANSFER_PER_TICK * sizeMultiplier, true);
		energy = output.getEnergyStorage().generateEnergy(energy, true);
		if (energy > 0) {
			controller.extractEnergy(energy, false);
			output.getEnergyStorage().generateEnergy(energy, false);
		}
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
