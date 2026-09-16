package net.crystalnexus.events;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.ToolEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class ToolEnergyTooltip {
    @SubscribeEvent
    public static void addEnergy(ItemTooltipEvent event) {
        var energy = event.getItemStack().getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy != null && ToolEnergy.isTool(event.getItemStack())) {
            event.getToolTip().add(Component.literal(String.format("Energy: %,d / %,d FE",
                energy.getEnergyStored(), energy.getMaxEnergyStored())).withStyle(ChatFormatting.GREEN));
        }
    }
}
