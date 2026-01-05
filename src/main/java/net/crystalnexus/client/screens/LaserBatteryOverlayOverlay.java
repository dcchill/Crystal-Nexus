package net.crystalnexus.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

import net.crystalnexus.procedures.LaserBatteryOverlayCheckerProcedure;
import net.crystalnexus.procedures.GetCooldownProcedure;

@EventBusSubscriber({Dist.CLIENT})
public class LaserBatteryOverlayOverlay {
	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		int w = event.getGuiGraphics().guiWidth();
		int h = event.getGuiGraphics().guiHeight();
		Level world = null;
		double x = 0;
		double y = 0;
		double z = 0;
		Player entity = Minecraft.getInstance().player;
		if (entity != null) {
			world = entity.level();
			x = entity.getX();
			y = entity.getY();
			z = entity.getZ();
		}
		if (LaserBatteryOverlayCheckerProcedure.execute(entity)) {

			event.getGuiGraphics().blit(ResourceLocation.parse("crystalnexus:textures/screens/battery_sprite.png"), w / 2 + 78, h / 2 + 72, 0, Mth.clamp((int) GetCooldownProcedure.execute(entity) * 64, 0, 576), 64, 64, 64, 640);

		}
	}
}