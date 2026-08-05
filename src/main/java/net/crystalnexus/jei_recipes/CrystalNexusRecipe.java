package net.crystalnexus.jei_recipes;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

public interface CrystalNexusRecipe extends Recipe<RecipeInput> {
	default int getInputCount(int index) {
		return 1;
	}

	@Override
	default boolean isSpecial() {
		return true;
	}
}
