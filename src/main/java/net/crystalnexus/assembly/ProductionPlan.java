package net.crystalnexus.assembly;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import java.util.*;

/** A bounded dependency graph: one node is one physical machine operation. */
public final class ProductionPlan {
    public static final int MAX_OPERATIONS = 4096;
    public final ItemStack requested;
    public final List<Task> tasks = new ArrayList<>();
    public final List<ItemStack> rawInputs = new ArrayList<>();
    public String error = "";

    public static final class Task {
        public final ResourceLocation recipe;
        public final List<ItemStack> inputs;
        public final ItemStack output;
        public final Set<Integer> dependencies;
        public Long machine;
        public boolean complete;
        public Task(ResourceLocation recipe, List<ItemStack> inputs, ItemStack output, Set<Integer> dependencies) {
            this.recipe = recipe; this.inputs = inputs; this.output = output; this.dependencies = dependencies;
        }
    }
    public ProductionPlan(ItemStack requested) { this.requested = requested.copy(); }
    public boolean complete() { return tasks.stream().allMatch(t -> t.complete); }
    public int completed() { return (int) tasks.stream().filter(t -> t.complete).count(); }

    private record Supply(ItemStack stack, int producer) {}
    private static final class State {
        final List<Supply> stock = new ArrayList<>();
        final List<ItemStack> raw = new ArrayList<>();
        final List<Task> tasks = new ArrayList<>();
        State copy() {
            State s = new State();
            for (Supply supply : stock) s.stock.add(new Supply(supply.stack.copy(), supply.producer));
            raw.forEach(i -> s.raw.add(i.copy())); s.tasks.addAll(tasks); return s;
        }
        void replace(State s) { stock.clear(); stock.addAll(s.stock); raw.clear(); raw.addAll(s.raw); tasks.clear(); tasks.addAll(s.tasks); }
    }
    private static final class Missing extends RuntimeException { Missing(String message) { super(message); } }

    public static ProductionPlan build(Level level, ItemStack target, List<ItemStack> available, List<AssemblyLineMachine> workers) {
        ProductionPlan plan = new ProductionPlan(target);
        State state = new State();
        available.stream().filter(i -> !i.isEmpty()).forEach(i -> state.stock.add(new Supply(i.copy(), -1)));
        // Registry lookup is per user request, never per tick. No JEI runtime dependency.
        List<RecipeHolder<?>> recipes = level.getRecipeManager().getRecipes().stream()
            .filter(r -> AssemblyLineMachine.kind(r) != null).sorted(Comparator.comparing(r -> r.id().toString())).toList();
        int[] budget = {16384};
        try {
            if (target.isEmpty() || target.getCount() < 1 || target.getCount() > MAX_OPERATIONS) throw new Missing("Quantity must be 1..4096");
            supply(level, Ingredient.of(target.copyWithCount(1)), target.getCount(), state, recipes, workers, new HashSet<>(), 0, budget);
            plan.tasks.addAll(state.tasks); plan.rawInputs.addAll(state.raw);
        } catch (Missing missing) { plan.error = missing.getMessage(); }
        return plan;
    }

