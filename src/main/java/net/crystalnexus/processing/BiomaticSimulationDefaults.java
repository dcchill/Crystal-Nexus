package net.crystalnexus.processing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.jei_recipes.BiomaticSimulationRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class BiomaticSimulationDefaults {
    private static final String PREFIX = "generated_biomatic_simulation/";

    private BiomaticSimulationDefaults() {}

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent event) {
        var server = event.getPlayerList().getServer();
        if (event.getPlayer() != null && server.getRecipeManager().getRecipes().stream()
                .anyMatch(BiomaticSimulationDefaults::generated)) return;
        rebuild(server);
    }

    public static void rebuild(MinecraftServer server) {
        var manager = server.getRecipeManager();
        var recipes = new ArrayList<RecipeHolder<?>>(manager.getRecipes());
        recipes.removeIf(BiomaticSimulationDefaults::generated);
        var explicit = recipes.stream().map(RecipeHolder::value)
            .filter(BiomaticSimulationRecipe.class::isInstance).map(BiomaticSimulationRecipe.class::cast).toList();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem plant)) continue;
            ItemStack input = new ItemStack(item);
            if (explicit.stream().anyMatch(recipe -> recipe.getIngredients().getFirst().test(input))) continue;
            try {
                ItemStack output = discover(server.overworld(), plant);
                if (output.isEmpty()) continue;
                var id = BuiltInRegistries.ITEM.getKey(item);
                recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID,
                    PREFIX + id.getNamespace() + "/" + id.getPath()), new BiomaticSimulationRecipe(
                        output.copyWithCount(6), NonNullList.of(Ingredient.EMPTY, Ingredient.of(item)))));
            } catch (RuntimeException exception) {
                CrystalnexusMod.LOGGER.warn("Unable to derive biomatic product for {}",
                    BuiltInRegistries.ITEM.getKey(item), exception);
            }
        }
        manager.replaceRecipes(recipes);
    }

    private static boolean generated(RecipeHolder<?> recipe) {
        return recipe.value() instanceof BiomaticSimulationRecipe
            && recipe.id().getNamespace().equals(CrystalnexusMod.MODID) && recipe.id().getPath().startsWith(PREFIX);
    }

    public static ItemStack discover(ServerLevel level, BlockItem input) {
        Block block = input.getBlock();
        Set<Item> products = new HashSet<>();
        if (block instanceof SaplingBlock sapling) {
            if (!treeProducts(level, sapling.treeGrower, products)) return ItemStack.EMPTY;
        } else if (block instanceof AzaleaBlock) {
            if (!treeProducts(level, TreeGrower.AZALEA, products)) return ItemStack.EMPTY;
        } else if (block instanceof MushroomBlock mushroom) {
            if (!featureProducts(level, mushroom.feature, products)) return ItemStack.EMPTY;
        } else if (block instanceof FungusBlock fungus) {
            if (!featureProducts(level, fungus.feature, products)) return ItemStack.EMPTY;
        } else if (block instanceof StemBlock stem) {
            var fruit = level.registryAccess().registryOrThrow(Registries.BLOCK).get(stem.fruit);
            if (fruit != null) products.add(fruit.asItem());
        } else {
            BlockState mature = block.defaultBlockState();
            if (mature.is(BlockTags.SAPLINGS)) return ItemStack.EMPTY;
            var ageProperty = block.getStateDefinition().getProperty("age");
            if (!(block instanceof CropBlock || ageProperty instanceof IntegerProperty && (block instanceof BushBlock || block instanceof BonemealableBlock)
                    || block instanceof FlowerBlock || block instanceof TallFlowerBlock || block instanceof GrowingPlantHeadBlock
                    || block instanceof GrowingPlantBodyBlock || block instanceof SugarCaneBlock
                    || block instanceof CactusBlock || block instanceof BambooStalkBlock)) return ItemStack.EMPTY;
            if (block instanceof CropBlock crop) mature = crop.getStateForAge(crop.getMaxAge());
            else if (ageProperty instanceof IntegerProperty age)
                mature = mature.setValue(age, age.getPossibleValues().stream().max(Integer::compareTo).orElseThrow());
            var params = new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE, mature)
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO).withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .create(LootContextParamSets.BLOCK);
            var context = new LootContext.Builder(params).create(Optional.empty());
            if (!lootProducts(level, mature.getBlock().getLootTable(), context, products, new HashSet<>())) return ItemStack.EMPTY;
            if (products.size() > 1 && input.builtInRegistryHolder().is(Tags.Items.SEEDS)) products.remove(input);
        }
        products.remove(Items.AIR);
        return products.size() == 1 ? new ItemStack(products.iterator().next()) : ItemStack.EMPTY;
    }

    private static boolean treeProducts(ServerLevel level, TreeGrower grower, Set<Item> products) {
        for (var key : List.of(grower.tree, grower.secondaryTree, grower.megaTree,
                grower.secondaryMegaTree, grower.flowers, grower.secondaryFlowers)) {
            if (key.isPresent() && !featureProducts(level, key.get(), products)) return false;
        }
        return true;
    }

    private static boolean featureProducts(ServerLevel level, ResourceKey<ConfiguredFeature<?, ?>> key, Set<Item> products) {
        var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(key);
        if (feature == null) return false;
        if (feature.config() instanceof TreeConfiguration tree) return providerProducts(tree.trunkProvider, products);
        if (feature.config() instanceof HugeMushroomFeatureConfiguration mushroom) return providerProducts(mushroom.capProvider, products);
        if (feature.config() instanceof HugeFungusConfiguration fungus) {
            products.add(fungus.stemState.getBlock().asItem());
            return true;
        }
        return false;
    }

    private static boolean providerProducts(BlockStateProvider provider, Set<Item> products) {
        var json = BlockStateProvider.CODEC.encodeStart(JsonOps.INSTANCE, provider).result();
        return json.isPresent() && providerProducts(json.get().getAsJsonObject(), products);
    }

    private static boolean providerProducts(JsonObject provider, Set<Item> products) {
        return switch (provider.get("type").getAsString()) {
            case "minecraft:simple_state_provider", "minecraft:rotated_block_provider" -> addState(provider.get("state"), products);
            case "minecraft:randomized_int_state_provider" -> providerProducts(provider.getAsJsonObject("source"), products);
            case "minecraft:weighted_state_provider" -> {
                for (var entry : provider.getAsJsonArray("entries"))
                    if (!addState(entry.getAsJsonObject().get("data"), products)) yield false;
                yield true;
            }
            case "minecraft:noise_provider", "minecraft:dual_noise_provider" -> {
                for (var state : provider.getAsJsonArray("states")) if (!addState(state, products)) yield false;
                yield true;
            }
            default -> false;
        };
    }

    private static boolean addState(JsonElement json, Set<Item> products) {
        var state = BlockState.CODEC.parse(JsonOps.INSTANCE, json).result();
        state.ifPresent(value -> products.add(value.getBlock().asItem()));
        return state.isPresent();
    }

    private static boolean lootProducts(ServerLevel level, ResourceKey<LootTable> key, LootContext context,
                                        Set<Item> products, Set<ResourceKey<LootTable>> visited) {
        if (!visited.add(key)) return false;
        try {
            var table = level.getServer().reloadableRegistries().getLootTable(key);
            var json = LootTable.DIRECT_CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), table).result();
            return json.isPresent() && lootProducts(level, json.get(), context, products, visited);
        } finally {
            visited.remove(key);
        }
    }

    private static boolean lootProducts(ServerLevel level, JsonElement json, LootContext context,
                                        Set<Item> products, Set<ResourceKey<LootTable>> visited) {
        JsonObject object = json.getAsJsonObject();
        if (object.has("conditions")) for (var condition : object.getAsJsonArray("conditions")) {
            // shortcut: only context-independent harvest conditions, custom loot conditions need an explicit recipe.
            if (!harvestCondition(condition)) {
                if (condition.isJsonObject() && condition.getAsJsonObject().get("condition").getAsString()
                        .equals("minecraft:random_chance")) return true;
                return false;
            }
            var parsed = LootItemCondition.DIRECT_CODEC.parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), condition).result();
            if (parsed.isEmpty()) return false;
            if (!parsed.get().test(context)) return true;
        }
        if (object.has("functions")) for (var function : object.getAsJsonArray("functions")) {
            String type = function.getAsJsonObject().get("function").getAsString();
            if (!Set.of("minecraft:set_count", "minecraft:apply_bonus", "minecraft:explosion_decay").contains(type)) return false;
        }
        if (object.has("type")) {
            switch (object.get("type").getAsString()) {
                case "minecraft:item" -> {
                    var id = ResourceLocation.parse(object.get("name").getAsString());
                    if (!BuiltInRegistries.ITEM.containsKey(id)) return false;
                    products.add(BuiltInRegistries.ITEM.get(id));
                }
                case "minecraft:loot_table" -> {
                    var value = object.get("value");
                    return value.isJsonObject() ? lootProducts(level, value, context, products, visited)
                        : lootProducts(level, ResourceKey.create(Registries.LOOT_TABLE,
                            ResourceLocation.parse(value.getAsString())), context, products, visited);
                }
                case "minecraft:empty", "minecraft:block", "minecraft:group" -> {}
                // All branches are considered; different possible products remain ambiguous.
                case "minecraft:alternatives", "minecraft:sequence" -> {}
                default -> { return false; }
            }
        }
        for (String field : List.of("pools", "entries", "children")) {
            if (object.has(field)) for (var child : object.getAsJsonArray(field))
                if (!lootProducts(level, child, context, products, visited)) return false;
        }
        return true;
    }

    private static boolean harvestCondition(JsonElement json) {
        if (json.isJsonArray()) {
            for (var child : json.getAsJsonArray()) if (!harvestCondition(child)) return false;
            return true;
        }
        var condition = json.getAsJsonObject();
        return switch (condition.get("condition").getAsString()) {
            case "minecraft:block_state_property", "minecraft:survives_explosion", "minecraft:match_tool" -> true;
            case "minecraft:inverted" -> harvestCondition(condition.get("term"));
            case "minecraft:all_of", "minecraft:any_of" -> harvestCondition(condition.get("terms"));
            default -> false;
        };
    }
}
