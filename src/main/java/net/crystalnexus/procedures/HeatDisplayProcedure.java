package net.crystalnexus.procedures;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

public class HeatDisplayProcedure {
	public static double execute(LevelAccessor world, double x, double y, double z) {
		if (1 == (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat") / getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxHeat")) * 10 + 1) {
			return 0;
		}
		return (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat") / getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxHeat")) * 10 + 1;
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
	}
}