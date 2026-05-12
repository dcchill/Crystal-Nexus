package net.crystalnexus.init;

import net.crystalnexus.item.BatteryCellItem;
import net.crystalnexus.item.BatteryEnergyStorage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;         
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.crystalnexus.item.DenseBatteryCellItem;
import net.crystalnexus.item.CarbonBatteryCellItem;
import net.crystalnexus.item.DarkBatteryCellItem;

@EventBusSubscriber(modid = "crystalnexus", bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new BatteryEnergyStorage(stack, BatteryCellItem.capacity(), BatteryCellItem.maxReceive(), BatteryCellItem.maxExtract()),
            CrystalnexusModItems.BATTERY_CELL.get()
        );
      event.registerItem(
    Capabilities.EnergyStorage.ITEM,
    (stack, ctx) -> new BatteryEnergyStorage(stack, DenseBatteryCellItem.capacity(), DenseBatteryCellItem.maxReceive(), DenseBatteryCellItem.maxExtract()),
    CrystalnexusModItems.DENSE_BATTERY_CELL.get()
);

      event.registerItem(
    Capabilities.EnergyStorage.ITEM,
    (stack, ctx) -> new BatteryEnergyStorage(stack, CarbonBatteryCellItem.capacity(), CarbonBatteryCellItem.maxReceive(), CarbonBatteryCellItem.maxExtract()),
    CrystalnexusModItems.CARBON_BATTERY_CELL.get()
);

      event.registerItem(
    Capabilities.EnergyStorage.ITEM,
    (stack, ctx) -> new BatteryEnergyStorage(stack, DarkBatteryCellItem.capacity(), DarkBatteryCellItem.maxReceive(), DarkBatteryCellItem.maxExtract()),
    CrystalnexusModItems.DARK_BATTERY_CELL.get()
);


    }
    
}
