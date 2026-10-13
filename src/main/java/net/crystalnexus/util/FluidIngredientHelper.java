package net.crystalnexus.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** Matches a recipe's fluid or another mod's fluid in its corresponding common tag. */
public final class FluidIngredientHelper {
    private FluidIngredientHelper() {}

    public static boolean matches(FluidStack stack, Fluid fluid) {
        if (stack.isEmpty()) return false;
        String path = BuiltInRegistries.FLUID.getKey(fluid).getPath();
        if (path.startsWith("flowing_")) path = path.substring("flowing_".length());
        return stack.is(fluid) || stack.is(TagKey.create(Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath("c", path)));
    }
}
