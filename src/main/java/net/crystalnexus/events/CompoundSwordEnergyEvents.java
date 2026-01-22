package net.crystalnexus.events;

import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.items.IItemHandler;

public class CompoundSwordEnergyEvents {

    private static final int ENERGY_COST_PER_HIT = 50; // tweak
    private static final ResourceLocation BATTERY_TAG =
            ResourceLocation.fromNamespaceAndPath("crystalnexus", "battery");

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        // server only
        if (player.level().isClientSide()) return;

        // Only our sword
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(CrystalnexusModItems.COMPOUND_SWORD.get())) return;

        // If we can't pay, cancel the attack (no damage)
        if (!consumeBatteryEnergy(player, ENERGY_COST_PER_HIT)) {
            event.setCanceled(true);

            // optional: throttle spam
            if (player.tickCount % 10 == 0) {
                player.displayClientMessage(
                        Component.literal("Out of power!").withStyle(ChatFormatting.RED),
                        true
                );
            }
        }
    }

    private boolean consumeBatteryEnergy(Player player, int cost) {
        IItemHandler inv = player.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (inv == null) return false;

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (!stack.is(ItemTags.create(BATTERY_TAG))) continue;

            IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM, null);
            if (energy == null) continue;

            if (energy.extractEnergy(cost, true) >= cost) {
                energy.extractEnergy(cost, false);
                return true;
            }
        }
        return false;
    }
}
