package net.crystalnexus.cli;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.init.CrystalnexusModBlocks;
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
import java.util.WeakHashMap;

public final class DepotCraftingService {
    private static final int MAX_DEPTH = 32;
    private static final int MAX_STEPS = 20_000;
    public static final int MAX_PREVIEW_CHOICES = 16;
    public static final ResourceLocation NO_RECIPE_ROUTE =
            ResourceLocation.fromNamespaceAndPath("crystalnexus", "no_recipe");
    private static final int TICKS_PER_CRAFT = 20;
    private static final Map<RecipeManager, List<AvailableRecipe>> AVAILABLE_RECIPE_CACHE =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static final Map<RecipeManager, PlanningRecipeIndex> PLANNING_RECIPE_INDEX_CACHE =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static final Map<ServerPlayer, CatalogIndex> CATALOG_INDEX_CACHE =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ServerPlayer, CraftabilityCache> CRAFTABILITY_CACHE =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private enum PlanMode { CRAFT, PROCESS, VISUAL }

    public record Result(boolean success, ItemStack output, DepotSavedData.CraftingJob job, List<String> details) {}
    public record IngredientPlan(IntArrayList choices, Map<ResourceLocation, Integer> required) {}
    public record AvailableRecipe(ResourceLocation id, Recipe<?> recipe, ItemStack output, boolean processing) {}
    public record RecipeChoice(ResourceLocation id, ItemStack output, boolean processing, String category,
            List<DepotJeiRecipeCache.Slot> inputs, List<ResourceLocation> machineTypes) {}
    public enum PreviewSource { STORED, CRAFTING, MACHINE, MISSING }
    public record PreviewNode(int id, int parentId, ResourceLocation itemId, long required, long stored,
            PreviewSource source, ResourceLocation selectedRoute, ResourceLocation selectedMachine,
            List<RecipeChoice> alternatives) {}
    public record Preview(boolean success, boolean startable, ResourceLocation targetId, int requested,
            long totalWork, long estimatedTicks, List<PreviewNode> nodes, List<String> details) {}
    public record CatalogEntry(ResourceLocation itemId, long stored, boolean craftable) {}
    public record CatalogPage(List<CatalogEntry> entries, int page, int totalPages) {}
    private record CatalogItem(ResourceLocation id, String searchKey, String sortKey) {}
    private record CatalogIndex(RecipeManager recipeManager, long jeiRevision,
            Set<ResourceLocation> patternOutputs, List<CatalogItem> entries) {}
    private record PlanningRecipeIndex(Map<ResourceLocation, List<RecipeHolder<CraftingRecipe>>> crafting,
            Map<ResourceLocation, List<AvailableRecipe>> processing, List<PotentialRecipe> potentialCrafting) {}
    private record PotentialRecipe(ResourceLocation output, List<Set<ResourceLocation>> inputs) {}
    private record CraftabilityState(RecipeManager recipeManager, long jeiRevision,
            Map<ResourceLocation, Long> counts,
            Map<ResourceLocation, ResourceLocation> preferredRecipes,
            Map<ResourceLocation, ResourceLocation> preferredMachines,
            List<DepotSavedData.ProcessingPattern> patterns, Set<ResourceLocation> connectedMachines) {}
    private record CraftabilityCache(CraftabilityState state, Set<ResourceLocation> candidates,
            Map<ResourceLocation, Boolean> results) {}
    private record SmeltChoice(ResourceLocation outputId, int outputCount, List<ResourceLocation> machineTypes) {}

    private DepotCraftingService() {
    }

    public static Result craft(ServerPlayer player, DepotSavedData depot, Item target, int requested) {
        return create(player, depot, target, requested, PlanMode.CRAFT);
    }

    /** Queues the GUI's craft-first plan; machine routes must have been explicitly preferred. */
    public static Result craftVisual(ServerPlayer player, DepotSavedData depot, Item target, int requested) {
        return create(player, depot, target, requested, PlanMode.VISUAL);
    }

    /** Queues a recipe whose final step must run in an external processing machine. */
    public static Result process(ServerPlayer player, DepotSavedData depot, Item target, int requested) {
        return create(player, depot, target, requested, PlanMode.PROCESS);
    }

    private static Result create(ServerPlayer player, DepotSavedData depot, Item target, int requested, PlanMode mode) {
        int processors = DepotNetwork.craftingProcessorCount(player);
        if (processors <= 0) {
            return new Result(false, ItemStack.EMPTY, null, List.of(
                    "Crafting service unavailable.",
                    "Connect a Crafting Processor to use this command."));
        }
        int capacity = DepotNetwork.craftingJobCapacity(player);
        if (depot.getCraftingJobs().size() >= capacity) {
            return new Result(false, ItemStack.EMPTY, null, List.of(
                    "All " + capacity + " crafting process" + (capacity == 1 ? " is" : "es are") + " busy.",
                    "Add Crafting Cores or use queue cancel <id> to free a process."));
        }
        ResourceLocation targetId = BuiltInRegistries.ITEM.getKey(target);
        if (targetId == null || target == Items.AIR || requested <= 0) {
            return new Result(false, ItemStack.EMPTY, null, List.of("Invalid crafting target."));
        }

        Planner planner = new Planner(player, depot, mode);
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

    /** Queues a direct item-to-item smelting operation without considering crafting recipes. */
    public static Result smelt(ServerPlayer player, DepotSavedData depot, Item input, int requested) {
        int processors = DepotNetwork.craftingProcessorCount(player);
        if (processors <= 0) return new Result(false, ItemStack.EMPTY, null, List.of(
                "Crafting service unavailable.", "Connect a Crafting Processor to use this command."));
        int capacity = DepotNetwork.craftingJobCapacity(player);
        if (depot.getCraftingJobs().size() >= capacity) return new Result(false, ItemStack.EMPTY, null, List.of(
                "All " + capacity + " crafting process" + (capacity == 1 ? " is" : "es are") + " busy.",
                "Add Crafting Cores or use queue cancel <id> to free a process."));
        if (input == null || input == Items.AIR || requested <= 0) {
            return new Result(false, ItemStack.EMPTY, null, List.of("Invalid smelting input."));
        }
        ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input);
        if (inputId == null || depot.getCount(inputId) < requested) {
            return new Result(false, ItemStack.EMPTY, null, List.of("Insufficient stored input items."));
        }
        SmeltChoice choice = smeltingChoices(player, input).stream().findFirst().orElse(null);
        if (choice == null) return new Result(false, ItemStack.EMPTY, null, List.of(
                "No supported furnace or electric-furnace recipe produces an item from " + new ItemStack(input).getHoverName().getString() + "."));
        long outputTotal = (long) choice.outputCount() * requested;
        if (outputTotal <= 0 || outputTotal > Integer.MAX_VALUE || !depot.accepts(choice.outputId())) {
            return new Result(false, ItemStack.EMPTY, null, List.of("The smelting result cannot be stored."));
        }
        long reservation = Math.max(requested, outputTotal);
        List<DepotSavedData.CraftingStep> steps = new ArrayList<>(requested);
        for (int craft = 0; craft < requested; craft++) {
            steps.add(new DepotSavedData.CraftingStep(choice.outputId(), choice.outputCount(), 1,
                List.of(new DepotSavedData.SlotEntry(inputId, 1)),
                Map.of(choice.outputId(), (long) choice.outputCount()), true, choice.machineTypes()));
        }
        DepotSavedData.CraftingJob job = depot.startCraftingJob(choice.outputId(), (int) outputTotal, requested, reservation,
            Map.of(inputId, (long) requested), Map.of(choice.outputId(), outputTotal), steps);
        if (job == null) return new Result(false, ItemStack.EMPTY, null, List.of("Depot storage capacity reached."));
        return new Result(true, new ItemStack(BuiltInRegistries.ITEM.get(choice.outputId()), (int) outputTotal), job, List.of());
    }

