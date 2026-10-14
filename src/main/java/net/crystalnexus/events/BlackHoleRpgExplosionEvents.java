package net.crystalnexus.events;

import net.crystalnexus.entity.BlackHoleProjectileEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = "crystalnexus")
public final class BlackHoleRpgExplosionEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void detonate(ExplosionEvent.Detonate event) {
        if (!(event.getExplosion().getDirectSourceEntity() instanceof BlackHoleProjectileEntity shot)) return;
        event.getAffectedEntities().removeIf(entity -> entity instanceof ItemEntity);
        // Harvest explicitly to preserve loot regardless of explosion-decay gamerules.
        for (var pos : event.getAffectedBlocks()) shot.breakBlock(pos, false);
        event.getAffectedBlocks().clear();
    }
}
