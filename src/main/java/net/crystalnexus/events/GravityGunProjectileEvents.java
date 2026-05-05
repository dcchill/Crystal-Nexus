package net.crystalnexus.events;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.GravityGunItem;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public class GravityGunProjectileEvents {
	@SubscribeEvent
	public static void onEntityTickPre(EntityTickEvent.Pre event) {
		if (event.getEntity() instanceof FallingBlockEntity fallingBlock) {
			GravityGunItem.prepareHeldFallingBlock(fallingBlock);
		}
	}

	@SubscribeEvent
	public static void onEntityTickPost(EntityTickEvent.Post event) {
		if (event.getEntity() instanceof FallingBlockEntity fallingBlock) {
			GravityGunItem.tickThrownBlockProjectile(fallingBlock);
		}
	}
}
