package net.crystalnexus.events;

import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

public class CompoundSwordEnergyEvents {

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
        if (!net.crystalnexus.item.ToolEnergy.consume(player, weapon, CrystalnexusConfig.ITEMS.COMPOUND_SWORD.energyCost(), false)) {
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

}
