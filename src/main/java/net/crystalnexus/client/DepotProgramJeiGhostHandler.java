package net.crystalnexus.client;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.crystalnexus.client.gui.DepotCliScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class DepotProgramJeiGhostHandler implements IGhostIngredientHandler<DepotCliScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(DepotCliScreen screen, ITypedIngredient<I> ingredient,
            boolean doStart) {
        ItemStack stack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) return List.of();
        List<Target<I>> targets = new ArrayList<>();
        screen.jeiProgramItemInputs().forEach(input -> targets.add(target(screen, input)));
        EditBox machine = screen.jeiProgramMachineInput();
        if (machine != null && stack.getItem() instanceof BlockItem) targets.add(target(screen, machine));
        return List.copyOf(targets);
    }

    private static <I> Target<I> target(DepotCliScreen screen, EditBox input) {
        return new Target<>() {
            @Override
            public Rect2i getArea() {
                return new Rect2i(input.getX(), input.getY(), input.getWidth(), input.getHeight());
            }

            @Override
            public void accept(I ingredient) {
                if (ingredient instanceof ItemStack stack) screen.acceptJeiIngredient(input, stack);
            }
        };
    }

    @Override
    public void onComplete() {
    }
}
