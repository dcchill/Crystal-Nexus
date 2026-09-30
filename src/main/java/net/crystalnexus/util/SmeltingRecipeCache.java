package net.crystalnexus.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/** A per-machine result cache. The recipe collection changes identity on recipe replacement. */
public final class SmeltingRecipeCache {
    public record Match(boolean present, ItemStack result) {}

    private Level level;
    private Object recipes;
    private ItemStack input = ItemStack.EMPTY;
    private Match match;

    public Match find(Level currentLevel, ItemStack currentInput) {
        Object currentRecipes = currentLevel.getRecipeManager().getRecipes();
        if (match != null && level == currentLevel && recipes == currentRecipes
                && input.getCount() == currentInput.getCount()
                && ItemStack.isSameItemSameComponents(input, currentInput)) return match;

        var recipe = currentLevel.getRecipeManager().getRecipeFor(RecipeType.SMELTING,
            new SingleRecipeInput(currentInput), currentLevel);
        match = new Match(recipe.isPresent(), recipe.map(holder ->
            holder.value().getResultItem(currentLevel.registryAccess()).copy()).orElse(ItemStack.EMPTY));
        level = currentLevel;
        recipes = currentRecipes;
        input = currentInput.copy();
        return match;
    }

    public void clear() {
        level = null;
        recipes = null;
        input = ItemStack.EMPTY;
        match = null;
    }
}
