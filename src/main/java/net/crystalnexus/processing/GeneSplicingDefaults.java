package net.crystalnexus.processing;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.GeneSplicingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Adds defaults before recipes are sent to clients, so machines and JEI use the same outputs. */
@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class GeneSplicingDefaults {
    private GeneSplicingDefaults() {}

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent event) {
        var server = event.getPlayerList().getServer();
        var manager = server.getRecipeManager();
        var recipes = new ArrayList<RecipeHolder<?>>(manager.getRecipes());
        Set<ResourceLocation> defined = new HashSet<>();
        manager.getAllRecipesFor(GeneSplicingRecipe.Type.INSTANCE)
            .forEach(holder -> defined.add(holder.value().mob()));
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            var mob = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (defined.contains(mob) || SpawnEggItem.byId(type) == null) continue;
            ItemStack drop = firstDrop(server.getResourceManager(), type.getDefaultLootTable().location(), new HashSet<>());
            var recipe = new GeneSplicingRecipe(Ingredient.of(CrystalnexusModItems.BIOMASS.get()),
                Ingredient.of(CrystalnexusModItems.PRISON_CUBE.get()), mob, drop, 32, 1);
            recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("crystalnexus",
                "generated_gene_splicing/" + mob.getNamespace() + "/" + mob.getPath()), recipe));
        }
        if (recipes.size() != manager.getRecipes().size()) manager.replaceRecipes(recipes);
    }

    private static ItemStack firstDrop(ResourceManager resources, ResourceLocation table, Set<ResourceLocation> visited) {
        if (!visited.add(table)) return ItemStack.EMPTY;
        var file = table.withPath("loot_table/" + table.getPath() + ".json");
        var resource = resources.getResource(file);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        try (var reader = resource.get().openAsReader()) {
            return firstDrop(resources, JsonParser.parseReader(reader), visited);
        } catch (Exception exception) {
            CrystalnexusMod.LOGGER.warn("Unable to derive gene-splicing drop from {}", table, exception);
            return ItemStack.EMPTY;
        }
    }

    // One output slot holds one ordinary drop; explicit recipes control special items and quantities.
    private static ItemStack firstDrop(ResourceManager resources, JsonElement element, Set<ResourceLocation> visited) {
        if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            String type = object.has("type") ? object.get("type").getAsString() : "";
            if (type.equals("minecraft:item") && object.has("name")) {
                var id = ResourceLocation.parse(object.get("name").getAsString());
                if (BuiltInRegistries.ITEM.containsKey(id)) return new ItemStack(BuiltInRegistries.ITEM.get(id));
            }
            if (type.equals("minecraft:loot_table") && object.has("value")) {
                var value = object.get("value");
                ItemStack drop = value.isJsonPrimitive()
                    ? firstDrop(resources, ResourceLocation.parse(value.getAsString()), visited)
                    : firstDrop(resources, value, visited);
                if (!drop.isEmpty()) return drop;
            }
            for (String key : new String[]{"pools", "entries", "children"}) {
                if (!object.has(key)) continue;
                ItemStack drop = firstDrop(resources, object.get(key), visited);
                if (!drop.isEmpty()) return drop;
            }
        } else if (element.isJsonArray()) {
            for (var child : element.getAsJsonArray()) {
                ItemStack drop = firstDrop(resources, child, visited);
                if (!drop.isEmpty()) return drop;
            }
        }
        return ItemStack.EMPTY;
    }
}
