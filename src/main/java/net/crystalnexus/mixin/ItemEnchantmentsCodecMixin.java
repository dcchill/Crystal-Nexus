package net.crystalnexus.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.crystalnexus.recipe.CelestialGearForgeEnchanting;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEnchantments.class)
abstract class ItemEnchantmentsCodecMixin {
    // Accept legacy levels when decoding; the constructor clamps them to the balance cap.
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 255))
    private static int crystalnexus$allowHigherLevels(int maximum) {
        return Integer.MAX_VALUE;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 255))
    private int crystalnexus$allowHigherConstructedLevels(int maximum) {
        return Integer.MAX_VALUE;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void crystalnexus$capLevels(Object2IntOpenHashMap<Holder<Enchantment>> levels, boolean showInTooltip, CallbackInfo callback) {
        levels.replaceAll((enchantment, level) -> Math.min(level, CelestialGearForgeEnchanting.MAX_LEVEL));
    }
}
