package net.crystalnexus.procedures;

import net.minecraft.world.entity.Entity;

public class HoverpackRiseOnKeyReleasedProcedure {
	public static void execute(Entity entity) {
		if (entity == null) return;
		entity.getPersistentData().putBoolean("cn_hoverpack_rise", false);
	}
}
