package net.crystalnexus.mixin;

import net.crystalnexus.CrystalnexusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FlowingFluid.class, remap = false)
abstract class FlowingFluidMixin {
    @Inject(method = "canHoldFluid", at = @At("HEAD"), cancellable = true)
    private void crystalnexus$protectBlocksFromWater(BlockGetter level, BlockPos pos, BlockState state,
            Fluid fluid, CallbackInfoReturnable<Boolean> callback) {
        if (fluid.is(FluidTags.WATER) && !(state.getBlock() instanceof LiquidBlock)
                && !(state.getBlock() instanceof LiquidBlockContainer)
                && BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(CrystalnexusMod.MODID))
            callback.setReturnValue(false);
    }
}
