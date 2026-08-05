package net.crystalnexus.cli;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.jei_recipes.CrystalNexusRecipe;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DepotCraftingService {
    private static final int MAX_DEPTH = 32;
    private static final int MAX_STEPS = 20_000;
    private static final int TICKS_PER_CRAFT = 20;

    public record Result(boolean success, ItemStack output, DepotSavedData.CraftingJob job, List<String> details) {}
    public record IngredientPlan(IntArrayList choices, Map<ResourceLocation, Integer> required) {}
    public record AvailableRecipe(ResourceLocation id, Recipe<?> recipe, ItemStack output, boolean processing) {}
    public record RecipeChoice(ResourceLocation id, ItemStack output, boolean processing, String category,
            List<DepotJeiRecipeCache.Slot> inputs, List<ResourceLocation> machineTypes) {}

    private DepotCraftingService() {
    }

    public static Result craft(ServerPlayer player, DepotSavedData depot, Item target, int requested) {
        int processors = DepotNetwork.craftingProcessorCount(player);
        if (processors <= 0) {
            return new Result(false, ItemStack.EMPTY, null, List.of(
                    "Crafting service unavailable.",
                    "Connect a Crafting Processor to use this command."));
        }
        if (depot.getCraftingJob() != null) {
            return new Result(false, ItemStack.EMPTY, null, List.of(
                    "A crafting job is already active.",
                    "Use queue to inspect it or queue cancel <id> to cancel it."));
        }
        ResourceLocation targetId = BuiltInRegistries.ITEM.getKey(target);
        if (targetId == null || target == Items.AIR || requested <= 0) {
            return new Result(false, ItemStack.EMPTY, null, List.of("Invalid crafting target."));
        }

        Planner planner = new Planner(player, depot);
        Optional<Planned> planned = planner.plan(targetId, requested);
        if (planned.isEmpty()) {
            List<String> details = planner.missingDetails();
            return new Result(false, ItemStack.EMPTY, null,
                    details.isEmpty() ? List.of("No complete crafting path is available from stored items.") : details);
        }
        Map<ResourceLocation, Long> finalCounts = planned.get().counts();
        if (!planner.fits(finalCounts)) {
            return new Result(false, ItemStack.EMPTY, null, List.of("Depot storage capacity reached."));
        }

        long produced = finalCounts.getOrDefault(targetId, 0L) - planner.initialCount(targetId);
        Optional<ExecutionPlan> execution = executionPlan(planned.get().steps());
        long totalWork = execution.isEmpty() ? -1 : execution.get().totalWork();
        if (produced <= 0 || totalWork <= 0) {
            return new Result(false, ItemStack.EMPTY, null, List.of("The crafting plan produced no usable output."));
        }
        DepotSavedData.CraftingJob job = depot.startCraftingJob(targetId,
                (int) Math.min(Integer.MAX_VALUE, produced), totalWork, execution.get().peakItems(),
                execution.get().baseInputs(), execution.get().outputs(), planned.get().steps());
        if (job == null) return new Result(false, ItemStack.EMPTY, null, List.of("Unable to reserve the crafting ingredients."));
        return new Result(true, new ItemStack(target, job.amount()), job, List.of());
    }

    public static long estimatedTicks(long remainingWork, int processors) {
        return remainingWork <= 0 ? 0 : processors <= 0 ? Long.MAX_VALUE : 1 + (remainingWork - 1) / processors;
    }

    public static int maxCraftable(DepotSavedData depot, RecipeHolder<CraftingRecipe> recipe) {
        StackedContents contents = new StackedContents();
        depot.fillStackedContents(contents);
        return contents.getBiggestCraftableStack(recipe, new IntArrayList());
    }

    public static Optional<IngredientPlan> plan(DepotSavedData depot, CraftingRecipe recipe, int amount) {
        if (amount <= 0) return Optional.empty();
        StackedContents contents = new StackedContents();
        depot.fillStackedContents(contents);
        IntArrayList choices = new IntArrayList();
        if (!contents.canCraft(recipe, choices, amount)) return Optional.empty();
        Map<ResourceLocation, Integer> required = new HashMap<>();
        for (int choice : choices) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(StackedContents.fromStackingIndex(choice).getItem());
            required.merge(id, amount, Integer::sum);
        }
        if (required.entrySet().stream().anyMatch(entry -> depot.getCount(entry.getKey()) < entry.getValue())) return Optional.empty();
        return Optional.of(new IngredientPlan(choices, Map.copyOf(required)));
    }

    public static List<AvailableRecipe> recipesFor(ServerPlayer player, Item outputItem) {
        return availableRecipes(player).stream().filter(candidate -> candidate.output().is(outputItem)).toList();
    }

    public static List<RecipeChoice> recipeChoices(ServerPlayer player, Item outputItem) {
        List<RecipeChoice> choices = new ArrayList<>();
        List<DepotJeiRecipeCache.Recipe> synced = DepotJeiRecipeCache.recipesFor(player,
                BuiltInRegistries.ITEM.getKey(outputItem));
        for (AvailableRecipe candidate : recipesFor(player, outputItem)) {
            ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(candidate.recipe().getType());
            choices.add(new RecipeChoice(candidate.id(), candidate.output(), candidate.processing(),
                    candidate.processing() && typeId != null ? friendly(typeId.getPath()) : "Crafting",
                    slots(candidate.recipe()), List.of()));
        }
        for (DepotJeiRecipeCache.Recipe recipe : synced) {
            choices.add(new RecipeChoice(recipe.id(), new ItemStack(BuiltInRegistries.ITEM.get(
                    recipe.primaryOutput().itemId()), recipe.primaryOutput().count()), true,
                    recipe.categoryName(), recipe.inputs(), recipe.machineTypes()));
        }
        return choices.stream().sorted(Comparator.comparing(RecipeChoice::processing)
                .thenComparing(choice -> choice.category().toLowerCase(java.util.Locale.ROOT))
                .thenComparing(choice -> choice.id().toString())).toList();
    }

    private static List<DepotJeiRecipeCache.Slot> slots(Recipe<?> recipe) {
        List<DepotJeiRecipeCache.Slot> slots = new ArrayList<>();
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (int index = 0; index < ingredients.size(); index++) {
            Ingredient ingredient = ingredients.get(index);
            if (ingredient.isEmpty()) continue;
            int count = recipe instanceof CrystalNexusRecipe custom ? custom.getInputCount(index) : 1;
            List<DepotJeiRecipeCache.StackRef> alternatives = java.util.Arrays.stream(ingredient.getItems())
                    .filter(stack -> !stack.isEmpty()).map(stack -> new DepotJeiRecipeCache.StackRef(
                            BuiltInRegistries.ITEM.getKey(stack.getItem()), Math.max(count, stack.getCount())))
                    .distinct().toList();
            if (!alternatives.isEmpty()) slots.add(new DepotJeiRecipeCache.Slot(alternatives));
        }
        return List.copyOf(slots);
    }

    private static String friendly(String value) {
        String[] words = value.replace('/', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    public static List<AvailableRecipe> availableRecipes(ServerPlayer player) {
        RecipeManager manager = player.serverLevel().getRecipeManager();
        List<AvailableRecipe> result = new ArrayList<>();
        for (RecipeType<?> type : BuiltInRegistries.RECIPE_TYPE) {
            boolean processing = type != RecipeType.CRAFTING;
            ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(type);
            if (processing && typeId != null && typeId.getPath().contains("guide")) continue;
            // Skip tag/lookup/group recipe types — they are informational groupings, not
            // actionable machine recipes. This prevents items from being "craftable" via
            // tag-based categories that have no real processing machines behind them.
            if (processing && typeId != null) {
                String path = typeId.getPath().toLowerCase(java.util.Locale.ROOT);
                if (path.contains("tag") || path.contains("lookup") || path.contains("group")) continue;
            }
            for (RecipeHolder<?> holder : recipesForType(manager, type)) {
                Recipe<?> recipe = holder.value();
                ItemStack output = recipe.getResultItem(player.serverLevel().registryAccess());
                if (!output.isEmpty() && output.getCount() > 0 && !recipe.getIngredients().isEmpty()) {
                    result.add(new AvailableRecipe(holder.id(), recipe, output.copy(), processing));
                }
            }
        }
        result.sort(Comparator.comparing(candidate -> candidate.id().toString()));
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<RecipeHolder<?>> recipesForType(RecipeManager manager, RecipeType<?> type) {
        return (List) manager.getAllRecipesFor((RecipeType) type);
    }

    private static CraftingInput craftingInput(CraftingRecipe recipe, IntArrayList choices) {
        List<ResourceLocation> ids = new ArrayList<>(choices.size());
        for (int choice : choices) {
            ids.add(BuiltInRegistries.ITEM.getKey(StackedContents.fromStackingIndex(choice).getItem()));
        }
        return craftingInput(recipe, ids);
    }

    private static CraftingInput craftingInput(CraftingRecipe recipe, List<ResourceLocation> choices) {
        NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        int choice = 0;
        for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
            if (ingredients.get(ingredient).isEmpty()) continue;
            int slot = recipe instanceof ShapedRecipe ? ingredient % width + ingredient / width * 3 : choice;
            Item item = BuiltInRegistries.ITEM.get(choices.get(choice++));
            grid.set(slot, new ItemStack(item));
        }
        return CraftingInput.of(3, 3, grid);
    }

    private static Optional<ExecutionPlan> executionPlan(List<DepotSavedData.CraftingStep> steps) {
        Map<ResourceLocation, Long> base = new HashMap<>();
        Map<ResourceLocation, Long> virtual = new HashMap<>();
        long totalWork = 0;
        for (DepotSavedData.CraftingStep step : steps) {
            totalWork = Planner.add(totalWork, step.work());
            if (totalWork < 0) return Optional.empty();
            for (DepotSavedData.SlotEntry input : step.inputs()) {
                long have = virtual.getOrDefault(input.itemId(), 0L);
                if (have < input.count()) {
                    long shortage = input.count() - have;
                    if (!Planner.increase(base, input.itemId(), shortage)
                            || !Planner.increase(virtual, input.itemId(), shortage)) return Optional.empty();
                }
                long left = virtual.get(input.itemId()) - input.count();
                if (left == 0) virtual.remove(input.itemId());
                else virtual.put(input.itemId(), left);
            }
            for (Map.Entry<ResourceLocation, Long> output : step.outputs().entrySet()) {
                if (!Planner.increase(virtual, output.getKey(), output.getValue())) return Optional.empty();
            }
        }

        Map<ResourceLocation, Long> working = new HashMap<>(base);
        long peak = total(working);
        for (DepotSavedData.CraftingStep step : steps) {
            for (DepotSavedData.SlotEntry input : step.inputs()) {
                long left = working.getOrDefault(input.itemId(), 0L) - input.count();
                if (left < 0) return Optional.empty();
                if (left == 0) working.remove(input.itemId());
                else working.put(input.itemId(), left);
            }
            for (Map.Entry<ResourceLocation, Long> output : step.outputs().entrySet()) {
                if (!Planner.increase(working, output.getKey(), output.getValue())) return Optional.empty();
            }
            long items = total(working);
            if (items < 0) return Optional.empty();
            peak = Math.max(peak, items);
        }
        return working.isEmpty() ? Optional.empty()
                : Optional.of(new ExecutionPlan(totalWork, peak, Map.copyOf(base), Map.copyOf(working)));
    }

    private static long total(Map<ResourceLocation, Long> counts) {
        long result = 0;
        for (long count : counts.values()) {
            result = Planner.add(result, count);
            if (result < 0) return Long.MAX_VALUE;
        }
        return result;
    }

    private record Planned(Map<ResourceLocation, Long> counts, List<DepotSavedData.CraftingStep> steps) {}
    private record PreparedProcessing(Map<ResourceLocation, Long> counts, List<DepotSavedData.SlotEntry> inputs) {}
    private record ExecutionPlan(long totalWork, long peakItems, Map<ResourceLocation, Long> baseInputs,
            Map<ResourceLocation, Long> outputs) {}

    private static final class Planner {
        private final ServerPlayer player;
        private final DepotSavedData depot;
        private final Map<ResourceLocation, Long> initial = new HashMap<>();
        private final Map<ResourceLocation, List<RecipeHolder<CraftingRecipe>>> recipes = new HashMap<>();
        private final Map<ResourceLocation, List<AvailableRecipe>> processingRecipes = new HashMap<>();
        private final Map<ResourceLocation, List<DepotJeiRecipeCache.Recipe>> jeiRecipes = new HashMap<>();
        private final Set<ResourceLocation> missing = new LinkedHashSet<>();
        private final Map<String, Long> missingIngredients = new java.util.LinkedHashMap<>();
        private final List<DepotSavedData.CraftingStep> craftingSteps = new ArrayList<>();
        private final boolean processingAvailable;
        private boolean processingMachineRequired;
        private int steps;

        private Planner(ServerPlayer player, DepotSavedData depot) {
            this.player = player;
            this.depot = depot;
            this.processingAvailable = !DepotNetwork.processingMachines(player).isEmpty();
            depot.entries().forEach(entry -> initial.put(entry.itemId(), entry.count()));
            for (AvailableRecipe candidate : availableRecipes(player)) {
                ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(candidate.output().getItem());
                if (candidate.processing()) {
                    processingRecipes.computeIfAbsent(outputId, ignored -> new ArrayList<>()).add(candidate);
                } else if (candidate.recipe() instanceof CraftingRecipe recipe) {
                    recipes.computeIfAbsent(outputId, ignored -> new ArrayList<>())
                            .add(new RecipeHolder<>(candidate.id(), recipe));
                }
            }
            for (DepotJeiRecipeCache.Recipe recipe : DepotJeiRecipeCache.recipes(player)) {
                jeiRecipes.computeIfAbsent(recipe.primaryOutput().itemId(), ignored -> new ArrayList<>()).add(recipe);
            }
            recipes.values().forEach(list -> list.sort(Comparator.comparing(holder -> holder.id().toString())));
        }

        private Optional<Planned> plan(ResourceLocation targetId, int requested) {
            long goal = add(initialCount(targetId), requested);
            if (goal < 0) return Optional.empty();
            return ensure(new HashMap<>(initial), targetId, goal, new HashSet<>(), 0)
                    .map(counts -> new Planned(counts, List.copyOf(craftingSteps)));
        }

        private Optional<Map<ResourceLocation, Long>> ensure(Map<ResourceLocation, Long> inventory,
                ResourceLocation itemId, long needed, Set<ResourceLocation> visiting, int depth) {
            if (inventory.getOrDefault(itemId, 0L) >= needed) return Optional.of(inventory);
            // ponytail: bounded recursive search; raise these limits only if real recipe packs exceed them.
            if (depth >= MAX_DEPTH || ++steps > MAX_STEPS || !visiting.add(itemId)) return Optional.empty();
            try {
                List<RecipeHolder<CraftingRecipe>> candidates = new ArrayList<>(recipes.getOrDefault(itemId, List.of()));
                ResourceLocation preferred = depot.getPreferredRecipe(itemId);
                candidates.sort(Comparator
                        .comparing((RecipeHolder<CraftingRecipe> holder) -> preferred == null || !holder.id().equals(preferred))
                        .thenComparing(holder -> holder.id().toString()));
                long deficit = needed - inventory.getOrDefault(itemId, 0L);
                ResourceLocation preferredMachine = depot.getPreferredMachine(itemId);
                List<DepotJeiRecipeCache.Recipe> jeiCandidates = new ArrayList<>(
                        jeiRecipes.getOrDefault(itemId, List.of()));
                jeiCandidates.sort(Comparator
                        .comparing((DepotJeiRecipeCache.Recipe candidate) -> preferred == null
                                || !candidate.id().equals(preferred))
                        .thenComparing(candidate -> preferredMachine != null
                                && !candidate.machineTypes().contains(preferredMachine))
                        .thenComparingLong(candidate -> directMissing(candidate, inventory))
                        .thenComparing(candidate -> candidate.id().toString()));
                DepotJeiRecipeCache.Recipe eagerJei = jeiCandidates.stream()
                        .filter(candidate -> preferred != null ? candidate.id().equals(preferred)
                                : directMissing(candidate, inventory) == 0)
                        .findFirst().orElse(null);
                if (eagerJei != null && processingAvailable) {
                    int beforeSteps = craftingSteps.size();
                    Optional<Map<ResourceLocation, Long>> result = tryJeiRecipe(
                            inventory, eagerJei, deficit, visiting, depth);
                    if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                }
                List<AvailableRecipe> machineCandidates = new ArrayList<>(
                        processingRecipes.getOrDefault(itemId, List.of()));
                machineCandidates.sort(Comparator
                        .comparing((AvailableRecipe candidate) -> preferred == null || !candidate.id().equals(preferred))
                        .thenComparingLong(candidate -> directMissing(candidate, inventory))
                        .thenComparing(candidate -> candidate.id().toString()));
                AvailableRecipe eagerMachine = machineCandidates.stream()
                        .filter(candidate -> preferred != null ? candidate.id().equals(preferred)
                                : directMissing(candidate, inventory) == 0)
                        .findFirst().orElse(null);
                if (eagerMachine != null && processingAvailable) {
                    int beforeSteps = craftingSteps.size();
                    Optional<Map<ResourceLocation, Long>> result = tryProcessingRecipe(
                            inventory, eagerMachine, deficit, visiting, depth);
                    if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                }
                // Try machine/processing recipes before crafting to prefer smelting ore
                // over crafting ingots from sub-components when both exist.
                if (processingAvailable) {
                    for (DepotJeiRecipeCache.Recipe candidate : jeiCandidates) {
                        if (eagerJei != null && candidate.id().equals(eagerJei.id())) continue;
                        int beforeSteps = craftingSteps.size();
                        Optional<Map<ResourceLocation, Long>> result = tryJeiRecipe(
                                inventory, candidate, deficit, visiting, depth);
                        if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                        craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    }
                    for (AvailableRecipe candidate : machineCandidates) {
                        if (eagerMachine != null && candidate.id().equals(eagerMachine.id())) continue;
                        int beforeSteps = craftingSteps.size();
                        Optional<Map<ResourceLocation, Long>> result = tryProcessingRecipe(
                                inventory, candidate, deficit, visiting, depth);
                        if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                        craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    }
                }
                for (RecipeHolder<CraftingRecipe> holder : candidates) {
                    int beforeSteps = craftingSteps.size();
                    ItemStack output = holder.value().getResultItem(player.serverLevel().registryAccess());
                    if (output.isEmpty() || output.getCount() <= 0) continue;
                    long craftsLong = (deficit + output.getCount() - 1L) / output.getCount();
                    if (craftsLong <= 0 || craftsLong > Integer.MAX_VALUE) continue;
                    Optional<Map<ResourceLocation, Long>> result = craftRecipe(
                            new HashMap<>(inventory), holder.value(), (int) craftsLong, visiting, depth);
                    if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                }
                DepotSavedData.ProcessingPattern pattern = depot.getProcessingPattern(itemId);
                if (pattern != null) {
                    if (!processingAvailable) {
                        processingMachineRequired = true;
                    } else {
                        int beforeSteps = craftingSteps.size();
                        long craftsLong = (deficit + pattern.outputAmount() - 1L) / pattern.outputAmount();
                        if (craftsLong > 0 && craftsLong <= Integer.MAX_VALUE) {
                            Optional<Map<ResourceLocation, Long>> result = processPattern(
                                    new HashMap<>(inventory), pattern, (int) craftsLong, visiting, depth);
                            if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                        }
                        craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    }
                }
                if ((!jeiCandidates.isEmpty() || !machineCandidates.isEmpty()) && !processingAvailable) {
                    processingMachineRequired = true;
                }
                missing.add(itemId);
                return Optional.empty();
            } finally {
                visiting.remove(itemId);
            }
        }

        private Optional<Map<ResourceLocation, Long>> tryJeiRecipe(Map<ResourceLocation, Long> inventory,
                DepotJeiRecipeCache.Recipe candidate, long deficit, Set<ResourceLocation> visiting, int depth) {
            int output = candidate.primaryOutput().count();
            long crafts = (deficit + output - 1L) / output;
            return crafts <= 0 || crafts > Integer.MAX_VALUE ? Optional.empty()
                    : processJeiRecipe(new HashMap<>(inventory), candidate, (int) crafts, visiting, depth);
        }

        private long directMissing(DepotJeiRecipeCache.Recipe candidate, Map<ResourceLocation, Long> inventory) {
            long missing = 0;
            for (DepotJeiRecipeCache.Slot slot : candidate.inputs()) {
                long least = slot.alternatives().stream().mapToLong(stack -> Math.max(0,
                        (long) stack.count() - inventory.getOrDefault(stack.itemId(), 0L))).min().orElse(Long.MAX_VALUE);
                missing = add(missing, least);
                if (missing < 0) return Long.MAX_VALUE;
            }
            return missing;
        }

        private Optional<Map<ResourceLocation, Long>> tryProcessingRecipe(Map<ResourceLocation, Long> inventory,
                AvailableRecipe candidate, long deficit, Set<ResourceLocation> visiting, int depth) {
            long crafts = (deficit + candidate.output().getCount() - 1L) / candidate.output().getCount();
            return crafts <= 0 || crafts > Integer.MAX_VALUE ? Optional.empty()
                    : processRecipe(new HashMap<>(inventory), candidate, (int) crafts, visiting, depth);
        }

        private long directMissing(AvailableRecipe candidate, Map<ResourceLocation, Long> inventory) {
            long missing = 0;
            NonNullList<Ingredient> ingredients = candidate.recipe().getIngredients();
            for (int index = 0; index < ingredients.size(); index++) {
                Ingredient ingredient = ingredients.get(index);
                if (ingredient.isEmpty()) continue;
                long available = java.util.Arrays.stream(ingredient.getItems())
                        .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                        .mapToLong(id -> inventory.getOrDefault(id, 0L)).max().orElse(0);
                int required = candidate.recipe() instanceof CrystalNexusRecipe custom
                        ? custom.getInputCount(index) : 1;
                missing = add(missing, Math.max(0, required - available));
                if (missing < 0) return Long.MAX_VALUE;
            }
            return missing;
        }

        private Optional<Map<ResourceLocation, Long>> processPattern(Map<ResourceLocation, Long> inventory,
                DepotSavedData.ProcessingPattern pattern, int crafts, Set<ResourceLocation> visiting, int depth) {
            if (craftingSteps.size() > MAX_STEPS - crafts) return Optional.empty();
            Map<ResourceLocation, Long> result = inventory;
            List<DepotSavedData.SlotEntry> inputs = pattern.inputs().entrySet().stream()
                    .map(entry -> new DepotSavedData.SlotEntry(entry.getKey(), entry.getValue())).toList();
            for (int craft = 0; craft < crafts; craft++) {
                for (Map.Entry<ResourceLocation, Long> input : pattern.inputs().entrySet()) {
                    Optional<Map<ResourceLocation, Long>> supplied = ensure(result, input.getKey(),
                            input.getValue(), visiting, depth + 1);
                    if (supplied.isEmpty()) return Optional.empty();
                    result = supplied.get();
                    long left = result.getOrDefault(input.getKey(), 0L) - input.getValue();
                    if (left < 0) return Optional.empty();
                    if (left == 0) result.remove(input.getKey());
                    else result.put(input.getKey(), left);
                }
                for (Map.Entry<ResourceLocation, Long> output : pattern.outputs().entrySet()) {
                    if (!increase(result, output.getKey(), output.getValue())) return Optional.empty();
                }
                craftingSteps.add(new DepotSavedData.CraftingStep(pattern.outputId(), pattern.outputAmount(), 1,
                        inputs, pattern.outputs(), true));
            }
            return Optional.of(result);
        }

        private Optional<Map<ResourceLocation, Long>> processRecipe(Map<ResourceLocation, Long> inventory,
                AvailableRecipe candidate, int crafts, Set<ResourceLocation> visiting, int depth) {
            if (craftingSteps.size() > MAX_STEPS - crafts) return Optional.empty();
            List<Ingredient> ingredients = new ArrayList<>();
            List<Integer> amounts = new ArrayList<>();
            NonNullList<Ingredient> declared = candidate.recipe().getIngredients();
            for (int index = 0; index < declared.size(); index++) {
                Ingredient ingredient = declared.get(index);
                if (ingredient.isEmpty()) continue;
                ingredients.add(ingredient);
                int amount = candidate.recipe() instanceof CrystalNexusRecipe custom
                        ? custom.getInputCount(index)
                        : java.util.Arrays.stream(ingredient.getItems()).mapToInt(ItemStack::getCount).max().orElse(1);
                amounts.add(Math.max(1, amount));
            }
            if (ingredients.isEmpty()) return Optional.empty();
            Optional<PreparedProcessing> prepared = prepareProcessingIngredients(inventory, ingredients, amounts,
                    0, crafts, new ArrayList<>(), visiting, depth);
            if (prepared.isEmpty()) return Optional.empty();

            ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(candidate.output().getItem());
            long outputAmount = candidate.output().getCount();
            long totalOutput = multiply(outputAmount, crafts);
            if (outputId == null || !depot.accepts(outputId) || totalOutput < 0
                    || !increase(prepared.get().counts(), outputId, totalOutput)) return Optional.empty();
            Map<ResourceLocation, Long> outputs = Map.of(outputId, outputAmount);
            for (int craft = 0; craft < crafts; craft++) {
                craftingSteps.add(new DepotSavedData.CraftingStep(outputId, outputAmount, 1,
                        prepared.get().inputs(), outputs, true));
            }
            return Optional.of(prepared.get().counts());
        }

        private Optional<Map<ResourceLocation, Long>> processJeiRecipe(Map<ResourceLocation, Long> inventory,
                DepotJeiRecipeCache.Recipe candidate, int crafts, Set<ResourceLocation> visiting, int depth) {
            if (craftingSteps.size() > MAX_STEPS - crafts) return Optional.empty();
            Optional<PreparedProcessing> prepared = prepareJeiIngredients(inventory, candidate.inputs(), 0, crafts,
                    new ArrayList<>(), visiting, depth);
            if (prepared.isEmpty()) return Optional.empty();
            DepotJeiRecipeCache.StackRef primary = candidate.primaryOutput();
            long totalOutput = multiply(primary.count(), crafts);
            if (!depot.accepts(primary.itemId()) || totalOutput < 0
                    || !increase(prepared.get().counts(), primary.itemId(), totalOutput)) return Optional.empty();
            Map<ResourceLocation, Long> outputs = Map.of(primary.itemId(), (long) primary.count());
            for (int craft = 0; craft < crafts; craft++) {
                craftingSteps.add(new DepotSavedData.CraftingStep(primary.itemId(), primary.count(), 1,
                        prepared.get().inputs(), outputs, true, candidate.machineTypes()));
            }
            return Optional.of(prepared.get().counts());
        }

        private Optional<PreparedProcessing> prepareJeiIngredients(Map<ResourceLocation, Long> inventory,
                List<DepotJeiRecipeCache.Slot> slots, int index, int crafts, List<DepotSavedData.SlotEntry> inputs,
                Set<ResourceLocation> visiting, int depth) {
            if (++steps > MAX_STEPS) return Optional.empty();
            if (index >= slots.size()) return Optional.of(new PreparedProcessing(inventory, List.copyOf(inputs)));
            List<DepotJeiRecipeCache.StackRef> alternatives = slots.get(index).alternatives().stream()
                    .sorted(Comparator.<DepotJeiRecipeCache.StackRef>comparingLong(
                                    stack -> inventory.getOrDefault(stack.itemId(), 0L)).reversed()
                            .thenComparing(stack -> stack.itemId().toString()))
                    .toList();
            for (DepotJeiRecipeCache.StackRef alternative : alternatives) {
                long required = multiply(alternative.count(), crafts);
                if (required < 0) continue;
                int beforeSteps = craftingSteps.size();
                Optional<Map<ResourceLocation, Long>> supplied = ensure(new HashMap<>(inventory),
                        alternative.itemId(), required, visiting, depth + 1);
                if (supplied.isEmpty()) {
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    continue;
                }
                Map<ResourceLocation, Long> consumed = supplied.get();
                long left = consumed.getOrDefault(alternative.itemId(), 0L) - required;
                if (left < 0) {
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    continue;
                }
                if (left == 0) consumed.remove(alternative.itemId());
                else consumed.put(alternative.itemId(), left);
                List<DepotSavedData.SlotEntry> selected = new ArrayList<>(inputs);
                selected.add(new DepotSavedData.SlotEntry(alternative.itemId(), alternative.count()));
                Optional<PreparedProcessing> rest = prepareJeiIngredients(consumed, slots, index + 1, crafts,
                        selected, visiting, depth);
                if (rest.isPresent()) return rest;
                craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
            }
            return Optional.empty();
        }

        private Optional<PreparedProcessing> prepareProcessingIngredients(Map<ResourceLocation, Long> inventory,
                List<Ingredient> ingredients, List<Integer> amounts, int index, int crafts,
                List<DepotSavedData.SlotEntry> inputs, Set<ResourceLocation> visiting, int depth) {
            if (++steps > MAX_STEPS) return Optional.empty();
            if (index >= ingredients.size()) return Optional.of(new PreparedProcessing(inventory, List.copyOf(inputs)));
            Ingredient ingredient = ingredients.get(index);
            long required = multiply(amounts.get(index), crafts);
            if (required < 0) return Optional.empty();
            List<ResourceLocation> candidates = java.util.Arrays.stream(ingredient.getItems())
                    .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                    .filter(id -> id != null && BuiltInRegistries.ITEM.get(id) != Items.AIR)
                    .distinct()
                    .sorted(Comparator.<ResourceLocation>comparingLong(id -> inventory.getOrDefault(id, 0L)).reversed()
                            .thenComparing(ResourceLocation::toString))
                    .toList();
            for (ResourceLocation itemId : candidates) {
                int beforeSteps = craftingSteps.size();
                Optional<Map<ResourceLocation, Long>> supplied = ensure(
                        new HashMap<>(inventory), itemId, required, visiting, depth + 1);
                if (supplied.isEmpty()) {
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    continue;
                }
                Map<ResourceLocation, Long> consumed = supplied.get();
                long left = consumed.getOrDefault(itemId, 0L) - required;
                if (left < 0) {
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    continue;
                }
                if (left == 0) consumed.remove(itemId);
                else consumed.put(itemId, left);
                List<DepotSavedData.SlotEntry> selected = new ArrayList<>(inputs);
                selected.add(new DepotSavedData.SlotEntry(itemId, amounts.get(index)));
                Optional<PreparedProcessing> rest = prepareProcessingIngredients(consumed, ingredients, amounts,
                        index + 1, crafts, selected, visiting, depth);
                if (rest.isPresent()) return rest;
                craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
            }
            rememberMissing(ingredient, required);
            return Optional.empty();
        }

        private Optional<Map<ResourceLocation, Long>> craftRecipe(Map<ResourceLocation, Long> inventory,
                CraftingRecipe recipe, int crafts, Set<ResourceLocation> visiting, int depth) {
            List<Ingredient> ingredients = recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList();
            if (ingredients.isEmpty()) return Optional.empty();
            List<ResourceLocation> choices = new ArrayList<>(ingredients.size());
            Optional<Map<ResourceLocation, Long>> prepared = prepareIngredients(
                    inventory, ingredients, 0, crafts, choices, visiting, depth);
            if (prepared.isEmpty()) return Optional.empty();

            CraftingInput input = craftingInput(recipe, choices);
            ItemStack output = recipe.assemble(input, player.serverLevel().registryAccess());
            ResourceLocation outputId = output.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(output.getItem());
            if (outputId == null || !depot.accepts(outputId)) return Optional.empty();
            Map<ResourceLocation, Long> result = prepared.get();
            long outputAmount = multiply(output.getCount(), crafts);
            if (!increase(result, outputId, outputAmount)) return Optional.empty();
            List<DepotSavedData.SlotEntry> inputs = choices.stream()
                    .map(id -> new DepotSavedData.SlotEntry(id, 1))
                    .toList();
            Map<ResourceLocation, Long> remainders = new HashMap<>();
            for (ItemStack remainder : recipe.getRemainingItems(input)) {
                if (remainder.isEmpty()) continue;
                ResourceLocation remainderId = BuiltInRegistries.ITEM.getKey(remainder.getItem());
                long remainderAmount = multiply(remainder.getCount(), crafts);
                if (!depot.accepts(remainderId) || !increase(result, remainderId, remainderAmount)
                        || !increase(remainders, remainderId, remainder.getCount())) {
                    return Optional.empty();
                }
            }
            long addedSteps = multiply(crafts, output.getCount());
            if (addedSteps > MAX_STEPS - craftingSteps.size()) return Optional.empty();
            for (int i = 0; i < crafts; i++) {
                long previousEnd = 0;
                for (int item = 0; item < output.getCount(); item++) {
                    long end = ((long) (item + 1) * TICKS_PER_CRAFT + output.getCount() - 1) / output.getCount();
                    Map<ResourceLocation, Long> itemOutputs = new HashMap<>();
                    itemOutputs.put(outputId, 1L);
                    if (item == 0) remainders.forEach((id, amount) -> itemOutputs.merge(id, amount, Long::sum));
                    craftingSteps.add(new DepotSavedData.CraftingStep(outputId, 1, end - previousEnd,
                            item == 0 ? inputs : List.of(), itemOutputs));
                    previousEnd = end;
                }
            }
            return Optional.of(result);
        }

        private Optional<Map<ResourceLocation, Long>> prepareIngredients(Map<ResourceLocation, Long> inventory,
                List<Ingredient> ingredients, int index, int crafts, List<ResourceLocation> choices,
                Set<ResourceLocation> visiting, int depth) {
            if (++steps > MAX_STEPS) return Optional.empty();
            if (index >= ingredients.size()) return Optional.of(inventory);
            List<ResourceLocation> candidates = java.util.Arrays.stream(ingredients.get(index).getItems())
                    .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                    .filter(id -> id != null && BuiltInRegistries.ITEM.get(id) != Items.AIR)
                    .distinct()
                    .sorted(Comparator.<ResourceLocation>comparingLong(id -> inventory.getOrDefault(id, 0L)).reversed()
                            .thenComparing(ResourceLocation::toString))
                    .toList();
            for (ResourceLocation candidate : candidates) {
                int beforeSteps = craftingSteps.size();
                Map<ResourceLocation, Long> next = new HashMap<>(inventory);
                Optional<Map<ResourceLocation, Long>> supplied = ensure(next, candidate, crafts, visiting, depth + 1);
                if (supplied.isEmpty()) {
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    continue;
                }
                Map<ResourceLocation, Long> consumed = supplied.get();
                consumed.put(candidate, consumed.get(candidate) - crafts);
                if (consumed.get(candidate) == 0) consumed.remove(candidate);
                choices.add(candidate);
                Optional<Map<ResourceLocation, Long>> rest = prepareIngredients(
                        consumed, ingredients, index + 1, crafts, choices, visiting, depth);
                if (rest.isPresent()) return rest;
                choices.removeLast();
                craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
            }
            rememberMissing(ingredients.get(index), crafts);
            return Optional.empty();
        }

        private boolean fits(Map<ResourceLocation, Long> finalCounts) {
            long used = 0;
            for (Map.Entry<ResourceLocation, Long> entry : finalCounts.entrySet()) {
                long count = entry.getValue();
                if (count < 0 || count > 0 && !depot.accepts(entry.getKey()) || Long.MAX_VALUE - used < count) return false;
                used += count;
            }
            return used <= depot.getCapacity();
        }

        private long initialCount(ResourceLocation id) {
            return initial.getOrDefault(id, 0L);
        }

        private List<String> missingDetails() {
            if (missing.isEmpty() && missingIngredients.isEmpty()) return List.of();
            List<String> lines = new ArrayList<>();
            lines.add("Missing ingredients or crafting paths:");
            if (processingMachineRequired) lines.add("Connect an item-handling machine to the depot cable network.");
            missingIngredients.entrySet().stream().limit(8)
                    .forEach(entry -> lines.add(entry.getValue() + " x " + entry.getKey()));
            if (!missingIngredients.isEmpty()) return lines;
            missing.stream().limit(8).forEach(id -> {
                Item item = BuiltInRegistries.ITEM.get(id);
                lines.add((item == null || item == Items.AIR ? id.toString() : new ItemStack(item).getHoverName().getString()));
            });
            return lines;
        }

        private void rememberMissing(Ingredient ingredient, long amount) {
            List<String> names = java.util.Arrays.stream(ingredient.getItems())
                    .filter(stack -> !stack.isEmpty() && !stack.is(Items.BARRIER))
                    .map(stack -> stack.getHoverName().getString()).distinct().limit(4).toList();
            String label;
            if (!names.isEmpty()) {
                label = String.join(" / ", names)
                        + (ingredient.getItems().length > names.size() ? " / ..." : "");
            } else if (!ingredient.isCustom()) {
                label = java.util.Arrays.stream(ingredient.getValues())
                        .filter(Ingredient.TagValue.class::isInstance).map(Ingredient.TagValue.class::cast)
                        .map(value -> "#" + value.tag().location()).findFirst().orElse("unavailable ingredient");
            } else {
                label = "unavailable ingredient";
            }
            missingIngredients.merge(label, amount, DepotCraftingService.Planner::add);
        }

        private static boolean increase(Map<ResourceLocation, Long> counts, ResourceLocation id, long amount) {
            if (id == null || amount < 0) return false;
            long updated = add(counts.getOrDefault(id, 0L), amount);
            if (updated < 0) return false;
            counts.put(id, updated);
            return true;
        }

        private static long multiply(long left, long right) {
            if (left < 0 || right < 0 || left != 0 && right > Long.MAX_VALUE / left) return -1;
            return left * right;
        }

        private static long add(long left, long right) {
            if (left < 0 || right < 0 || Long.MAX_VALUE - left < right) return -1;
            return left + right;
        }
    }
}
