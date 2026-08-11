package net.crystalnexus.util;

import net.crystalnexus.jei_recipes.OreCrushingJeiRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

public final class CrushingRecipeSupport {
	private CrushingRecipeSupport() {
	}

	public static ItemStack findResult(Level level, ItemStack input) {
		if (input.isEmpty())
			return ItemStack.EMPTY;

		for (RecipeHolder<OreCrushingJeiRecipe> holder : level.getRecipeManager().getAllRecipesFor(OreCrushingJeiRecipe.Type.INSTANCE)) {
			OreCrushingJeiRecipe recipe = holder.value();
			if (!recipe.getIngredients().isEmpty() && recipe.getIngredients().getFirst().test(input))
				return recipe.getResultItem(level.registryAccess());
		}

		SingleRecipeInput recipeInput = new SingleRecipeInput(input);
		for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
			Recipe<?> recipe = holder.value();
			if (isExternalCrushing(recipe)) {
				ItemStack result = tryAssemble(recipe, recipeInput, level);
				if (!result.isEmpty())
					return result;
			}
		}
		return ItemStack.EMPTY;
	}

	public static List<OreCrushingJeiRecipe> jeiRecipes(Level level) {
		return level.getRecipeManager().getRecipes().stream().map(RecipeHolder::value)
				.filter(recipe -> recipe instanceof OreCrushingJeiRecipe || isExternalCrushing(recipe))
				.map(recipe -> toJeiRecipe(recipe, level))
				.filter(recipe -> recipe != null)
				.toList();
	}

	private static boolean isExternalCrushing(Recipe<?> recipe) {
		ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
		return typeId != null && typeId.getPath().equals("crushing");
	}

	private static OreCrushingJeiRecipe toJeiRecipe(Recipe<?> recipe, Level level) {
		if (recipe instanceof OreCrushingJeiRecipe crushingRecipe)
			return crushingRecipe;
		NonNullList<Ingredient> ingredients = ingredients(recipe);
		ItemStack output = recipe.getResultItem(level.registryAccess());
		return ingredients.isEmpty() || output.isEmpty() ? null : new OreCrushingJeiRecipe(output, ingredients);
	}

	private static NonNullList<Ingredient> ingredients(Recipe<?> recipe) {
		NonNullList<Ingredient> ingredients = recipe.getIngredients();
		if (!ingredients.isEmpty())
			return ingredients;

		try {
			Object input = recipe.getClass().getMethod("getInput").invoke(recipe);
			Object representations = input.getClass().getMethod("getRepresentations").invoke(input);
			if (representations instanceof List<?> list) {
				ItemStack[] stacks = list.stream().filter(ItemStack.class::isInstance)
						.map(ItemStack.class::cast).map(ItemStack::copy).toArray(ItemStack[]::new);
				if (stacks.length > 0)
					return NonNullList.of(Ingredient.EMPTY, Ingredient.of(stacks));
			}
		} catch (ReflectiveOperationException | LinkageError ignored) {
			// Optional recipe APIs are deliberately not compile-time dependencies.
		}
		return NonNullList.create();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static ItemStack tryAssemble(Recipe<?> recipe, SingleRecipeInput input, Level level) {
		try {
			Recipe<RecipeInput> genericRecipe = (Recipe) recipe;
			return genericRecipe.matches(input, level)
					? genericRecipe.assemble(input, level.registryAccess())
					: ItemStack.EMPTY;
		} catch (ClassCastException | UnsupportedOperationException ignored) {
			return ItemStack.EMPTY;
		}
	}
}
