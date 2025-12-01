package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;

public class MatterTransmutationTableCancelCraftProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		CrystalPurifierBlockAddedProcedure.execute(world, x, y, z);
	}
}