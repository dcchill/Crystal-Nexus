package net.crystalnexus.processing;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.jei_recipes.PartsAssemblingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class PartsAssemblingDefaults {
    private PartsAssemblingDefaults() {}

    // Same namespace preference as the crusher; item ID breaks ties deterministically.
    public static final Comparator<ItemStack> OUTPUT_ORDER = Comparator
        .comparingInt((ItemStack stack) -> switch (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()) {
            case "crystalnexus" -> 0;
            case "alltheores" -> 1;
            default -> 2;
        }).thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent event) {
        var manager = event.getPlayerList().getServer().getRecipeManager();
        var recipes = new ArrayList<RecipeHolder<?>>(manager.getRecipes());
        var explicit = manager.getAllRecipesFor(PartsAssemblingRecipe.Type.INSTANCE);
        Map<String, List<ItemStack>> ingots = family("ingots", "ingot");
        for (var mode : PartsAssemblingRecipe.Mode.values()) {
            String part = mode.serializedName();
            var outputs = family(part + "s", part);
            for (var entry : ingots.entrySet()) {
                var candidates = outputs.getOrDefault(entry.getKey(), List.of());
                ItemStack output = candidates.stream().min(OUTPUT_ORDER).orElse(ItemStack.EMPTY);
                if (output.isEmpty()) continue;
                // Preserve datapack recipes, including their counts and processing costs.
                var inputs = entry.getValue().stream().filter(input -> explicit.stream().noneMatch(holder ->
                    holder.value().mode() == mode && holder.value().ingredient().test(input))).toList();
                if (inputs.isEmpty()) continue;
                var id = ResourceLocation.fromNamespaceAndPath("crystalnexus",
                    "generated_parts_assembling/" + part + "/" + entry.getKey());
                recipes.add(new RecipeHolder<>(id, new PartsAssemblingRecipe(
                    Ingredient.of(inputs.stream()), output.copyWithCount(mode == PartsAssemblingRecipe.Mode.BOLT ? 2 : 1),
                    mode, 100, 10)));
            }
        }
        var rods = family("rods", "rod");
        var ids = recipes.stream().map(RecipeHolder::id).collect(java.util.stream.Collectors.toSet());
        for (var holder : List.copyOf(recipes)) {
            if (!(holder.value() instanceof PartsAssemblingRecipe recipe) || recipe.mode() != PartsAssemblingRecipe.Mode.BOLT
                    || isLatheOnly(holder.id()) || isLatheRecipe(recipe)) continue;
            var inputs = java.util.Arrays.stream(recipe.ingredient().getItems()).flatMap(input -> materials(input).stream())
                .distinct().flatMap(material -> rods.getOrDefault(material, List.of()).stream()).toList();
            var id = ResourceLocation.fromNamespaceAndPath("crystalnexus",
                "generated_lathing/" + holder.id().getNamespace() + "/" + holder.id().getPath());
            if (inputs.isEmpty() || !ids.add(id)) continue;
            recipes.add(new RecipeHolder<>(id, new PartsAssemblingRecipe(Ingredient.of(inputs.stream()),
                recipe.getResultItem(event.getPlayerList().getServer().registryAccess()), recipe.mode(),
                recipe.processingTime(), recipe.energyPerTick())));
        }
        var plates = family("plates", "plate");
        for (var entry : family("storage_blocks", "storage_block").entrySet()) {
            var output = plates.getOrDefault(entry.getKey(), List.of()).stream().min(OUTPUT_ORDER).orElse(ItemStack.EMPTY);
            var id = ResourceLocation.fromNamespaceAndPath("crystalnexus", "generated_pressing/" + entry.getKey());
            if (output.isEmpty() || !ids.add(id)) continue;
            recipes.add(new RecipeHolder<>(id, new PartsAssemblingRecipe(Ingredient.of(entry.getValue().stream()),
                output.copyWithCount(12), PartsAssemblingRecipe.Mode.PLATE, 100, 10)));
        }
        if (recipes.size() != manager.getRecipes().size()) manager.replaceRecipes(recipes);
    }

    public static boolean isPressOnly(ResourceLocation id) {
        return id.getNamespace().equals("crystalnexus") && id.getPath().startsWith("generated_pressing/");
    }

    public static boolean isLatheOnly(ResourceLocation id) {
        return id.getNamespace().equals("crystalnexus") && id.getPath().startsWith("generated_lathing/");
    }

    public static boolean isLatheRecipe(PartsAssemblingRecipe recipe) {
        return recipe.mode() == PartsAssemblingRecipe.Mode.ROD || recipe.mode() == PartsAssemblingRecipe.Mode.BOLT
            && recipe.ingredient().getItems().length > 0
            && java.util.Arrays.stream(recipe.ingredient().getItems()).allMatch(PartsAssemblingDefaults::isRod);
    }

    public static boolean isRod(ItemStack input) {
        return input.getTags().map(tag -> tag.location()).anyMatch(id ->
            (id.getNamespace().equals("c") || id.getNamespace().equals("forge"))
                && (id.getPath().startsWith("rods/") || id.getPath().startsWith("rod/")));
    }

    private static List<String> materials(ItemStack input) {
        return input.getTags().map(tag -> tag.location())
            .filter(id -> id.getNamespace().equals("c") || id.getNamespace().equals("forge"))
            .map(ResourceLocation::getPath)
            .filter(path -> path.startsWith("ingots/") || path.startsWith("ingot/")
                || path.startsWith("rods/") || path.startsWith("rod/"))
            .map(path -> path.substring(path.indexOf('/') + 1)).distinct().sorted().toList();
    }

    public static ItemStack associatedDust(ItemStack input) {
        var dusts = family("dusts", "dust");
        return materials(input).stream().flatMap(material -> dusts.getOrDefault(material, List.of()).stream())
            .min(OUTPUT_ORDER).map(stack -> stack.copyWithCount(1)).orElse(ItemStack.EMPTY);
    }

    private static Map<String, List<ItemStack>> family(String plural, String singular) {
        Map<String, List<ItemStack>> result = new TreeMap<>();
        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            var id = pair.getFirst().location();
            if (!id.getNamespace().equals("c") && !id.getNamespace().equals("forge")) return;
            String path = id.getPath();
            int slash = path.indexOf('/');
            if (slash < 0 || slash == path.length() - 1) return;
            String family = path.substring(0, slash);
            if (!family.equals(plural) && !family.equals(singular)) return;
            var stacks = result.computeIfAbsent(path.substring(slash + 1), ignored -> new ArrayList<>());
            pair.getSecond().forEach(holder -> stacks.add(new ItemStack(holder.value())));
        });
        return result;
    }
}
