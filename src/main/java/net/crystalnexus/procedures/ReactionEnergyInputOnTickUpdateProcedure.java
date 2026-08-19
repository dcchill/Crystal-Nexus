package net.crystalnexus.procedures;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class ReactionEnergyInputOnTickUpdateProcedure {
	private static final int TRANSFER_PER_TICK = 512000;

	private ReactionEnergyInputOnTickUpdateProcedure() {
	}

	public static void execute(LevelAccessor world, BlockPos portPos) {
		CenteredMultiblockValidator.Link controller = CenteredMultiblockValidator.validateFromPort(world, portPos,
				CrystalnexusModBlocks.REACTION_CHAMBER_CORE.get(), CrystalnexusModBlocks.REACTION_CHAMBER_COMPUTER.get(), ReactionBlocksCheckerProcedure.CASING);
		if (controller == null || !(world instanceof ILevelExtension ext)) {
			return;
		}
		IEnergyStorage source = ext.getCapability(Capabilities.EnergyStorage.BLOCK, portPos, null);
		IEnergyStorage destination = ext.getCapability(Capabilities.EnergyStorage.BLOCK, controller.pos, null);
		if (source == null || destination == null) {
			return;
		}
		int transferLimit = TRANSFER_PER_TICK * CenteredMultiblockDimensions.sizeMultiplier(controller.radius);
		int energy = source.extractEnergy(transferLimit, true);
		energy = destination.receiveEnergy(energy, true);
		if (energy > 0) {
			source.extractEnergy(energy, false);
			destination.receiveEnergy(energy, false);
		}
	}
}