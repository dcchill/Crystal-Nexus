package net.crystalnexus.events;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.StarItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class StarRepairEvents {
    private StarRepairEvents() {}

    @SubscribeEvent
    public static void preventStarCombination(AnvilUpdateEvent event) {
        if (event.getLeft().getItem() instanceof StarItem && event.getRight().getItem() instanceof StarItem) {
            event.setCanceled(true);
        }
    }
}
