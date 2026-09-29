package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.crystalnexus.block.entity.ChlorophyteAcceleratorBlockEntity;

public class ChlorophyteAcceleratorOnTickUpdateProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		if (world instanceof ServerLevel level
				&& level.getBlockEntity(pos) instanceof ChlorophyteAcceleratorBlockEntity accelerator) {
			accelerator.runAccelerationCycle(level);
		}
    }
}
