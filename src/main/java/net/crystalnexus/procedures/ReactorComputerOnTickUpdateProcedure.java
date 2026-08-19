package net.crystalnexus.procedures;

import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.crystalnexus.reactor.ReactorSimulation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public class ReactorComputerOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockPos controllerPos = BlockPos.containing(x, y, z);
		BlocksCheckerProcedure.executeFromController(world, controllerPos);
		if (world.getBlockEntity(controllerPos) instanceof ReactorComputerBlockEntity computer) {
			ReactorSimulation.tick(world, controllerPos, computer);
		}
	}
}
