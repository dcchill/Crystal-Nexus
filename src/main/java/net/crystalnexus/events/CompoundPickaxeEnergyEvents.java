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
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.IItemHandler;

public class CompoundPickaxeEnergyEvents {

    private static final int ENERGY_COST = 25;
    private static final ResourceLocation BATTERY_TAG =
            ResourceLocation.fromNamespaceAndPath("crystalnexus", "battery");

    // 1) "Starts breaking" – block mining should NOT even begin if no power
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack tool = player.getMainHandItem();
        if (!tool.is(CrystalnexusModItems.COMPOUND_PICKAXE.get())) return;

        if (!hasEnoughEnergyInAnyBattery(player, ENERGY_COST)) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.literal("Out of power!").withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    // 2) "Actually breaks" – drain energy once (and also cancel if somehow no power)
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.level().isClientSide()) return;

        ItemStack tool = player.getMainHandItem();
        if (!tool.is(CrystalnexusModItems.COMPOUND_PICKAXE.get())) return;

        if (!consumeEnergyFromAnyBattery(player, ENERGY_COST)) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.literal("Out of power!").withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    private boolean hasEnoughEnergyInAnyBattery(Player player, int cost) {
        IItemHandler inv = player.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (inv == null) return false;

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (!stack.is(ItemTags.create(BATTERY_TAG))) continue;

            IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM, null);
            if (energy == null) continue;

            if (energy.extractEnergy(cost, true) >= cost) return true;
        }
        return false;
    }

    private boolean consumeEnergyFromAnyBattery(Player player, int cost) {
        IItemHandler inv = player.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (inv == null) return false;

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (!stack.is(ItemTags.create(BATTERY_TAG))) continue;

            IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM, null);
            if (energy == null) continue;

            if (energy.extractEnergy(cost, true) >= cost) {
                energy.extractEnergy(cost, false); // drain for real
                return true;
            }
        }
        return false;
    }
}
