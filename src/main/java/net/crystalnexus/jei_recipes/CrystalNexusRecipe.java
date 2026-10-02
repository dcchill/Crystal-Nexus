package net.crystalnexus.jei_recipes;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

public interface CrystalNexusRecipe extends Recipe<RecipeInput> {
	default int getInputCount(int index) {
		return 1;
	}

	@Override
	default boolean matches(RecipeInput input, Level level) {
		if (level.isClientSide() || input == null) {
			return false;
		}
		NonNullList<Ingredient> ingredients = getIngredients();
		if (ingredients.isEmpty() || ingredients.size() > input.size()) {
			return false;
		}
		for (int i = 0; i < ingredients.size(); i++) {
			Ingredient ingredient = ingredients.get(i);
			if (ingredient == null || ingredient.isEmpty()) {
				continue;
			}
			if (!ingredient.test(input.getItem(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	default boolean isSpecial() {
		return true;
	}
}
