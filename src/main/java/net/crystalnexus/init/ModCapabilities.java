package net.crystalnexus.init;

import net.crystalnexus.item.BatteryCellItem;
import net.crystalnexus.item.BatteryEnergyStorage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;         
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.crystalnexus.item.DenseBatteryCellItem;
import net.crystalnexus.item.CarbonBatteryCellItem;

@EventBusSubscriber(modid = "crystalnexus", bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new BatteryEnergyStorage(stack, BatteryCellItem.CAPACITY, BatteryCellItem.MAX_IO),
            CrystalnexusModItems.BATTERY_CELL.get()
        );
      event.registerItem(
    Capabilities.EnergyStorage.ITEM,
    (stack, ctx) -> new BatteryEnergyStorage(stack, DenseBatteryCellItem.CAPACITY, DenseBatteryCellItem.MAX_IO),
    CrystalnexusModItems.DENSE_BATTERY_CELL.get()
);

      event.registerItem(
    Capabilities.EnergyStorage.ITEM,
    (stack, ctx) -> new BatteryEnergyStorage(stack, CarbonBatteryCellItem.CAPACITY, CarbonBatteryCellItem.MAX_IO),
    CrystalnexusModItems.CARBON_BATTERY_CELL.get()
);

    }
    
}
