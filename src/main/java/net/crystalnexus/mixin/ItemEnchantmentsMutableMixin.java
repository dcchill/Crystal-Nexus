package net.crystalnexus.mixin;

import net.crystalnexus.recipe.CelestialGearForgeEnchanting;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ItemEnchantments.Mutable.class)
abstract class ItemEnchantmentsMutableMixin {
    @ModifyConstant(method = {"set", "upgrade"}, constant = @Constant(intValue = 255), remap = false)
    private int crystalnexus$maximumLevel(int maximum) {
        return CelestialGearForgeEnchanting.MAX_LEVEL;
    }
}
