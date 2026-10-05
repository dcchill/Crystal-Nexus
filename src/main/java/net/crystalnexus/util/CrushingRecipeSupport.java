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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Collection;
import net.crystalnexus.CrystalnexusMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.processing.MaterialProcessingCatalog;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class CrushingRecipeSupport {
	private static RecipeManager cachedManager;
	private static Collection<RecipeHolder<?>> cachedSource;
	private static MaterialProcessingCatalog.Snapshot cachedMaterials;
	private record Candidate(Recipe<?> recipe, NonNullList<Ingredient> ingredients) {}
	private static List<Candidate> cachedCandidates = List.of();

	@SubscribeEvent
	public static synchronized void tagsUpdated(TagsUpdatedEvent event) {
		cachedManager = null;
		cachedSource = null;
		cachedMaterials = null;
		cachedCandidates = List.of();
	}

	private static synchronized List<Candidate> candidates(Level level) {
		RecipeManager manager = level.getRecipeManager();
		Collection<RecipeHolder<?>> source = manager.getRecipes();
		var materials = MaterialProcessingCatalog.get(level);
		if (manager != cachedManager || source != cachedSource || materials != cachedMaterials) {
			List<Candidate> found = new java.util.ArrayList<>();
			for (RecipeHolder<?> holder : source) {
				Recipe<?> recipe = holder.value();
				if (!(recipe instanceof OreCrushingJeiRecipe) && !isExternalCrushing(recipe)) continue;
				NonNullList<Ingredient> inputs = recipe instanceof OreCrushingJeiRecipe
						? recipe.getIngredients() : ingredients(recipe);
				if (recipe instanceof OreCrushingJeiRecipe || inputs.stream().flatMap(ingredient -> Arrays.stream(ingredient.getItems()))
						.noneMatch(input -> materials.source(input).isPresent()))
					found.add(new Candidate(recipe, inputs));
			}
			cachedCandidates = List.copyOf(found);
			cachedManager = manager;
			cachedSource = source;
			cachedMaterials = materials;
		}
		return cachedCandidates;
	}
	private CrushingRecipeSupport() {
	}

	public static ItemStack findResult(Level level, ItemStack input) {
		return findResult(level, input, MachineTier.CRYSTAL);
	}

	public static ItemStack findResult(Level level, ItemStack input, MachineTier machineTier) {
        var profiler = net.crystalnexus.commands.NexusDebugCommand.profiler(level);
        profiler.push("crystalnexus_recipe_lookup");
        try { return findResultInternal(level, input, machineTier); }
        finally { profiler.pop(); }
    }

    private static ItemStack findResultInternal(Level level, ItemStack input, MachineTier machineTier) {
		if (input.isEmpty())
			return ItemStack.EMPTY;

		
		
		
		ItemStack bestResult = ItemStack.EMPTY;
		int bestPriority = Integer.MAX_VALUE;
		int bestMinimumTier = 1;
		boolean matchedRecipe = false;
		for (Candidate candidate : candidates(level)) {
			Recipe<?> recipe = candidate.recipe();
			NonNullList<Ingredient> recipeIngredients = candidate.ingredients();
			if (recipeIngredients.isEmpty() || !recipeIngredients.getFirst().test(input)) continue;
			ItemStack result = ItemStack.EMPTY;
			int minimumTier = 1;
			if (recipe instanceof OreCrushingJeiRecipe crushingRecipe) {
				matchedRecipe = true;
				result = crushingRecipe.getResultItem(level.registryAccess());
				minimumTier = crushingRecipe.minimumMachineTier();
			} else {
				result = recipe.getResultItem(level.registryAccess());
				if (result.isEmpty())
					result = tryAssemble(recipe, new SingleRecipeInput(input), level);
				matchedRecipe |= !result.isEmpty();
			}
			int priority = outputPriority(result);
			if (priority < bestPriority) {
				bestResult = result;
				bestPriority = priority;
				bestMinimumTier = minimumTier;
			}
		}
		if (matchedRecipe)
			return !bestResult.isEmpty() && machineTier.supports(bestMinimumTier) ? bestResult : ItemStack.EMPTY;

		var materials = MaterialProcessingCatalog.get(level).materials().values();
		for (var material : materials) {
			if (!material.matchesSource(input) || material.profile().disabledStages().contains("crushing")) continue;
			ItemStack result = MaterialProcessingCatalog.generatedCrushingResult(material, input);
			int priority = outputPriority(result);
			if (priority < bestPriority) {
				bestResult = result;
				bestPriority = priority;
				bestMinimumTier = material.profile().minimumMachineTier();
			}
		}
		return !bestResult.isEmpty() && machineTier.supports(bestMinimumTier) ? bestResult : ItemStack.EMPTY;
	}

	public static List<OreCrushingJeiRecipe> jeiRecipes(Level level) {
		List<OreCrushingJeiRecipe> candidates = level.getRecipeManager().getRecipes().stream().map(RecipeHolder::value)
				.filter(recipe -> recipe instanceof OreCrushingJeiRecipe || isExternalCrushing(recipe))
				
				
				.filter(recipe -> recipe instanceof OreCrushingJeiRecipe || !isMaterialSource(level, recipe))
				.map(recipe -> toJeiRecipe(recipe, level))
				.filter(recipe -> recipe != null)
				.toList();
		return preferredPerInput(candidates, level);
	}

	private static List<OreCrushingJeiRecipe> preferredPerInput(List<OreCrushingJeiRecipe> recipes, Level level) {
		Map<ResourceLocation, OreCrushingJeiRecipe> preferred = new LinkedHashMap<>();
		for (OreCrushingJeiRecipe recipe : recipes) {
			if (recipe.getIngredients().isEmpty()) continue;
			ItemStack output = recipe.getResultItem(level.registryAccess());
			if (output.isEmpty()) continue;
			for (ItemStack input : recipe.getIngredients().getFirst().getItems()) {
				if (input.isEmpty()) continue;
				OreCrushingJeiRecipe candidate = new OreCrushingJeiRecipe(output.copy(),
					NonNullList.of(Ingredient.EMPTY, Ingredient.of(input.copyWithCount(1))), recipe.minimumMachineTier());
				ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
				preferred.merge(inputId, candidate, (current, next) ->
					outputPriority(next.getResultItem(level.registryAccess()))
						< outputPriority(current.getResultItem(level.registryAccess())) ? next : current);
			}
		}
		return List.copyOf(preferred.values());
	}

	private static int outputPriority(ItemStack output) {
		if (output.isEmpty()) return Integer.MAX_VALUE;
		ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(output.getItem());
		if (outputId.getNamespace().equals("crystalnexus")) return 0;
		String path = outputId.getPath().toLowerCase(java.util.Locale.ROOT);
		boolean taggedDust = isDustTagged(output);
		boolean namedDust = path.contains("dust");
		if (outputId.getNamespace().equals("alltheores") && (taggedDust || namedDust)) return 1;
		if (taggedDust) return 2;
		if (namedDust) return 3;
		return 4;
	}

	private static boolean isDustTagged(ItemStack stack) {
		return stack.getTags().anyMatch(tag -> {
			String path = tag.location().getPath();
			return (path.equals("dust") || path.equals("dusts") || path.startsWith("dust/")
					|| path.startsWith("dusts/"));
		});
	}

	private static boolean isMaterialSource(Level level, Recipe<?> recipe) {
		return ingredients(recipe).stream().flatMap(ingredient -> Arrays.stream(ingredient.getItems()))
				.anyMatch(input -> MaterialProcessingCatalog.get(level).source(input).isPresent());
	}

	public static List<OreCrushingJeiRecipe> generatedJeiRecipes(Level level) {
		List<OreCrushingJeiRecipe> explicit = level.getRecipeManager()
			.getAllRecipesFor(OreCrushingJeiRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList();
		List<OreCrushingJeiRecipe> generated = new java.util.ArrayList<>();
		for (var material : MaterialProcessingCatalog.get(level).materials().values()) {
			if (material.profile().disabledStages().contains("crushing")) continue;
			ItemStack[] sources = material.sourceIngredient().getItems();
			boolean overridden = explicit.stream().anyMatch(recipe -> !recipe.getIngredients().isEmpty()
				&& java.util.Arrays.stream(sources).anyMatch(recipe.getIngredients().getFirst()::test));
			if (overridden) continue;
			for (ItemStack source : sources) {
				ItemStack output = MaterialProcessingCatalog.generatedCrushingResult(material, source);
				if (output.isEmpty()) continue;
				generated.add(new OreCrushingJeiRecipe(output,
					NonNullList.of(Ingredient.EMPTY, Ingredient.of(source.copyWithCount(1))),
					material.profile().minimumMachineTier()));
			}
		}
		return preferredPerInput(generated, level);
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
