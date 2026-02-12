package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class LUNADroneOnInitialEntitySpawnProcedure {
	public static boolean execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return false;
		boolean found = false;
		double sx = 0;
		double sy = 0;
		double sz = 0;
		double xx = 0;
		double yy = 0;
		double zz = 0;
		sx = -3;
		found = false;
		for (int index0 = 0; index0 < 6; index0++) {
			sy = -3;
			for (int index1 = 0; index1 < 6; index1++) {
				sz = -3;
				for (int index2 = 0; index2 < 6; index2++) {
					if ((world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz))).getBlock() == CrystalnexusModBlocks.CONTAINER.get()) {
						xx = x + sx;
						yy = y + sy;
						zz = z + sz;
						found = true;
					}
					sz = sz + 1;
				}
				sy = sy + 1;
			}
			sx = sx + 1;
		}
		if (found == true) {
			entity.getPersistentData().putDouble("homeX", xx);
			entity.getPersistentData().putDouble("homeY", yy);
			entity.getPersistentData().putDouble("homeZ", zz);
			entity.getPersistentData().putBoolean("goDest", true);
			entity.getPersistentData().putBoolean("goHome", false);
		}
		return true;
	}
}