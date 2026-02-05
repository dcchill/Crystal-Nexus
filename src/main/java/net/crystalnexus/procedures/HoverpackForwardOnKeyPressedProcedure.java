package net.crystalnexus.procedures;

import net.minecraft.world.entity.Entity;

public class HoverpackForwardOnKeyPressedProcedure {
	public static void execute(Entity entity) {
		if (entity == null) return;
		entity.getPersistentData().putBoolean("cn_hp_fwd", true);
	}
}
