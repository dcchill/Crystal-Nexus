package net.crystalnexus.recipe;

import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Shared by the forge and JEI, so previews use the actual merging and power rules. */
public final class CelestialGearForgeEnchanting {
    public static final int MAX_LEVEL = 1000;
    private CelestialGearForgeEnchanting() {}

    public record Operation(ItemStack result, int energyPerTick) {}

    public static int starBoost(ItemStack star) {
        return star.is(CrystalnexusModItems.ZERO_STAR.get()) ? 16 : star.is(CrystalnexusModItems.PINK_STAR.get()) ? 8 : star.is(CrystalnexusModItems.BLUE_STAR.get()) ? 4
            : star.is(CrystalnexusModItems.ORANGE_STAR.get()) ? 2 : star.is(CrystalnexusModItems.YELLOW_DWARF_STAR.get()) ? 1 : 0;
    }

    public static int levelCap(ItemStack star) { return star.is(CrystalnexusModItems.ZERO_STAR.get()) ? MAX_LEVEL : 255; }

    public static int bookLimit(ItemStack star) { return Math.min(7, starBoost(star)); }

    public static boolean hasDonors(List<ItemStack> inputs) {
        return inputs.subList(1, CelestialGearForgeRecipe.STAR_SLOT).stream().anyMatch(stack -> !stack.isEmpty());
    }

    @Nullable
    public static Operation calculate(List<ItemStack> inputs, @Nullable Holder<Enchantment> selected) {
        if (inputs.size() < CelestialGearForgeRecipe.INPUT_COUNT || inputs.getFirst().isEmpty()) return null;
        int boost = starBoost(inputs.get(CelestialGearForgeRecipe.STAR_SLOT));
        if (boost == 0) return null;
        int cap = levelCap(inputs.get(CelestialGearForgeRecipe.STAR_SLOT));
        ItemStack target = inputs.getFirst(), result = target.copyWithCount(1);
        var original = EnchantmentHelper.getEnchantmentsForCrafting(target);
        if (!hasDonors(inputs)) {
            if (selected == null || original.getLevel(selected) == 0 || original.getLevel(selected) >= cap) return null;
            EnchantmentHelper.updateEnchantments(result, mutable -> mutable.set(selected, (int) Math.min(cap, (long) original.getLevel(selected) + boost)));
        } else {
            int count = 0;
            boolean book = target.is(Items.ENCHANTED_BOOK);
            for (int slot = 1; slot < CelestialGearForgeRecipe.STAR_SLOT; slot++) {
                ItemStack donor = inputs.get(slot);
                if (donor.isEmpty()) continue;
                if (!donor.is(Items.ENCHANTED_BOOK) || ++count > bookLimit(inputs.get(CelestialGearForgeRecipe.STAR_SLOT))) return null;
                var before = EnchantmentHelper.getEnchantmentsForCrafting(result);
                var entries = EnchantmentHelper.getEnchantmentsForCrafting(donor).entrySet().stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().unwrapKey().orElseThrow().location().toString())).toList();
                EnchantmentHelper.updateEnchantments(result, mutable -> {
                    for (var entry : entries) {
                        var enchantment = entry.getKey();
                        if (!book && !enchantment.value().canEnchant(target)) continue;
                        if (mutable.keySet().stream().anyMatch(existing -> !existing.equals(enchantment) && !Enchantment.areCompatible(existing, enchantment))) continue;
                        int current = mutable.getLevel(enchantment), incoming = entry.getIntValue();
                        mutable.set(enchantment, (int) Math.min(cap, current == incoming ? (long) current + 1 : Math.max(current, incoming)));
                    }
                });
                if (before.equals(EnchantmentHelper.getEnchantmentsForCrafting(result))) return null;
            }
        }
        long cost = 0;
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(result).entrySet()) {
            if (entry.getIntValue() > original.getLevel(entry.getKey())) {
                long level = entry.getIntValue();
                long levelCost = level > 14654 ? Integer.MAX_VALUE : 10L * level * level;
                cost = Math.min(Integer.MAX_VALUE, cost + levelCost);
            }
        }
        return cost == 0 ? null : new Operation(result, (int) Math.min(Integer.MAX_VALUE, cost));
    }
}
