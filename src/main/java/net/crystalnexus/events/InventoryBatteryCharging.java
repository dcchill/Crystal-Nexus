package net.crystalnexus.events;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.ToolEnergy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class InventoryBatteryCharging {
    @SubscribeEvent
    public static void chargeInventory(PlayerTickEvent.Post event) {
        chargeInventory(event.getEntity());
    }

    public static void chargeInventory(Player player) {
        if (player.level().isClientSide() || player.isSpectator()) return;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack battery = player.getInventory().getItem(slot);
            if (battery.isEmpty() || ToolEnergy.isTool(battery)) continue;
            IEnergyStorage source = battery.getCapability(Capabilities.EnergyStorage.ITEM);
            if (source == null || !source.canExtract()) continue;
            int remaining = source.extractEnergy(Integer.MAX_VALUE, true);
            for (int targetSlot = 0; targetSlot < player.getInventory().getContainerSize() && remaining > 0; targetSlot++) {
                ItemStack target = player.getInventory().getItem(targetSlot);
                if (target.isEmpty() || !ToolEnergy.isTool(target)) continue;
                IEnergyStorage destination = target.getCapability(Capabilities.EnergyStorage.ITEM);
                if (destination == null || !destination.canReceive()) continue;
                int accepted = destination.receiveEnergy(remaining, true);
                int extracted = source.extractEnergy(accepted, false);
                destination.receiveEnergy(extracted, false);
                remaining -= extracted;
            }
        }
    }
}
