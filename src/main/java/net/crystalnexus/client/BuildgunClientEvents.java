package net.crystalnexus.client;

import net.crystalnexus.item.BuildGunItem;
import net.crystalnexus.network.BuildgunAdjustPlacementMessage;
import net.crystalnexus.network.BuildgunMenuMessage;
import net.crystalnexus.network.BuildgunTogglePlacementModeMessage;
import net.crystalnexus.init.CrystalnexusModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT)
public class BuildgunClientEvents {
	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.screen != null) {
			return;
		}
		while (CrystalnexusModKeyMappings.BUILDGUN_MENU.consumeClick()) {
			if (heldBuildgun().isEmpty()) {
				continue;
			}
			PacketDistributor.sendToServer(new BuildgunMenuMessage(0, 0));
		}
	}

	@SubscribeEvent
	public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.screen != null) {
			return;
		}
		ItemStack stack = heldBuildgun();
		if (stack.isEmpty()) {
			return;
		}
		if (!stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("buildgunPlacementActive")) {
			return;
		}

		int steps = event.getScrollDeltaY() > 0 ? 1 : -1;
		PacketDistributor.sendToServer(new BuildgunAdjustPlacementMessage(steps, minecraft.player.isShiftKeyDown()));
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.screen != null) {
			return;
		}
		if (!event.isAttack() || !minecraft.player.isShiftKeyDown()) {
			return;
		}
		if (heldBuildgun().isEmpty()) {
			return;
		}
		PacketDistributor.sendToServer(new BuildgunTogglePlacementModeMessage());
		event.setSwingHand(false);
		event.setCanceled(true);
	}

	private static ItemStack heldBuildgun() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = minecraft.player.getMainHandItem();
		if (stack.getItem() instanceof BuildGunItem) {
			return stack;
		}
		stack = minecraft.player.getOffhandItem();
		return stack.getItem() instanceof BuildGunItem ? stack : ItemStack.EMPTY;
	}
}
