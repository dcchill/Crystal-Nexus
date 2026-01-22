package net.crystalnexus.init;

import net.crystalnexus.item.BatteryCellItem;
import net.crystalnexus.item.BatteryEnergyStorage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;          // ✅ correct
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = "crystalnexus", bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new BatteryEnergyStorage(stack, BatteryCellItem.CAPACITY, BatteryCellItem.MAX_IO),
            CrystalnexusModItems.BATTERY_CELL.get()
        );
    }
}
