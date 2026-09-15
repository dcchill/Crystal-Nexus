package net.crystalnexus.events;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.PrisonCubeItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class PrisonCubeEvents {
	private PrisonCubeEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		handleCapture(event, event.getTarget());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		handleCapture(event, event.getTarget());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		ItemStack stack = event.getItemStack();
		Player player = event.getEntity();
		if (!(stack.getItem() instanceof PrisonCubeItem cube) || !player.isShiftKeyDown() || !PrisonCubeItem.hasStoredEntity(stack)) {
			return;
		}

		InteractionResult result = event.getLevel() instanceof ServerLevel serverLevel
				? cube.release(stack, player, serverLevel, event.getPos(), event.getFace())
				: InteractionResult.SUCCESS;
		event.setCancellationResult(result);
		event.setCanceled(true);
	}

	private static void handleCapture(PlayerInteractEvent event, Entity target) {
		ItemStack stack = event.getItemStack();
		if (!(stack.getItem() instanceof PrisonCubeItem cube)) {
			return;
		}

		InteractionResult result = cube.capture(stack, event.getEntity(), target);
		if (result == InteractionResult.PASS) {
			return;
		}
		if (event instanceof PlayerInteractEvent.EntityInteractSpecific specific) {
			specific.setCancellationResult(result);
			specific.setCanceled(true);
		} else if (event instanceof PlayerInteractEvent.EntityInteract general) {
			general.setCancellationResult(result);
			general.setCanceled(true);
		}
	}
}
