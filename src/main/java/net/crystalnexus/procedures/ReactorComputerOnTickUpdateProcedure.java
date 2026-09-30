package net.crystalnexus.procedures;

import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.crystalnexus.reactor.ReactorSimulation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public class ReactorComputerOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
        if (world.isClientSide()) return;
		BlockPos controllerPos = BlockPos.containing(x, y, z);
		if (world.getBlockEntity(controllerPos) instanceof ReactorComputerBlockEntity computer) {
			if (computer.shouldRecheckLayout(20)) {
				BlocksCheckerProcedure.executeFromController(world, controllerPos);
			}
			computer.pullFuelInputs();
			ReactorSimulation.tick(world, controllerPos, computer);
			computer.pushEnergyOutputs();
			computer.pushSpentCells();
		}
	}
}