    private record Taken(List<ItemStack> stacks, Set<Integer> dependencies) {}
    private static Taken supply(Level level, Ingredient ingredient, int amount, State state, List<RecipeHolder<?>> recipes,
                                List<AssemblyLineMachine> workers, Set<ResourceLocation> path, int depth, int[] budget) {
        if (depth > 32 || --budget[0] < 0) throw new Missing("Production graph exceeds planning limit");
        List<ItemStack> taken = new ArrayList<>(); Set<Integer> deps = new HashSet<>();
        for (Supply supply : state.stock) {
            if (amount == 0) break;
            if (!supply.stack.isEmpty() && ingredient.test(supply.stack)) {
                int count = Math.min(amount, supply.stack.getCount());
                ItemStack part = supply.stack.copyWithCount(count); taken.add(part); supply.stack.shrink(count); amount -= count;
                if (supply.producer < 0) state.raw.add(part.copy()); else deps.add(supply.producer);
            }
        }
        if (amount == 0) return new Taken(taken, deps);
        final State current = state;
        List<RecipeHolder<?>> candidates = recipes.stream().filter(r -> ingredient.test(r.value().getResultItem(level.registryAccess())))
            .sorted(Comparator.<RecipeHolder<?>>comparingInt(r -> workers.stream().anyMatch(w -> w.supports(r)) ? 0 : 1)
                .thenComparingInt(r -> -(int) r.value().getIngredients().stream().filter(i -> current.stock.stream().anyMatch(s -> !s.stack.isEmpty() && i.test(s.stack))).count()))
            .toList();
        String failure = "Missing Materials: " + Arrays.stream(ingredient.getItems()).findFirst()
            .map(i -> i.getHoverName().getString()).orElse("unknown ingredient") + " x" + amount;
        for (RecipeHolder<?> recipe : candidates) {
            if (path.contains(recipe.id())) continue;
            if (workers.stream().noneMatch(w -> w.supports(recipe))) {
                failure = "Missing Machine: " + AssemblyLineMachine.kind(recipe) + " (compatible tier)"; continue;
            }
            State trial = state.copy(); Set<ResourceLocation> branch = new HashSet<>(path); branch.add(recipe.id());
            try {
                ItemStack output = recipe.value().getResultItem(level.registryAccess());
                if (AssemblyLineMachine.kind(recipe) == AssemblyLineMachine.Kind.CRUSHER) output.setCount(Math.min(16, output.getCount()));
                int expected = AssemblyLineMachine.kind(recipe) == AssemblyLineMachine.Kind.CIRCUIT_PRESS ? 2 : 1;
                if (output.isEmpty() || output.getCount() > output.getMaxStackSize() || recipe.value().getIngredients().size() != expected) continue;
                int operations = (amount + output.getCount() - 1) / output.getCount();
                for (int op = 0; op < operations; op++) {
                    List<ItemStack> inputs = new ArrayList<>(); Set<Integer> dependencies = new HashSet<>();
                    for (Ingredient input : recipe.value().getIngredients()) {
                        Taken needed = supply(level, input, 1, trial, recipes, workers, branch, depth + 1, budget);
                        inputs.add(needed.stacks.getFirst()); dependencies.addAll(needed.dependencies);
                    }
                    if (trial.tasks.size() >= MAX_OPERATIONS) throw new Missing("Production exceeds 4096 operations");
                    int producer = trial.tasks.size(); trial.tasks.add(new Task(recipe.id(), inputs, output.copy(), dependencies));
                    trial.stock.add(new Supply(output.copy(), producer));
                }
                Taken remainder = supply(level, ingredient, amount, trial, recipes, workers, branch, depth + 1, budget);
                state.replace(trial); taken.addAll(remainder.stacks); deps.addAll(remainder.dependencies);
                return new Taken(taken, deps);
            } catch (Missing missing) { failure = missing.getMessage(); }
        }
        throw new Missing(failure);
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag(); tag.put("requested", saveRequest(requested, registries)); tag.putString("error", error);
        ListTag nodes = new ListTag();
        for (Task t : tasks) {
            CompoundTag node = new CompoundTag(); node.putString("recipe", t.recipe.toString());
            ListTag inputs = new ListTag(); t.inputs.forEach(i -> inputs.add(i.save(registries))); node.put("inputs", inputs);
            node.put("output", t.output.save(registries)); node.putIntArray("dependencies", t.dependencies.stream().mapToInt(Integer::intValue).toArray());
            if (t.machine != null) node.putLong("machine", t.machine);
            node.putBoolean("complete", t.complete); nodes.add(node);
        }
        tag.put("tasks", nodes); return tag;
    }
    public static ProductionPlan load(CompoundTag tag, HolderLookup.Provider registries) {
        ProductionPlan plan = new ProductionPlan(loadRequest(tag.getCompound("requested"), registries));
        plan.error = tag.getString("error");
        for (Tag entry : tag.getList("tasks", Tag.TAG_COMPOUND)) {
            CompoundTag node = (CompoundTag) entry; List<ItemStack> inputs = new ArrayList<>();
            for (Tag input : node.getList("inputs", Tag.TAG_COMPOUND)) inputs.add(ItemStack.parseOptional(registries, (CompoundTag) input));
            Set<Integer> deps = new HashSet<>(); for (int id : node.getIntArray("dependencies")) deps.add(id);
            Task task = new Task(ResourceLocation.parse(node.getString("recipe")), inputs, ItemStack.parseOptional(registries, node.getCompound("output")), deps);
            task.complete = node.getBoolean("complete"); if (node.contains("machine")) task.machine = node.getLong("machine");
            plan.tasks.add(task);
        }
        return plan;
    }
    public static CompoundTag saveRequest(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag(); tag.put("item", stack.copyWithCount(1).save(registries)); tag.putInt("quantity", stack.getCount()); return tag;
    }
    public static ItemStack loadRequest(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack item = ItemStack.parseOptional(registries, tag.getCompound("item")); item.setCount(Math.clamp(tag.getInt("quantity"), 1, MAX_OPERATIONS)); return item;
    }
}
