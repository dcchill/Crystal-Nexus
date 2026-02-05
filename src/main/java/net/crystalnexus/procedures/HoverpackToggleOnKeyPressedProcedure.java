package net.crystalnexus.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

public class HoverpackToggleOnKeyPressedProcedure {
	private static final String PD_TOGGLE = "cn_hoverpack_enabled";

	public static void execute(Entity entity) {
		if (entity == null) return;

		boolean enabled = entity.getPersistentData().getBoolean(PD_TOGGLE);
		enabled = !enabled;

		entity.getPersistentData().putBoolean(PD_TOGGLE, enabled);

		// Message (client-side only)
		if (entity instanceof Player player && player.level().isClientSide) {
			player.displayClientMessage(Component.literal(enabled ? "Hover Pack Toggle" : "Hover Pack Toggle"), true);
		}
	}
}
