package net.crystalnexus.client;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.crystalnexus.client.gui.AssemblyLineScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Ghost-only JEI integration; drops are configuration and never grant resources. */
public final class AssemblyLineJeiGhostHandler implements IGhostIngredientHandler<AssemblyLineScreen> {
    @Override public <I> List<Target<I>> getTargetsTyped(AssemblyLineScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        // Try item first using correct JEI API
        Optional<ItemStack> optItem = ingredient.getItemStack();
        if (optItem.isPresent() && !optItem.get().isEmpty()) {
            ItemStack stack = optItem.get();
            return screen.ghostTargets(stack).stream().<Target<I>>map(target -> new Target<>() {
                @Override public Rect2i getArea() { return target.area(); }
                @Override public void accept(I value) { screen.acceptGhostItem(target.nodeId(), target.socket(), stack); }
            }).toList();
        }
        // Try fluid - JEI doesn't directly expose fluids in the same way, but we can try to cast
        // Use reflection to check if it's a fluid ingredient
        try {
            Object ingredientObj = ingredient;
            java.lang.reflect.Method getType = ingredient.getClass().getMethod("getType");
            Object type = getType.invoke(ingredientObj);
            // Check if type class name contains "Fluid"
            if (type.getClass().getSimpleName().contains("Fluid")) {
                // Try to get fluid via reflection
                java.lang.reflect.Method getFluid = ingredient.getClass().getMethod("getFluid");
                Object fluidObj = getFluid.invoke(ingredientObj);
                if (fluidObj instanceof FluidStack fluid && !fluid.isEmpty()) {
                    return screen.ghostFluidTargets(fluid).stream().<Target<I>>map(target -> new Target<>() {
                        @Override public Rect2i getArea() { return target.area(); }
                        @Override public void accept(I value) { screen.acceptGhostFluid(target.nodeId(), target.socket(), fluid); }
                    }).toList();
                }
            }
        } catch (Exception e) {
            // Not a fluid or reflection failed
        }
        return List.of();
    }
    @Override public void onComplete() { }
}
