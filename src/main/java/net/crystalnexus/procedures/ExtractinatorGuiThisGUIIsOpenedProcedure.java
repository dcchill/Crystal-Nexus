package net.crystalnexus.procedures;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

public class ExtractinatorGuiThisGUIIsOpenedProcedure {
	public static double execute(LevelAccessor world, double x, double y, double z) {
		double frame = 0;
		if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") > 0 && getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") < 100) {
			frame = 1 + frame;
			return frame;
		} else if (2 == frame) {
			frame = 1;
		}
		return 1;
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
	}
}