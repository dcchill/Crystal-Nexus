package net.crystalnexus.util;

import net.crystalnexus.jei_recipes.ArcFurnaceRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Converts item-only alloy recipes to the furnace's existing two-slot recipe format. */
public final class ArcFurnaceRecipeSupport {
	private static RecipeManager cachedManager;
	private static Collection<RecipeHolder<?>> cachedSource;
	private static List<ArcFurnaceRecipe> cachedRecipes = List.of();

	private ArcFurnaceRecipeSupport() {}

	public static synchronized List<ArcFurnaceRecipe> externalRecipes(Level level) {
		RecipeManager manager = level.getRecipeManager();
		Collection<RecipeHolder<?>> source = manager.getRecipes();
		if (manager != cachedManager || source != cachedSource) {
			List<ArcFurnaceRecipe> found = new ArrayList<>();
			for (RecipeHolder<?> holder : source) {
				try {
					ArcFurnaceRecipe converted = convert(holder, level.registryAccess(), level);
					if (converted != null) found.add(converted);
				} catch (RuntimeException | LinkageError ignored) {
					// Optional mods can expose recipes whose inputs cannot be read without their machine.
				}
			}
			cachedRecipes = List.copyOf(found);
			cachedManager = manager;
			cachedSource = source;
		}
		return cachedRecipes;
	}

	static ArcFurnaceRecipe convert(RecipeHolder<?> holder, HolderLookup.Provider registries, Level level) {
		Recipe<?> recipe = holder.value();
		ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
		if (recipe instanceof ArcFurnaceRecipe || holder.id().getNamespace().equals("crystalnexus")
				|| recipe.getType() == RecipeType.CRAFTING || recipe instanceof CraftingRecipe
				|| type == null || !(type.getPath().contains("alloy") || holder.id().getPath().contains("alloy"))) return null;

		if (type.toString().equals("enderio:alloy_smelting")
				&& recipe.getClass().getName().equals("com.enderio.machines.common.blocks.alloy.AlloySmeltingRecipe"))
			return convertEnderIo(recipe);
		ItemStack output = recipe.getResultItem(registries);
		if (output.isEmpty()) return null;
		List<Ingredient> ingredients = recipe.getIngredients();
		List<Ingredient> distinct = new ArrayList<>(2);
		List<Integer> counts = new ArrayList<>(2);
		for (Ingredient ingredient : ingredients) {
			ItemStack[] items = ingredient.getItems();
			if (items.length == 0) return null;
			int count = items[0].getCount();
			if (count < 1 || Arrays.stream(items).anyMatch(stack -> stack.isEmpty() || stack.getCount() != count
					|| !stack.getCraftingRemainingItem().isEmpty())) return null;
			if (indexOf(distinct, ingredient) >= 0) return null;
			distinct.add(ingredient);
			counts.add(count);
			if (distinct.size() > 2) return null;
		}
		if (distinct.size() != 2 || counts.stream().anyMatch(count -> count > 64)) return null;

		if (level == null || !verifiesItemOnly(recipe, distinct, counts, output, level)) return null;
		return new ArcFurnaceRecipe(output.copy(), NonNullList.of(Ingredient.EMPTY,
			distinct.get(0), distinct.get(1)), counts, 1);
	}

	private static ArcFurnaceRecipe convertEnderIo(Recipe<?> recipe) {
		try {
			if ((boolean) recipe.getClass().getMethod("isSmelting").invoke(recipe)) return null;
			Object result = recipe.getClass().getMethod("output").invoke(recipe);
			if (!(result instanceof ItemStack output) || output.isEmpty()) return null;
			Object raw = recipe.getClass().getMethod("inputs").invoke(recipe);
			if (!(raw instanceof List<?> inputs) || inputs.size() != 2
					|| !(inputs.get(0) instanceof SizedIngredient first)
					|| !(inputs.get(1) instanceof SizedIngredient second)
					|| first.count() < 1 || first.count() > 64 || second.count() < 1 || second.count() > 64
					|| first.ingredient().getItems().length == 0 || second.ingredient().getItems().length == 0
					|| Arrays.stream(first.ingredient().getItems()).anyMatch(stack -> !stack.getCraftingRemainingItem().isEmpty())
					|| Arrays.stream(second.ingredient().getItems()).anyMatch(stack -> !stack.getCraftingRemainingItem().isEmpty())
					|| indexOf(List.of(first.ingredient()), second.ingredient()) >= 0) return null;
			return new ArcFurnaceRecipe(output.copy(), NonNullList.of(Ingredient.EMPTY,
				first.ingredient(), second.ingredient()), List.of(first.count(), second.count()), 1);
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return null;
		}
	}

	private static int indexOf(List<Ingredient> distinct, Ingredient candidate) {
		ItemStack[] items = candidate.getItems();
		for (int i = 0; i < distinct.size(); i++) {
			ItemStack[] existing = distinct.get(i).getItems();
			if (items.length == existing.length && Arrays.stream(items).allMatch(item ->
				Arrays.stream(existing).anyMatch(other -> ItemStack.isSameItemSameComponents(item, other)))) return i;
		}
		return -1;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean verifiesItemOnly(Recipe<?> recipe, List<Ingredient> ingredients,
			List<Integer> counts, ItemStack output, Level level) {
		try {
			Recipe<RecipeInput> candidate = (Recipe) recipe;
			ItemStack first = ingredients.get(0).getItems()[0].copyWithCount(counts.get(0));
			ItemStack second = ingredients.get(1).getItems()[0].copyWithCount(counts.get(1));
			CraftingInput input = CraftingInput.of(2, 1, List.of(first, second));
			CraftingInput swapped = CraftingInput.of(2, 1, List.of(second, first));
			if (!candidate.matches(input, level) || !candidate.matches(swapped, level)) return false;
			if (candidate.getRemainingItems(input).stream().anyMatch(stack -> !stack.isEmpty())) return false;
			ItemStack assembled = candidate.assemble(input, level.registryAccess());
			ItemStack assembledSwapped = candidate.assemble(swapped, level.registryAccess());
			if (!ItemStack.isSameItemSameComponents(output, assembled) || output.getCount() != assembled.getCount()
					|| !ItemStack.isSameItemSameComponents(output, assembledSwapped)
					|| output.getCount() != assembledSwapped.getCount()) return false;
			for (int i = 0; i < 2; i++) if (counts.get(i) > 1) {
				ItemStack reduced = (i == 0 ? first : second).copyWithCount(counts.get(i) - 1);
				CraftingInput shortInput = CraftingInput.of(2, 1,
					i == 0 ? List.of(reduced, second) : List.of(first, reduced));
				if (candidate.matches(shortInput, level)) return false;
			}
			return true;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}
}
