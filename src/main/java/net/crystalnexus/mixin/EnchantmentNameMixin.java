package net.crystalnexus.mixin;

import net.crystalnexus.item.EnchantmentLevelFormat;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Enchantment.class)
abstract class EnchantmentNameMixin {
    @Redirect(method = "getFullname", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"), remap = false)
    private static MutableComponent crystalnexus$formatLevel(String key, Holder<Enchantment> enchantment, int level) {
        return Component.literal(EnchantmentLevelFormat.format(level));
    }
}