    private static List<SmeltChoice> smeltingChoices(ServerPlayer player, Item input) {
        List<SmeltChoice> result = new ArrayList<>();
        RecipeManager manager = player.serverLevel().getRecipeManager();
        for (RecipeHolder<?> holder : recipesForType(manager, RecipeType.SMELTING)) {
            Recipe<?> recipe = holder.value();
            if (recipe.getIngredients().isEmpty() || !recipe.getIngredients().getFirst().test(new ItemStack(input))) continue;
            ItemStack output = recipe.getResultItem(player.serverLevel().registryAccess());
            if (!output.isEmpty()) result.add(new SmeltChoice(BuiltInRegistries.ITEM.getKey(output.getItem()),
                    output.getCount(), smeltingMachines()));
        }
        ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input);
        for (DepotJeiRecipeCache.Recipe recipe : DepotJeiRecipeCache.recipes(player)) {
            if (!isSmeltingMachine(recipe.machineTypes()) || recipe.inputs().isEmpty()
                    || recipe.inputs().getFirst().alternatives().stream().noneMatch(stack -> stack.itemId().equals(inputId))) continue;
            DepotJeiRecipeCache.StackRef output = recipe.primaryOutput();
            result.add(new SmeltChoice(output.itemId(), output.count(), recipe.machineTypes()));
        }
        return result.stream().sorted(Comparator.comparing(choice -> choice.outputId().toString())).toList();
    }

    private static boolean isSmeltingMachine(List<ResourceLocation> machines) {
        return machines.stream().anyMatch(smeltingMachines()::contains);
    }

    private static List<ResourceLocation> smeltingMachines() {
        return List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "furnace"),
                BuiltInRegistries.BLOCK.getKey(CrystalnexusModBlocks.IRON_SMELTER.get()),
                BuiltInRegistries.BLOCK.getKey(CrystalnexusModBlocks.CRYSTAL_SMELTER.get()),
                BuiltInRegistries.BLOCK.getKey(CrystalnexusModBlocks.CHLOROPHYTE_SMELTER.get()),
                BuiltInRegistries.BLOCK.getKey(CrystalnexusModBlocks.INVERTIUM_SMELTER.get()),
                BuiltInRegistries.BLOCK.getKey(CrystalnexusModBlocks.ULTIMA_SMELTER.get()),
                ResourceLocation.parse("mekanism:energized_smelter"),
                ResourceLocation.parse("mekanism:basic_smelting_factory"),
                ResourceLocation.parse("mekanism:advanced_smelting_factory"),
                ResourceLocation.parse("mekanism:elite_smelting_factory"),
                ResourceLocation.parse("mekanism:ultimate_smelting_factory"));
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
        return recipeChoices(player, DepotSavedData.get(player), outputItem);
    }

    public static List<RecipeChoice> recipeChoices(ServerPlayer player, DepotSavedData depot, Item outputItem) {
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
        ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(outputItem);
        DepotSavedData.ProcessingPattern pattern = depot.getProcessingPattern(outputId);
        if (pattern != null) {
            List<DepotJeiRecipeCache.Slot> inputs = pattern.inputs().entrySet().stream()
                    .map(entry -> new DepotJeiRecipeCache.Slot(List.of(
                            new DepotJeiRecipeCache.StackRef(entry.getKey(), Math.toIntExact(Math.min(Integer.MAX_VALUE, entry.getValue()))))))
                    .toList();
            choices.add(new RecipeChoice(programmedRoute(outputId), new ItemStack(outputItem,
                    Math.toIntExact(Math.min(Integer.MAX_VALUE, pattern.outputAmount()))), true,
                    "Programmed", inputs, pattern.machineTypes()));
        }
        return choices.stream().sorted(Comparator.comparing(RecipeChoice::processing)
                .thenComparing(choice -> choice.category().toLowerCase(java.util.Locale.ROOT))
                .thenComparing(choice -> choice.id().toString())).toList();
    }

    public static List<RecipeChoice> visualRecipeChoices(ServerPlayer player, DepotSavedData depot, Item outputItem) {
        Map<ResourceLocation, List<ResourceLocation>> machines = recipesFor(player, outputItem).stream()
                .filter(AvailableRecipe::processing).collect(java.util.stream.Collectors.toMap(
                        AvailableRecipe::id, DepotCraftingService::machineTypes, (left, right) -> left));
        List<RecipeChoice> choices = new ArrayList<>(recipeChoices(player, depot, outputItem).stream().map(choice -> choice.processing()
                && choice.machineTypes().isEmpty() && machines.containsKey(choice.id())
                ? new RecipeChoice(choice.id(), choice.output(), true, choice.category(), choice.inputs(), machines.get(choice.id()))
                : choice).toList());
        choices.add(new RecipeChoice(NO_RECIPE_ROUTE, new ItemStack(outputItem), false,
                "No recipe", List.of(), List.of()));
        return List.copyOf(choices);
    }

    public static ResourceLocation programmedRoute(ResourceLocation outputId) {
        return ResourceLocation.fromNamespaceAndPath("crystalnexus",
                "programmed/" + outputId.getNamespace() + "/" + outputId.getPath());
    }

    public static CatalogPage catalog(ServerPlayer player, DepotSavedData depot, String search, int requestedPage,
            boolean craftableOnly) {
        String query = search == null ? "" : search.trim().toLowerCase(java.util.Locale.ROOT);
        List<CatalogItem> matches = catalogIndex(player, depot).stream()
                .filter(item -> query.isEmpty() || item.searchKey().contains(query)).toList();
        int pageSize = 12;
        if (!craftableOnly) {
            int totalPages = Math.max(1, (matches.size() + pageSize - 1) / pageSize);
            int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
            int from = Math.min(matches.size(), page * pageSize);
            int to = Math.min(matches.size(), from + pageSize);
            List<CatalogEntry> entries = matches.subList(from, to).stream()
                    .map(item -> new CatalogEntry(item.id(), depot.getCount(item.id()), false)).toList();
            return new CatalogPage(entries, page, totalPages);
        }

        CraftabilityCache cache = craftabilityCache(player, depot);
        List<CatalogItem> candidates = matches.stream().filter(item -> cache.candidates().contains(item.id())).toList();
        List<CatalogItem> filtered = new ArrayList<>(candidates.size());
        Planner planner = null;
        for (CatalogItem item : candidates) {
            Boolean canCraft = cache.results().get(item.id());
            if (canCraft == null) {
                if (planner == null) planner = new Planner(player, depot, PlanMode.VISUAL);
                canCraft = planner.plan(item.id(), 1).isPresent();
                cache.results().put(item.id(), canCraft);
            }
            if (canCraft) filtered.add(item);
        }
        int totalPages = Math.max(1, (filtered.size() + pageSize - 1) / pageSize);
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        int from = Math.min(filtered.size(), page * pageSize);
        int to = Math.min(filtered.size(), from + pageSize);
        List<CatalogEntry> entries = filtered.subList(from, to).stream()
                .map(item -> new CatalogEntry(item.id(), depot.getCount(item.id()), true)).toList();
        return new CatalogPage(entries, page, totalPages);
    }

    private static CraftabilityCache craftabilityCache(ServerPlayer player, DepotSavedData depot) {
        Map<ResourceLocation, Long> counts = depot.countSnapshot();
        Map<ResourceLocation, ResourceLocation> preferredRecipes = depot.preferredRecipesSnapshot();
        Map<ResourceLocation, ResourceLocation> preferredMachines = depot.preferredMachinesSnapshot();
        List<DepotSavedData.ProcessingPattern> patterns = depot.getProcessingPatterns();
        Set<ResourceLocation> connectedMachines = DepotNetwork.processingMachines(player).stream()
                .map(endpoint -> BuiltInRegistries.BLOCK.getKey(endpoint.level()
                        .getBlockState(endpoint.pos()).getBlock())).collect(java.util.stream.Collectors.toUnmodifiableSet());
        CraftabilityState state = new CraftabilityState(player.serverLevel().getRecipeManager(),
                DepotJeiRecipeCache.revision(player), counts, preferredRecipes, preferredMachines,
                patterns, connectedMachines);
        CraftabilityCache cached = CRAFTABILITY_CACHE.get(player);
        if (cached != null && cached.state().equals(state)) return cached;
        CraftabilityCache created = new CraftabilityCache(state,
                potentialCraftable(player, preferredRecipes, patterns, counts.keySet(), connectedMachines),
                new HashMap<>());
        CRAFTABILITY_CACHE.put(player, created);
        return created;
    }

    private static Set<ResourceLocation> potentialCraftable(ServerPlayer player,
            Map<ResourceLocation, ResourceLocation> preferredRecipes,
            List<DepotSavedData.ProcessingPattern> patterns, Set<ResourceLocation> stored,
            Set<ResourceLocation> connectedMachines) {
        List<PotentialRecipe> recipes = new ArrayList<>(planningRecipeIndex(player).potentialCrafting());
        PlanningRecipeIndex index = planningRecipeIndex(player);
        preferredRecipes.forEach((output, preferred) -> {
            if (NO_RECIPE_ROUTE.equals(preferred)) return;
            index.processing().getOrDefault(output, List.of()).stream()
                    .filter(candidate -> candidate.id().equals(preferred))
                    .filter(candidate -> compatibleMachine(machineTypes(candidate), connectedMachines))
                    .map(candidate -> potentialRecipe(output, candidate.recipe().getIngredients()))
                    .filter(java.util.Objects::nonNull).findFirst().ifPresent(recipes::add);
            DepotJeiRecipeCache.recipesFor(player, output).stream()
                    .filter(candidate -> candidate.id().equals(preferred))
                    .filter(candidate -> compatibleMachine(candidate.machineTypes(), connectedMachines))
                    .map(candidate -> potentialJeiRecipe(output, candidate.inputs()))
                    .filter(java.util.Objects::nonNull).findFirst().ifPresent(recipes::add);
        });
        patterns.stream().filter(pattern -> programmedRoute(pattern.outputId())
                        .equals(preferredRecipes.get(pattern.outputId())))
                .filter(pattern -> compatibleMachine(pattern.machineTypes(), connectedMachines))
                .map(pattern -> new PotentialRecipe(pattern.outputId(), pattern.inputs().keySet().stream()
                        .map(Set::of).toList())).forEach(recipes::add);

        Set<ResourceLocation> available = new HashSet<>(stored);
        Set<ResourceLocation> produced = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (PotentialRecipe recipe : recipes) {
                if (NO_RECIPE_ROUTE.equals(preferredRecipes.get(recipe.output()))
                        || recipe.inputs().stream().anyMatch(slot -> java.util.Collections.disjoint(slot, available))) continue;
                if (produced.add(recipe.output())) changed = true;
                if (available.add(recipe.output())) changed = true;
            }
        } while (changed);
        return Set.copyOf(produced);
    }

    private static boolean compatibleMachine(List<ResourceLocation> required, Set<ResourceLocation> connected) {
        return !connected.isEmpty() && (required.isEmpty() || required.stream().anyMatch(connected::contains));
    }

    private static PotentialRecipe potentialRecipe(ResourceLocation output, List<Ingredient> ingredients) {
        List<Set<ResourceLocation>> inputs = ingredients.stream().filter(ingredient -> !ingredient.isEmpty())
                .map(ingredient -> java.util.Arrays.stream(ingredient.getItems())
                        .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                        .filter(id -> id != null && BuiltInRegistries.ITEM.get(id) != Items.AIR)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet())).toList();
        return inputs.isEmpty() || inputs.stream().anyMatch(Set::isEmpty) ? null : new PotentialRecipe(output, inputs);
    }

    private static PotentialRecipe potentialJeiRecipe(ResourceLocation output, List<DepotJeiRecipeCache.Slot> slots) {
        List<Set<ResourceLocation>> inputs = slots.stream().map(slot -> slot.alternatives().stream()
                .map(DepotJeiRecipeCache.StackRef::itemId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet())).toList();
        return inputs.isEmpty() || inputs.stream().anyMatch(Set::isEmpty) ? null : new PotentialRecipe(output, inputs);
    }

    private static List<CatalogItem> catalogIndex(ServerPlayer player, DepotSavedData depot) {
        RecipeManager manager = player.serverLevel().getRecipeManager();
        long jeiRevision = DepotJeiRecipeCache.revision(player);
        Set<ResourceLocation> patternOutputs = depot.getProcessingPatterns().stream()
                .map(DepotSavedData.ProcessingPattern::outputId).collect(java.util.stream.Collectors.toUnmodifiableSet());
        CatalogIndex cached = CATALOG_INDEX_CACHE.get(player);
        if (cached != null && cached.recipeManager() == manager && cached.jeiRevision() == jeiRevision
                && cached.patternOutputs().equals(patternOutputs)) return cached.entries();

        Set<ResourceLocation> outputs = new HashSet<>();
        for (AvailableRecipe recipe : availableRecipes(player)) {
            outputs.add(BuiltInRegistries.ITEM.getKey(recipe.output().getItem()));
        }
        outputs.addAll(DepotJeiRecipeCache.outputIds(player));
        outputs.addAll(patternOutputs);
        List<CatalogItem> entries = outputs.stream().filter(id -> id != null).map(id -> {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) return null;
            String name = new ItemStack(item).getHoverName().getString().toLowerCase(java.util.Locale.ROOT);
            return new CatalogItem(id, name + " " + id.toString().toLowerCase(java.util.Locale.ROOT), name);
        }).filter(java.util.Objects::nonNull).sorted(Comparator.comparing(CatalogItem::sortKey)
                .thenComparing(item -> item.id().toString())).toList();
        CATALOG_INDEX_CACHE.put(player, new CatalogIndex(manager, jeiRevision, patternOutputs, entries));
        return entries;
    }

    private static PlanningRecipeIndex planningRecipeIndex(ServerPlayer player) {
        RecipeManager manager = player.serverLevel().getRecipeManager();
        PlanningRecipeIndex cached = PLANNING_RECIPE_INDEX_CACHE.get(manager);
        if (cached != null) return cached;
        Map<ResourceLocation, List<RecipeHolder<CraftingRecipe>>> crafting = new HashMap<>();
        Map<ResourceLocation, List<AvailableRecipe>> processing = new HashMap<>();
        List<PotentialRecipe> potentialCrafting = new ArrayList<>();
        for (AvailableRecipe candidate : availableRecipes(player)) {
            ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(candidate.output().getItem());
            if (candidate.processing()) {
                processing.computeIfAbsent(outputId, ignored -> new ArrayList<>()).add(candidate);
            } else if (candidate.recipe() instanceof CraftingRecipe recipe) {
                crafting.computeIfAbsent(outputId, ignored -> new ArrayList<>())
                        .add(new RecipeHolder<>(candidate.id(), recipe));
                PotentialRecipe potential = potentialRecipe(outputId, recipe.getIngredients());
                if (potential != null) potentialCrafting.add(potential);
            }
        }
        crafting.replaceAll((ignored, recipes) -> recipes.stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString())).toList());
        processing.replaceAll((ignored, recipes) -> List.copyOf(recipes));
        PlanningRecipeIndex index = new PlanningRecipeIndex(Map.copyOf(crafting), Map.copyOf(processing),
                List.copyOf(potentialCrafting));
        PLANNING_RECIPE_INDEX_CACHE.put(manager, index);
        return index;
    }

    public static Preview preview(ServerPlayer player, DepotSavedData depot, Item target, int requested) {
        ResourceLocation targetId = target == null ? null : BuiltInRegistries.ITEM.getKey(target);
        if (targetId == null || target == Items.AIR || requested <= 0) {
            return new Preview(false, false, targetId == null ? ResourceLocation.parse("minecraft:air") : targetId,
                    requested, 0, 0, List.of(), List.of("Invalid crafting target."));
        }
        Planner planner = new Planner(player, depot, PlanMode.VISUAL);
        Optional<Planned> planned = planner.plan(targetId, requested);
        if (planned.isEmpty()) {
            return new Preview(false, false, targetId, requested, 0, 0,
                    failedPreviewNodes(player, depot, targetId, requested), planner.missingDetails());
        }
        Optional<ExecutionPlan> execution = executionPlan(planned.get().steps());
        if (execution.isEmpty()) return new Preview(false, false, targetId, requested, 0, 0, List.of(),
                List.of("The crafting plan produced no usable output."));
        List<PreviewNode> nodes = previewNodes(player, depot, targetId, requested, planned.get().steps(),
                execution.get().baseInputs());
        int processors = DepotNetwork.craftingProcessorCount(player);
        int capacity = DepotNetwork.craftingJobCapacity(player);
        boolean startable = processors > 0 && depot.getCraftingJobs().size() < capacity;
        return new Preview(true, startable, targetId, requested, execution.get().totalWork(),
                estimatedTicks(execution.get().totalWork(), processors), nodes,
                startable ? List.of() : List.of(processors <= 0 ? "Connect a Crafting Processor to start."
                        : "All crafting processes are busy; add a Crafting Core or cancel a job."));
    }

    private static List<RecipeChoice> limitedChoices(ServerPlayer player, DepotSavedData depot, Item item) {
        if (item == null || item == Items.AIR) return List.of();
        Set<ResourceLocation> connected = DepotNetwork.processingMachines(player).stream()
                .map(endpoint -> BuiltInRegistries.BLOCK.getKey(endpoint.level().getBlockState(endpoint.pos()).getBlock()))
                .collect(java.util.stream.Collectors.toSet());
        List<RecipeChoice> choices = visualRecipeChoices(player, depot, item);
        List<RecipeChoice> ranked = choices.stream().sorted(Comparator
                .comparingLong((RecipeChoice choice) -> routeMissing(depot, choice))
                .thenComparing(RecipeChoice::processing)
                .thenComparing(choice -> choice.category().toLowerCase(java.util.Locale.ROOT))
                .thenComparing(choice -> choice.id().toString())).toList();
        Map<ResourceLocation, RecipeChoice> byId = choices.stream().collect(java.util.stream.Collectors.toMap(
                RecipeChoice::id, choice -> choice, (left, right) -> left, java.util.LinkedHashMap::new));
        Set<ResourceLocation> visible = new LinkedHashSet<>();
        ResourceLocation preferred = depot.getPreferredRecipe(BuiltInRegistries.ITEM.getKey(item));
        if (preferred != null && byId.containsKey(preferred)) visible.add(preferred);
        choices.stream().filter(choice -> !choice.processing()).findFirst()
                .ifPresent(choice -> visible.add(choice.id()));
        Set<String> categories = new HashSet<>();
        for (RecipeChoice choice : ranked) {
            if (visible.size() >= MAX_PREVIEW_CHOICES) break;
            if (categories.add(choice.processing() + ":" + choice.category().toLowerCase(java.util.Locale.ROOT)))
                visible.add(choice.id());
        }
        for (RecipeChoice choice : ranked) {
            if (visible.size() >= MAX_PREVIEW_CHOICES) break;
            visible.add(choice.id());
        }
        return visible.stream().map(byId::get).map(choice -> new RecipeChoice(choice.id(), choice.output(),
                choice.processing(), choice.category(), choice.inputs(),
                choice.machineTypes().stream().filter(connected::contains).toList())).toList();
    }

    private static long routeMissing(DepotSavedData depot, RecipeChoice choice) {
        long missing = 0;
        for (DepotJeiRecipeCache.Slot slot : choice.inputs()) {
            long least = slot.alternatives().stream().mapToLong(input -> Math.max(0,
                    (long) input.count() - depot.getCount(input.itemId()))).min().orElse(Long.MAX_VALUE);
            missing = Planner.add(missing, least);
            if (missing < 0) return Long.MAX_VALUE;
        }
        return missing;
    }

    private static List<PreviewNode> failedPreviewNodes(ServerPlayer player, DepotSavedData depot,
            ResourceLocation targetId, int requested) {
        List<PreviewNode> result = new ArrayList<>();
        appendFailedPreviewNode(player, depot, targetId, requested, -1, new HashSet<>(), result);
        return List.copyOf(result);
    }

    private static void appendFailedPreviewNode(ServerPlayer player, DepotSavedData depot, ResourceLocation itemId,
            long required, int parentId, Set<ResourceLocation> path, List<PreviewNode> result) {
        if (result.size() >= 64) return;
        boolean circular = path.contains(itemId);
        long stored = depot.getCount(itemId);
        boolean storedEnough = stored >= required;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        List<RecipeChoice> alternatives = limitedChoices(player, depot, item);
        ResourceLocation selected = depot.getPreferredRecipe(itemId);
        RecipeChoice route = selected == null ? alternatives.stream()
                .filter(choice -> !choice.processing() && !choice.id().equals(NO_RECIPE_ROUTE))
                .findFirst().orElse(null) : alternatives.stream().filter(choice -> choice.id().equals(selected))
                .findFirst().orElse(null);
        PreviewSource source = storedEnough ? PreviewSource.STORED : circular || NO_RECIPE_ROUTE.equals(selected)
                ? PreviewSource.MISSING : route == null
                ? PreviewSource.MISSING
                : route.processing() ? PreviewSource.MACHINE : PreviewSource.CRAFTING;
        int id = result.size();
        result.add(new PreviewNode(id, parentId, itemId, required, stored, source, selected,
                depot.getPreferredMachine(itemId), alternatives));
        if (storedEnough || route == null || circular || !path.add(itemId)) return;
        long crafts = (required + Math.max(1, route.output().getCount()) - 1) / Math.max(1, route.output().getCount());
        Map<ResourceLocation, Long> inputs = new java.util.LinkedHashMap<>();
        for (DepotJeiRecipeCache.Slot slot : route.inputs()) {
            DepotJeiRecipeCache.StackRef input = slot.alternatives().stream().max(Comparator
                    .comparingLong(stack -> depot.getCount(stack.itemId()))).orElse(null);
            if (input == null) continue;
            long childRequired = multiplyBounded(input.count(), crafts);
            inputs.merge(input.itemId(), childRequired, DepotSavedData::saturatedAdd);
        }
        inputs.forEach((inputId, amount) -> appendFailedPreviewNode(player, depot, inputId, amount, id, path, result));
        path.remove(itemId);
    }

    private static long multiplyBounded(long left, long right) {
        return left <= 0 || right <= 0 ? 0 : left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static List<PreviewNode> previewNodes(ServerPlayer player, DepotSavedData depot, ResourceLocation targetId,
            int requested, List<DepotSavedData.CraftingStep> steps, Map<ResourceLocation, Long> baseInputs) {
        Map<ResourceLocation, List<DepotSavedData.CraftingStep>> producers = new HashMap<>();
        for (DepotSavedData.CraftingStep step : steps) producers.computeIfAbsent(step.outputId(), ignored -> new ArrayList<>()).add(step);
        List<PreviewNode> result = new ArrayList<>();
        appendPreviewNode(player, depot, targetId, requested, -1, producers, baseInputs, new HashSet<>(), result);
        return List.copyOf(result);
    }

    private static void appendPreviewNode(ServerPlayer player, DepotSavedData depot, ResourceLocation itemId,
            long required, int parentId, Map<ResourceLocation, List<DepotSavedData.CraftingStep>> producers,
            Map<ResourceLocation, Long> baseInputs, Set<ResourceLocation> path, List<PreviewNode> result) {
        if (result.size() >= 64) return;
        int id = result.size();
        List<DepotSavedData.CraftingStep> itemSteps = producers.getOrDefault(itemId, List.of());
        if (itemSteps.isEmpty()) required = baseInputs.getOrDefault(itemId, required);
        boolean processing = itemSteps.stream().anyMatch(DepotSavedData.CraftingStep::processing);
        PreviewSource source = itemSteps.isEmpty()
                ? depot.getCount(itemId) >= required ? PreviewSource.STORED : PreviewSource.MISSING
                : processing ? PreviewSource.MACHINE : PreviewSource.CRAFTING;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        result.add(new PreviewNode(id, parentId, itemId, required, depot.getCount(itemId), source,
                depot.getPreferredRecipe(itemId), depot.getPreferredMachine(itemId), limitedChoices(player, depot, item)));
        if (itemSteps.isEmpty() || !path.add(itemId)) return;
        Map<ResourceLocation, Long> inputs = new java.util.LinkedHashMap<>();
        itemSteps.forEach(step -> step.inputs().forEach(input -> inputs.merge(input.itemId(), input.count(), Long::sum)));
        inputs.forEach((inputId, amount) -> appendPreviewNode(player, depot, inputId, amount, id, producers,
                baseInputs, path, result));
        path.remove(itemId);
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

    private static boolean vanillaProcessing(AvailableRecipe candidate) {
        return !machineTypes(candidate).isEmpty();
    }

    private static List<ResourceLocation> machineTypes(AvailableRecipe candidate) {
        ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(candidate.recipe().getType());
        if (type == null) return List.of();
        List<ResourceLocation> known = knownMachines(type);
        if (!known.isEmpty()) return known;
        if (!type.getNamespace().equals("minecraft")) return List.of();
        return switch (type.getPath()) {
            case "smelting" -> smeltingMachines();
            case "blasting" -> List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "blast_furnace"));
            case "smoking" -> List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "smoker"));
            case "campfire_cooking" -> List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "campfire"),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "soul_campfire"));
            default -> List.of();
        };
    }

    /** Machine identities that are safe to derive from a server recipe type. */
    private static List<ResourceLocation> knownMachines(ResourceLocation type) {
        return switch (type.toString()) {
            case "ae2:inscriber" -> List.of(ResourceLocation.parse("ae2:inscriber"));
            case "mekanism:enriching" -> List.of(ResourceLocation.parse("mekanism:enrichment_chamber"));
            case "mekanism:smelting" -> List.of(ResourceLocation.parse("mekanism:energized_smelter"));
            case "mekanism:crushing" -> List.of(ResourceLocation.parse("mekanism:crusher"));
            case "mekanism:compressing" -> List.of(ResourceLocation.parse("mekanism:osmium_compressor"));
            case "mekanism:purifying" -> List.of(ResourceLocation.parse("mekanism:purification_chamber"));
            case "mekanism:injecting" -> List.of(ResourceLocation.parse("mekanism:chemical_injection_chamber"));
            case "mekanism:metallurgic_infusing" -> List.of(ResourceLocation.parse("mekanism:metallurgic_infuser"));
            default -> List.of();
        };
    }

    public static List<AvailableRecipe> availableRecipes(ServerPlayer player) {
        RecipeManager manager = player.serverLevel().getRecipeManager();
        List<AvailableRecipe> cached = AVAILABLE_RECIPE_CACHE.get(manager);
        if (cached != null) return cached;
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
                AvailableRecipe candidate = new AvailableRecipe(holder.id(), recipe, output.copy(), processing);
                // A recipe with no deterministic machine target must be imported via
                // the JEI bridge. Never send it to an arbitrary item handler.
                // Check route support before touching ingredients. Some third-party processing
                // recipes expose cached, immutable ingredient stacks (for example when
                // AllTheLeaks ingredient deduplication is enabled). Unsupported recipe types
                // are imported through the JEI bridge, so inspecting their ingredients here is
                // both unnecessary and can throw ATLUnsupportedOperation.
                if (!output.isEmpty() && output.getCount() > 0
                        && (!processing || !machineTypes(candidate).isEmpty())
                        && !recipe.getIngredients().isEmpty()) {
                    result.add(candidate);
                }
            }
        }
        result.sort(Comparator.comparing(candidate -> candidate.id().toString()));
        List<AvailableRecipe> recipes = List.copyOf(result);
        AVAILABLE_RECIPE_CACHE.put(manager, recipes);
        return recipes;
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
        return craftingInput(recipe, choices, List.of());
    }

    private static CraftingInput craftingInput(CraftingRecipe recipe, List<ResourceLocation> choices,
            List<ItemStack> reusableItems) {
        NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        int choice = 0;
        for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
            if (ingredients.get(ingredient).isEmpty()) continue;
            int slot = recipe instanceof ShapedRecipe ? ingredient % width + ingredient / width * 3 : choice;
            Item item = BuiltInRegistries.ITEM.get(choices.get(choice++));
            ItemStack reusable = choice <= reusableItems.size() ? reusableItems.get(choice - 1) : ItemStack.EMPTY;
            grid.set(slot, !reusable.isEmpty() && reusable.is(item) ? reusable.copy() : new ItemStack(item));
        }
        return CraftingInput.of(3, 3, grid);
    }

    private static List<ItemStack> reusableItems(CraftingRecipe recipe, List<ResourceLocation> choices,
            NonNullList<ItemStack> remaining) {
        List<ItemStack> result = new ArrayList<>(choices.size());
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        int choice = 0;
        for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
            if (ingredients.get(ingredient).isEmpty()) continue;
            int slot = recipe instanceof ShapedRecipe ? ingredient % width + ingredient / width * 3 : choice;
            Item item = BuiltInRegistries.ITEM.get(choices.get(choice++));
            ItemStack remainder = slot < remaining.size() ? remaining.get(slot) : ItemStack.EMPTY;
            result.add(!remainder.isEmpty() && remainder.is(item) ? remainder.copy() : ItemStack.EMPTY);
        }
        return List.copyOf(result);
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
    private record FinishedCraft(Map<ResourceLocation, Long> counts, List<ItemStack> reusableItems) {}
    private record ExecutionPlan(long totalWork, long peakItems, Map<ResourceLocation, Long> baseInputs,
            Map<ResourceLocation, Long> outputs) {}

    private static final class Planner {
        private final ServerPlayer player;
        private final DepotSavedData depot;
        private final PlanMode mode;
        private final Map<ResourceLocation, Long> initial = new HashMap<>();
        private final Map<ResourceLocation, List<RecipeHolder<CraftingRecipe>>> recipes;
        private final Map<ResourceLocation, List<AvailableRecipe>> processingRecipes;
        private final Set<ResourceLocation> connectedMachineTypes = new HashSet<>();
        private final Set<ResourceLocation> missing = new LinkedHashSet<>();
        private final Set<ResourceLocation> circular = new LinkedHashSet<>();
        private final Map<String, Long> missingIngredients = new java.util.LinkedHashMap<>();
        private final List<DepotSavedData.CraftingStep> craftingSteps = new ArrayList<>();
        private final boolean processingAvailable;
        private boolean processingMachineRequired;
        private boolean processingRouteRequired;
        private int steps;

        private Planner(ServerPlayer player, DepotSavedData depot, PlanMode mode) {
            this.player = player;
            this.depot = depot;
            this.mode = mode;
            List<DepotNetwork.MachineEndpoint> machines = DepotNetwork.processingMachines(player);
            this.processingAvailable = !machines.isEmpty();
            machines.stream().map(endpoint -> BuiltInRegistries.BLOCK.getKey(endpoint.level()
                    .getBlockState(endpoint.pos()).getBlock())).forEach(connectedMachineTypes::add);
            initial.putAll(depot.countSnapshot());
            PlanningRecipeIndex recipeIndex = planningRecipeIndex(player);
            recipes = recipeIndex.crafting();
            processingRecipes = recipeIndex.processing();
        }

        private Optional<Planned> plan(ResourceLocation targetId, int requested) {
            missing.clear();
            circular.clear();
            missingIngredients.clear();
            craftingSteps.clear();
            processingMachineRequired = false;
            processingRouteRequired = false;
            steps = 0;
            long goal = add(initialCount(targetId), requested);
            if (goal < 0) return Optional.empty();
            return ensure(new HashMap<>(initial), targetId, goal, new HashSet<>(), 0)
                    .map(counts -> new Planned(counts, List.copyOf(craftingSteps)));
        }

        private Optional<Map<ResourceLocation, Long>> ensure(Map<ResourceLocation, Long> inventory,
                ResourceLocation itemId, long needed, Set<ResourceLocation> visiting, int depth) {
            if (inventory.getOrDefault(itemId, 0L) >= needed) return Optional.of(inventory);
            // ponytail: bounded recursive search; raise these limits only if real recipe packs exceed them.
            if (depth >= MAX_DEPTH || ++steps > MAX_STEPS) {
                missing.add(itemId);
                return Optional.empty();
            }
            if (!visiting.add(itemId)) {
                circular.add(itemId);
                missing.add(itemId);
                return Optional.empty();
            }
            try {
                List<RecipeHolder<CraftingRecipe>> candidates = new ArrayList<>(recipes.getOrDefault(itemId, List.of()));
                // `process` requires the requested final item to come from an
                // external machine. Recursive dependencies may still use a normal
                // crafting recipe when that is their only available path.
                if (mode == PlanMode.PROCESS && depth == 0) candidates.clear();
                ResourceLocation preferred = depot.getPreferredRecipe(itemId);
                if (NO_RECIPE_ROUTE.equals(preferred)) {
                    missing.add(itemId);
                    return Optional.empty();
                }
                DepotSavedData.ProcessingPattern programmed = depot.getProcessingPattern(itemId);
                if (programmed != null && programmedRoute(itemId).equals(preferred)) {
                    if (!processingAvailable || !compatibleMachine(programmed.machineTypes())) {
                        processingMachineRequired = true;
                        if (mode == PlanMode.VISUAL) {
                            missing.add(itemId);
                            return Optional.empty();
                        }
                    } else {
                        int beforeSteps = craftingSteps.size();
                        long craftsLong = (needed - inventory.getOrDefault(itemId, 0L)
                                + programmed.outputAmount() - 1L) / programmed.outputAmount();
                        if (craftsLong > 0 && craftsLong <= Integer.MAX_VALUE) {
                            Optional<Map<ResourceLocation, Long>> result = processPattern(new HashMap<>(inventory),
                                    programmed, (int) craftsLong, visiting, depth);
                            if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                        }
                        craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    }
                }
                candidates.sort(Comparator
                        .comparing((RecipeHolder<CraftingRecipe> holder) -> preferred == null || !holder.id().equals(preferred))
                    // Without an explicit user preference, consume the fewest
                    // missing inputs first, then favor compact, high-yield recipes.
                    .thenComparingLong(holder -> directMissing(holder.value(), inventory))
                    .thenComparingLong(holder -> recipeCost(holder.value()))
                        .thenComparing(holder -> holder.id().toString()));
                long deficit = needed - inventory.getOrDefault(itemId, 0L);
                ResourceLocation preferredMachine = depot.getPreferredMachine(itemId);
                List<DepotJeiRecipeCache.Recipe> jeiCandidates = new ArrayList<>(
                        DepotJeiRecipeCache.recipesFor(player, itemId));
                jeiCandidates.sort(Comparator
                        .comparing((DepotJeiRecipeCache.Recipe candidate) -> preferred == null
                                || !candidate.id().equals(preferred))
                        .thenComparing(candidate -> preferredMachine != null
                                && !candidate.machineTypes().contains(preferredMachine))
                        .thenComparing(candidate -> !compatibleMachine(candidate.machineTypes()))
                        .thenComparingLong(candidate -> directMissing(candidate, inventory))
                        .thenComparingLong(candidate -> recipeCost(candidate))
                        .thenComparing(candidate -> candidate.id().toString()));
                DepotJeiRecipeCache.Recipe eagerJei = preferred == null ? null : jeiCandidates.stream()
                        .filter(candidate -> candidate.id().equals(preferred))
                        .findFirst().orElse(null);
                if (mode == PlanMode.VISUAL && eagerJei != null
                        && (!processingAvailable || !compatibleMachine(eagerJei.machineTypes()))) {
                    processingMachineRequired = true;
                    missing.add(itemId);
                    return Optional.empty();
                }
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
                        .thenComparing(candidate -> !compatibleMachine(machineTypes(candidate)))
                        .thenComparingLong(candidate -> directMissing(candidate, inventory))
                        .thenComparingLong(candidate -> recipeCost(candidate))
                        .thenComparing(candidate -> candidate.id().toString()));
                AvailableRecipe eagerMachine = preferred == null ? null : machineCandidates.stream()
                        .filter(candidate -> candidate.id().equals(preferred))
                        .findFirst().orElse(null);
                if (mode == PlanMode.VISUAL && eagerMachine != null
                        && (!processingAvailable || !compatibleMachine(machineTypes(eagerMachine)))) {
                    processingMachineRequired = true;
                    missing.add(itemId);
                    return Optional.empty();
                }
                if (eagerMachine != null && processingAvailable) {
                    int beforeSteps = craftingSteps.size();
                    Optional<Map<ResourceLocation, Long>> result = tryProcessingRecipe(
                            inventory, eagerMachine, deficit, visiting, depth);
                    if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                }
                for (RecipeHolder<CraftingRecipe> holder : candidates) {
                    int beforeSteps = craftingSteps.size();
                    ItemStack output = holder.value().getResultItem(player.serverLevel().registryAccess());
                    if (output.isEmpty() || output.getCount() <= 0) continue;
                    long craftsLong = (deficit + output.getCount() - 1L) / output.getCount();
                    if (craftsLong <= 0 || craftsLong > Integer.MAX_VALUE) continue;
                    Optional<Map<ResourceLocation, Long>> result = craftRecipeDirect(
                            new HashMap<>(inventory), holder.value(), (int) craftsLong, visiting, depth);
                    if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                }
                // Standard `craft` must be able to recursively make ordinary
                // crafting-table dependencies. The direct attempt above is kept
                // first because it is cheaper when all inputs are already stored.
                if (mode != PlanMode.PROCESS) {
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
                }
                if (mode != PlanMode.VISUAL && processingAvailable) {
                    for (AvailableRecipe candidate : machineCandidates.stream().filter(DepotCraftingService::vanillaProcessing).toList()) {
                        if (eagerMachine != null && candidate.id().equals(eagerMachine.id())) continue;
                        int beforeSteps = craftingSteps.size();
                        Optional<Map<ResourceLocation, Long>> result = tryProcessingRecipe(
                                inventory, candidate, deficit, visiting, depth);
                        if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                        craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
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
                    for (DepotJeiRecipeCache.Recipe candidate : jeiCandidates) {
                        if (eagerJei != null && candidate.id().equals(eagerJei.id())) continue;
                        int beforeSteps = craftingSteps.size();
                        Optional<Map<ResourceLocation, Long>> result = tryJeiRecipe(
                                inventory, candidate, deficit, visiting, depth);
                        if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                        craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    }
                    for (AvailableRecipe candidate : machineCandidates.stream().filter(candidate -> !vanillaProcessing(candidate)).toList()) {
                        if (eagerMachine != null && candidate.id().equals(eagerMachine.id())) continue;
                        int beforeSteps = craftingSteps.size();
                        Optional<Map<ResourceLocation, Long>> result = tryProcessingRecipe(
                                inventory, candidate, deficit, visiting, depth);
                        if (result.isPresent() && result.get().getOrDefault(itemId, 0L) >= needed) return result;
                        craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    }
                }
                DepotSavedData.ProcessingPattern pattern = mode != PlanMode.VISUAL
                    ? depot.getProcessingPattern(itemId) : null;
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
                if (mode == PlanMode.VISUAL && preferred == null && missingIngredients.isEmpty()
                        && (!jeiCandidates.isEmpty() || !machineCandidates.isEmpty() || programmed != null)) {
                    processingRouteRequired = true;
                }
                missing.add(itemId);
                return Optional.empty();
            } finally {
                visiting.remove(itemId);
            }
        }

        private boolean compatibleMachine(List<ResourceLocation> machineTypes) {
            return machineTypes.isEmpty() || machineTypes.stream().anyMatch(connectedMachineTypes::contains);
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

        private long directMissing(CraftingRecipe candidate, Map<ResourceLocation, Long> inventory) {
            long missing = 0;
            for (Ingredient ingredient : candidate.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                long available = java.util.Arrays.stream(ingredient.getItems())
                        .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                        .mapToLong(id -> inventory.getOrDefault(id, 0L)).max().orElse(0);
                missing = add(missing, Math.max(0, 1 - available));
                if (missing < 0) return Long.MAX_VALUE;
            }
            return missing;
        }

        private long recipeCost(CraftingRecipe recipe) {
            long ingredients = recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).count();
            ItemStack output = recipe.getResultItem(player.serverLevel().registryAccess());
            return output.isEmpty() || output.getCount() <= 0 ? Long.MAX_VALUE
                    : (ingredients * 1_000L) / output.getCount();
        }

                private static long recipeCost(DepotJeiRecipeCache.Recipe recipe) {
                    long ingredients = recipe.inputs().stream().mapToLong(slot -> slot.alternatives().stream()
                        .mapToLong(DepotJeiRecipeCache.StackRef::count).min().orElse(Long.MAX_VALUE)).sum();
                    int output = recipe.primaryOutput().count();
                    return output <= 0 || ingredients < 0 ? Long.MAX_VALUE : (ingredients * 1_000L) / output;
                }

                private long recipeCost(AvailableRecipe recipe) {
                    long ingredients = 0;
                    NonNullList<Ingredient> declared = recipe.recipe().getIngredients();
                    for (int index = 0; index < declared.size(); index++) {
                    if (declared.get(index).isEmpty()) continue;
                    ingredients = add(ingredients, recipe.recipe() instanceof CrystalNexusRecipe custom
                        ? custom.getInputCount(index) : 1);
                    }
                    return ingredients < 0 || recipe.output().getCount() <= 0 ? Long.MAX_VALUE
                        : (ingredients * 1_000L) / recipe.output().getCount();
                }

        private Optional<Map<ResourceLocation, Long>> processPattern(Map<ResourceLocation, Long> inventory,
                DepotSavedData.ProcessingPattern pattern, int crafts, Set<ResourceLocation> visiting, int depth) {
            if (craftingSteps.size() > MAX_STEPS - crafts) return Optional.empty();
            Map<ResourceLocation, Long> result = inventory;
            List<DepotSavedData.SlotEntry> inputs = pattern.inputs().entrySet().stream()
                    .map(entry -> new DepotSavedData.SlotEntry(entry.getKey(), entry.getValue())).toList();
            List<ResourceLocation> machines = pattern.machineTypes().isEmpty() ? List.of()
                    : pattern.machineTypes().stream().filter(connectedMachineTypes::contains).toList();
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
                    inputs, pattern.outputs(), true, machines));
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
            List<ResourceLocation> machines = machineTypes(candidate).stream()
                    .filter(connectedMachineTypes::contains).toList();
            for (int craft = 0; craft < crafts; craft++) {
                craftingSteps.add(new DepotSavedData.CraftingStep(outputId, outputAmount, 1,
                        prepared.get().inputs(), outputs, true, machines));
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
            List<ResourceLocation> machines = candidate.machineTypes().stream()
                    .filter(connectedMachineTypes::contains).toList();
            for (int craft = 0; craft < crafts; craft++) {
                craftingSteps.add(new DepotSavedData.CraftingStep(primary.itemId(), primary.count(), 1,
                        prepared.get().inputs(), outputs, true, machines));
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
            Set<ResourceLocation> missingBeforeIngredient = new LinkedHashSet<>(missing);
            Map<String, Long> missingIngredientsBeforeIngredient = new java.util.LinkedHashMap<>(missingIngredients);
            Set<ResourceLocation> deeperMissing = null;
            Map<String, Long> deeperMissingIngredients = null;
            for (ResourceLocation itemId : candidates) {
                int beforeSteps = craftingSteps.size();
                Optional<Map<ResourceLocation, Long>> supplied = ensure(
                        new HashMap<>(inventory), itemId, required, visiting, depth + 1);
                if (supplied.isEmpty()) {
                    if (deeperMissing == null) {
                        deeperMissing = new LinkedHashSet<>(missing);
                        deeperMissingIngredients = new java.util.LinkedHashMap<>(missingIngredients);
                    }
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    restoreMissing(missingBeforeIngredient, missingIngredientsBeforeIngredient);
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
                if (deeperMissing == null) {
                    deeperMissing = new LinkedHashSet<>(missing);
                    deeperMissingIngredients = new java.util.LinkedHashMap<>(missingIngredients);
                }
                craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                restoreMissing(missingBeforeIngredient, missingIngredientsBeforeIngredient);
            }
            if (deeperMissing != null) restoreMissing(deeperMissing, deeperMissingIngredients);
            else rememberMissing(ingredient, required);
            return Optional.empty();
        }

        private Optional<Map<ResourceLocation, Long>> craftRecipeDirect(Map<ResourceLocation, Long> inventory,
                CraftingRecipe recipe, int crafts, Set<ResourceLocation> visiting, int depth) {
            List<Ingredient> ingredients = recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList();
            if (ingredients.isEmpty()) return Optional.empty();
            Map<ResourceLocation, Long> result = inventory;
            List<ItemStack> reusableItems = List.of();
            for (int craft = 0; craft < crafts; craft++) {
                List<ResourceLocation> choices = new ArrayList<>(ingredients.size());
                Optional<Map<ResourceLocation, Long>> prepared = prepareDirectIngredients(
                        result, ingredients, 0, 1, choices);
                if (prepared.isEmpty()) return Optional.empty();
                Optional<FinishedCraft> finished = finishCraftRecipe(prepared.get(), recipe, choices, reusableItems);
                if (finished.isEmpty()) return Optional.empty();
                result = finished.get().counts();
                reusableItems = finished.get().reusableItems();
            }
            return Optional.of(result);
        }

        private Optional<Map<ResourceLocation, Long>> craftRecipe(Map<ResourceLocation, Long> inventory,
                CraftingRecipe recipe, int crafts, Set<ResourceLocation> visiting, int depth) {
            List<Ingredient> ingredients = recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList();
            if (ingredients.isEmpty()) return Optional.empty();
            Map<ResourceLocation, Long> result = inventory;
            List<ItemStack> reusableItems = List.of();
            for (int craft = 0; craft < crafts; craft++) {
                List<ResourceLocation> choices = new ArrayList<>(ingredients.size());
                Optional<Map<ResourceLocation, Long>> prepared = prepareIngredients(
                        result, ingredients, 0, 1, choices, visiting, depth);
                if (prepared.isEmpty()) return Optional.empty();
                Optional<FinishedCraft> finished = finishCraftRecipe(prepared.get(), recipe, choices, reusableItems);
                if (finished.isEmpty()) return Optional.empty();
                result = finished.get().counts();
                reusableItems = finished.get().reusableItems();
            }
            return Optional.of(result);
        }

        private Optional<FinishedCraft> finishCraftRecipe(Map<ResourceLocation, Long> inventory,
                CraftingRecipe recipe, List<ResourceLocation> choices, List<ItemStack> reusableItems) {
            CraftingInput input = craftingInput(recipe, choices, reusableItems);
            ItemStack output = recipe.assemble(input, player.serverLevel().registryAccess());
            ResourceLocation outputId = output.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(output.getItem());
            if (outputId == null || !depot.accepts(outputId)) return Optional.empty();
            Map<ResourceLocation, Long> result = inventory;
            if (!increase(result, outputId, output.getCount())) return Optional.empty();
            List<DepotSavedData.SlotEntry> inputs = choices.stream()
                    .map(id -> new DepotSavedData.SlotEntry(id, 1))
                    .toList();
            Map<ResourceLocation, Long> remainders = new HashMap<>();
            NonNullList<ItemStack> remaining = recipe.getRemainingItems(input);
            for (ItemStack remainder : remaining) {
                if (remainder.isEmpty()) continue;
                ResourceLocation remainderId = BuiltInRegistries.ITEM.getKey(remainder.getItem());
                if (!depot.accepts(remainderId) || !increase(result, remainderId, remainder.getCount())
                        || !increase(remainders, remainderId, remainder.getCount())) {
                    return Optional.empty();
                }
            }
            if (output.getCount() > MAX_STEPS - craftingSteps.size()) return Optional.empty();
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
            return Optional.of(new FinishedCraft(result, reusableItems(recipe, choices, remaining)));
        }

        private Optional<Map<ResourceLocation, Long>> prepareDirectIngredients(Map<ResourceLocation, Long> inventory,
                List<Ingredient> ingredients, int index, int crafts, List<ResourceLocation> choices) {
            if (index >= ingredients.size()) return Optional.of(inventory);
            List<ResourceLocation> candidates = java.util.Arrays.stream(ingredients.get(index).getItems())
                    .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                    .filter(id -> id != null && BuiltInRegistries.ITEM.get(id) != Items.AIR)
                    .distinct()
                    .sorted(Comparator.<ResourceLocation>comparingLong(id -> inventory.getOrDefault(id, 0L)).reversed()
                            .thenComparing(ResourceLocation::toString))
                    .toList();
            for (ResourceLocation candidate : candidates) {
                long available = inventory.getOrDefault(candidate, 0L);
                if (available < crafts) continue;
                Map<ResourceLocation, Long> next = new HashMap<>(inventory);
                if (available == crafts) next.remove(candidate);
                else next.put(candidate, available - crafts);
                choices.add(candidate);
                Optional<Map<ResourceLocation, Long>> rest = prepareDirectIngredients(next, ingredients, index + 1,
                        crafts, choices);
                if (rest.isPresent()) return rest;
                choices.removeLast();
            }
            return Optional.empty();
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
            Set<ResourceLocation> deeperMissing = null;
            Map<String, Long> deeperMissingIngredients = null;
            for (ResourceLocation candidate : candidates) {
                int beforeSteps = craftingSteps.size();
                Set<ResourceLocation> missingBefore = new LinkedHashSet<>(missing);
                Map<String, Long> missingIngredientsBefore = new java.util.LinkedHashMap<>(missingIngredients);
                Map<ResourceLocation, Long> next = new HashMap<>(inventory);
                Optional<Map<ResourceLocation, Long>> supplied = ensure(next, candidate, crafts, visiting, depth + 1);
                if (supplied.isEmpty()) {
                    if (deeperMissing == null) {
                        deeperMissing = new LinkedHashSet<>(missing);
                        deeperMissingIngredients = new java.util.LinkedHashMap<>(missingIngredients);
                    }
                    craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                    restoreMissing(missingBefore, missingIngredientsBefore);
                    continue;
                }
                Map<ResourceLocation, Long> consumed = supplied.get();
                consumed.put(candidate, consumed.get(candidate) - crafts);
                if (consumed.get(candidate) == 0) consumed.remove(candidate);
                choices.add(candidate);
                Optional<Map<ResourceLocation, Long>> rest = prepareIngredients(
                        consumed, ingredients, index + 1, crafts, choices, visiting, depth);
                if (rest.isPresent()) return rest;
                if (deeperMissing == null) {
                    deeperMissing = new LinkedHashSet<>(missing);
                    deeperMissingIngredients = new java.util.LinkedHashMap<>(missingIngredients);
                }
                choices.removeLast();
                craftingSteps.subList(beforeSteps, craftingSteps.size()).clear();
                restoreMissing(missingBefore, missingIngredientsBefore);
            }
            if (deeperMissing != null) {
                restoreMissing(deeperMissing, deeperMissingIngredients);
                return Optional.empty();
            }
            rememberMissing(ingredients.get(index), crafts);
            return Optional.empty();
        }

        private void restoreMissing(Set<ResourceLocation> missingBefore, Map<String, Long> ingredientsBefore) {
            missing.clear();
            missing.addAll(missingBefore);
            missingIngredients.clear();
            missingIngredients.putAll(ingredientsBefore);
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
            if (missingIngredients.isEmpty() && missing.stream().allMatch(circular::contains)) {
                circular.stream().limit(2).forEach(id -> lines.add("Circular recipe path: " + itemName(id)));
            }
            if (processingRouteRequired) lines.add("Select a machine route for the blocked item.");
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

        private static String itemName(ResourceLocation id) {
            Item item = BuiltInRegistries.ITEM.get(id);
            return item == null || item == Items.AIR ? id.toString() : new ItemStack(item).getHoverName().getString();
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
