package net.crystalnexus.cli;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DepotJeiRecipeCache {
    // Large modpacks can contain more than 20,000 JEI machine recipes. Retain the
    // full per-player index so categories scanned later (notably Mekanism) are not
    // silently discarded after earlier recipe categories fill the old cap.
    public static final int MAX_RECIPES = 100_000;
    public static final int MAX_CHUNK = 64;
    public static final int MAX_SLOTS = 32;
    public static final int MAX_ALTERNATIVES = 256;
    public static final int MAX_COUNT = 1_000_000;

    public record StackRef(ResourceLocation itemId, int count) {}
    public record Slot(List<StackRef> alternatives) {
        public Slot { alternatives = List.copyOf(alternatives); }
    }
    public record Recipe(ResourceLocation id, ResourceLocation categoryId, String categoryName,
            List<Slot> inputs, List<StackRef> outputs, List<ResourceLocation> machineTypes) {
        public Recipe {
            categoryName = categoryName == null ? "Machine" : categoryName.substring(0, Math.min(128, categoryName.length()));
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
            machineTypes = List.copyOf(machineTypes);
        }

        public StackRef primaryOutput() { return outputs.getFirst(); }
    }

    private record State(int generation, LinkedHashMap<ResourceLocation, Recipe> recipes) {}
    private static final Map<UUID, State> BY_PLAYER = new ConcurrentHashMap<>();

    private DepotJeiRecipeCache() {}

    public static void accept(ServerPlayer player, int generation, boolean reset, List<Recipe> recipes) {
        if (recipes.size() > MAX_CHUNK || recipes.stream().anyMatch(recipe -> !valid(recipe))) return;
        BY_PLAYER.compute(player.getUUID(), (ignored, old) -> {
            LinkedHashMap<ResourceLocation, Recipe> updated = reset || old == null || old.generation() != generation
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(old.recipes());
            for (Recipe recipe : recipes) {
                if (updated.size() >= MAX_RECIPES && !updated.containsKey(recipe.id())) break;
                updated.put(recipe.id(), recipe);
            }
            return new State(generation, updated);
        });
    }

    public static List<Recipe> recipes(ServerPlayer player) {
        State state = BY_PLAYER.get(player.getUUID());
        return state == null ? List.of() : List.copyOf(state.recipes().values());
    }

    public static List<Recipe> recipesFor(ServerPlayer player, ResourceLocation outputId) {
        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : recipes(player)) {
            if (recipe.primaryOutput().itemId().equals(outputId)) result.add(recipe);
        }
        return result;
    }

    private static boolean valid(Recipe recipe) {
        if (recipe == null || recipe.id() == null || recipe.categoryId() == null
                || recipe.inputs().isEmpty() || recipe.inputs().size() > MAX_SLOTS
                || recipe.outputs().isEmpty() || recipe.outputs().size() > MAX_SLOTS
                || recipe.machineTypes().size() > MAX_ALTERNATIVES) return false;
        return recipe.inputs().stream().allMatch(slot -> !slot.alternatives().isEmpty()
                        && slot.alternatives().size() <= MAX_ALTERNATIVES
                        && slot.alternatives().stream().allMatch(DepotJeiRecipeCache::valid))
                && recipe.outputs().stream().allMatch(DepotJeiRecipeCache::valid);
    }

    private static boolean valid(StackRef stack) {
        return stack != null && stack.itemId() != null && stack.count() > 0 && stack.count() <= MAX_COUNT;
    }
}
