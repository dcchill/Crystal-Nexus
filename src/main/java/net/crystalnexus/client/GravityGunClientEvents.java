package net.crystalnexus.client;

import net.crystalnexus.item.GravityGunItem;
import net.crystalnexus.network.GravityGunAdjustDistanceMessage;
import net.crystalnexus.network.GravityGunShootMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT)
public class GravityGunClientEvents {
	@SubscribeEvent
	public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.screen != null) {
			return;
		}

		ItemStack stack = heldGravityGun();
		if (stack.isEmpty() || !isHoldingEntity(stack)) {
			return;
		}

		int steps = event.getScrollDeltaY() > 0 ? 1 : -1;
		PacketDistributor.sendToServer(new GravityGunAdjustDistanceMessage(steps));
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.screen != null) {
			return;
		}
		ItemStack stack = heldGravityGun();
		if (stack.isEmpty()) {
			return;
		}
		if (event.isAttack() && isHoldingEntity(stack)) {
			PacketDistributor.sendToServer(new GravityGunShootMessage());
			event.setSwingHand(false);
			event.setCanceled(true);
		} else if (event.isUseItem()) {
			event.setSwingHand(false);
		}
	}

	private static ItemStack heldGravityGun() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = minecraft.player.getMainHandItem();
		if (stack.getItem() instanceof GravityGunItem) {
			return stack;
		}
		stack = minecraft.player.getOffhandItem();
		return stack.getItem() instanceof GravityGunItem ? stack : ItemStack.EMPTY;
	}

	private static boolean isHoldingEntity(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("gravityGunHolding");
	}
}
