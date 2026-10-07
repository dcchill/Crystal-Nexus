package net.crystalnexus.mixin;

import net.crystalnexus.CrystalnexusMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockBehaviour.BlockStateBase.class, remap = false)
abstract class BlockStateWaterProtectionMixin {
    @Inject(method = "canBeReplaced(Lnet/minecraft/world/level/material/Fluid;)Z", at = @At("HEAD"), cancellable = true)
    private void crystalnexus$preventWaterReplacement(Fluid fluid, CallbackInfoReturnable<Boolean> callback) {
        var block = ((BlockBehaviour.BlockStateBase) (Object) this).getBlock();
        if (fluid.is(FluidTags.WATER) && !(block instanceof LiquidBlock)
                && BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(CrystalnexusMod.MODID))
            callback.setReturnValue(false);
    }
}
