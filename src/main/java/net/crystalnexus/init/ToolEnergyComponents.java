package net.crystalnexus.init;

import net.crystalnexus.CrystalnexusMod;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ToolEnergyComponents {
    @SubscribeEvent
    public static void removeDurability(ModifyDefaultComponentsEvent event) {
        event.modify(CrystalnexusModItems.HOVER_PACK_CHESTPLATE.get(), builder -> builder.remove(DataComponents.MAX_DAMAGE).remove(DataComponents.DAMAGE));
        event.modify(CrystalnexusModItems.FLORATHANE_WAND.get(), builder -> builder.remove(DataComponents.MAX_DAMAGE).remove(DataComponents.DAMAGE));
    }
}
