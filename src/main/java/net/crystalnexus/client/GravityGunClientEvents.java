package net.crystalnexus.client;

import net.crystalnexus.item.GravityGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class GravityGunClientEvents {
	@SubscribeEvent
	public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.screen != null || !event.isUseItem()) {
			return;
		}
		if (heldGravityGun().isEmpty()) {
			return;
		}
		event.setSwingHand(false);
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
}
