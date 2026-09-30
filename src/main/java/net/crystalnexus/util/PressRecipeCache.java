package net.crystalnexus.util;

import net.crystalnexus.jei_recipes.CircuitPressingRecipe;
import net.crystalnexus.jei_recipes.TitaniumCarbideCircuitPressRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Objects;

/** Caches recipe selection only; output space, FE, and processing remain live each tick. */
public final class PressRecipeCache {
    public record Match(TitaniumCarbideCircuitPressRecipe advanced, ItemStack result) {}

    private Level level;
    private Object recipes;
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack material = ItemStack.EMPTY;
    private FluidStack fluid = FluidStack.EMPTY;
    private ResourceLocation assigned;
    private boolean batch;
    private Match match;

    public Match find(Level currentLevel, ItemStack currentInput, ItemStack currentMaterial,
            FluidStack currentFluid, boolean batchPress, ResourceLocation assignedRecipe) {
        Object currentRecipes = currentLevel.getRecipeManager().getRecipes();
        if (match != null && level == currentLevel && recipes == currentRecipes && batch == batchPress
                && Objects.equals(assigned, assignedRecipe)
                && sameStack(input, currentInput) && sameStack(material, currentMaterial)
                && fluid.getAmount() == currentFluid.getAmount()
                && FluidStack.isSameFluidSameComponents(fluid, currentFluid)) return match;

        TitaniumCarbideCircuitPressRecipe advanced = null;
        if (batchPress) {
            for (var holder : currentLevel.getRecipeManager().getAllRecipesFor(TitaniumCarbideCircuitPressRecipe.Type.INSTANCE)) {
                var recipe = holder.value();
                if (recipe.itemInput(0).isPresent() && recipe.itemInput(0).get().test(currentInput)
                        && recipe.itemInput(1).map(ingredient -> ingredient.test(currentMaterial)).orElse(true)
                        && recipe.fluidInput(0).isPresent()
                        && recipe.fluidInput(0).get().matches(currentFluid)) {
                    advanced = recipe;
                    break;
                }
            }
        }

        ItemStack result = ItemStack.EMPTY;
        if (advanced != null) {
            result = advanced.getResultItem(null).copy();
        } else {
            for (var holder : currentLevel.getRecipeManager().getAllRecipesFor(CircuitPressingRecipe.Type.INSTANCE)) {
                if (assignedRecipe != null && !assignedRecipe.equals(holder.id())) continue;
                var ingredients = holder.value().getIngredients();
                if (ingredients.get(0).test(currentInput) && ingredients.get(1).test(currentMaterial)) {
                    result = holder.value().getResultItem(null).copy();
                    break;
                }
            }
        }

        match = new Match(advanced, result);
        level = currentLevel;
        recipes = currentRecipes;
        input = currentInput.copy();
        material = currentMaterial.copy();
        fluid = currentFluid.copy();
        batch = batchPress;
        assigned = assignedRecipe;
        return match;
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        return left.getCount() == right.getCount() && ItemStack.isSameItemSameComponents(left, right);
    }

    public void clear() {
        level = null;
        recipes = null;
        input = ItemStack.EMPTY;
        material = ItemStack.EMPTY;
        fluid = FluidStack.EMPTY;
        assigned = null;
        match = null;
    }
}
