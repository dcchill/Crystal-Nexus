package net.crystalnexus.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.crystalnexus.init.CrystalnexusModItems;

public class DiamondSingularityItemInInventoryTickProcedure {
    public static void execute(LivingEntity entity) {
        if (entity == null) return;

        if (entity instanceof Player player) {
            boolean hasSingularity = false;

            // Check if the player has the diamond singularity
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && stack.getItem() == CrystalnexusModItems.DIAMOND_SINGULARITY.get()) {
                    hasSingularity = true;
                    break;
                }
            }

            // Loop through inventory
            for (ItemStack stack : player.getInventory().items) {
                if (stack.isEmpty()) continue;

                // Only affect damageable items (tools/weapons/armor)
                if (stack.isDamageableItem()) {
                    if (hasSingularity) {
                        // Reset damage to 0 so it never wears down
                        stack.setDamageValue(0);
                    }
                }
            }
        }
    }
}
