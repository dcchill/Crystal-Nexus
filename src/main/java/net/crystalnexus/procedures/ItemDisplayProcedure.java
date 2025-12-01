package net.crystalnexus.procedures;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

public class ItemDisplayProcedure {
	public static double execute(LevelAccessor world, double x, double y, double z) {
		if (1 == (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "item") / getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxItem")) * 10 + 1) {
			return 0;
		}
		return (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "item") / getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxItem")) * 10 + 1;
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
	}
}