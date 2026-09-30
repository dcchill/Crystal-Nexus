package net.crystalnexus.client;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.item.MeteorSwordItem;
import net.crystalnexus.item.SolarisItem;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, value = Dist.CLIENT)
public final class MeteorSwordChargeOverlay {
	private MeteorSwordChargeOverlay() { }

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.player.isUsingItem() || minecraft.player.tickCount % 4 != 0) return;
		var stack = minecraft.player.getMainHandItem();
		if (stack.is(CrystalnexusModItems.METEOR_SWORD.get()) && MeteorSwordItem.chargeLevel(stack) > 0)
			minecraft.player.displayClientMessage(MeteorSwordItem.chargeStatus(stack), true);
		else if (stack.is(CrystalnexusModItems.SOLARIS.get()))
			minecraft.player.displayClientMessage(SolarisItem.chargeStatus(stack), true);
	}
}
