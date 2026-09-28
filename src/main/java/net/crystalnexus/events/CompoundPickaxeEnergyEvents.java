package net.crystalnexus.events;

import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class CompoundPickaxeEnergyEvents {

    
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack tool = player.getMainHandItem();
        if (!tool.is(CrystalnexusModItems.COMPOUND_PICKAXE.get())) return;

        int energyCost = CrystalnexusConfig.ITEMS.COMPOUND_PICKAXE.energyCost();
        if (!net.crystalnexus.item.ToolEnergy.consume(player, tool, energyCost, true)) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.literal("Out of power!").withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.level().isClientSide()) return;

        ItemStack tool = player.getMainHandItem();
        if (!tool.is(CrystalnexusModItems.COMPOUND_PICKAXE.get())) return;

        int energyCost = CrystalnexusConfig.ITEMS.COMPOUND_PICKAXE.energyCost();
        if (!net.crystalnexus.item.ToolEnergy.consume(player, tool, energyCost, false)) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.literal("Out of power!").withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

}
