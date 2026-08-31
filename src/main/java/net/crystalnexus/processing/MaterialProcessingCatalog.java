package net.crystalnexus.processing;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModDataComponents;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.network.payload.S2C_MaterialProfiles;
import net.crystalnexus.jei_recipes.DustSeperationRecipe;
import net.crystalnexus.jei_recipes.FluidChemicalReactionRecipe;
import net.crystalnexus.jei_recipes.RefiningRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class MaterialProcessingCatalog {
    public static final int SLURRY_AMOUNT = 1000;
    public static final int NUGGETS_PER_DUST = 11;
    private static final Gson GSON = new Gson();
    private static volatile Map<String, Profile> profiles = Map.of();
    private static volatile Snapshot snapshot = new Snapshot(Map.of());
    private static volatile RecipeManager recipeManager;

    private MaterialProcessingCatalog() {}

    public record Secondary(Output output, float chance) {}
    public record Output(ResourceLocation id, boolean tag, int count) {
        public ItemStack resolve(String preferredNamespace) {
            if (!tag) {
                Item item = BuiltInRegistries.ITEM.get(id);
                return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
            }
            TagKey<Item> key = TagKey.create(Registries.ITEM, id);
            return BuiltInRegistries.ITEM.getTag(key).stream()
                .flatMap(set -> set.stream())
                .sorted(Comparator.comparing((net.minecraft.core.Holder<Item> holder) ->
                    !BuiltInRegistries.ITEM.getKey(holder.value()).getNamespace().equals(preferredNamespace))
                    .thenComparing(holder -> BuiltInRegistries.ITEM.getKey(holder.value()).toString()))
                .findFirst().map(holder -> new ItemStack(holder.value(), count)).orElse(ItemStack.EMPTY);
        }
    }
    public record Profile(ResourceLocation primaryMaterial, ResourceLocation reagent, boolean reagentTag,
                          int reagentAmount, int crusherMultiplier, int advancedMultiplier,
                          Optional<Secondary> secondary, Set<String> disabledStages, int minimumMachineTier) {
        static Profile defaults(String material) {
            return new Profile(ResourceLocation.fromNamespaceAndPath("c", material),
                ResourceLocation.fromNamespaceAndPath("crystalnexus", "sulfuric_acid"), false,
                SLURRY_AMOUNT, 2, 3,
                Optional.empty(), Set.of(), MaterialProcessingNames.requiredMachineTier(material));
        }
    }
    public record Material(String name, ResourceLocation id, TagKey<Item> ores, TagKey<Item> raw,
                           TagKey<Item> dust, TagKey<Item> ingot, TagKey<Item> nugget, TagKey<Item> crushed,
                           List<Item> crushedItems, Profile profile) {
        public boolean hasCrushed() { return !crushedItems.isEmpty(); }
        public boolean matchesSource(ItemStack stack) { return stack.is(raw) || stack.is(ores); }
        public boolean matchesCrushed(ItemStack stack) { return crushedItems.contains(stack.getItem()); }
        public boolean matchesDust(ItemStack stack) { return stack.is(dust); }
        public Ingredient crushedIngredient() {
            return Ingredient.of(crushedItems.stream().map(ItemStack::new));
        }
        public Ingredient sourceIngredient() {
            return Ingredient.of(java.util.stream.Stream.concat(
                BuiltInRegistries.ITEM.getTag(raw).stream().flatMap(set -> set.stream()).map(holder -> new ItemStack(holder.value())),
                BuiltInRegistries.ITEM.getTag(ores).stream().flatMap(set -> set.stream()).map(holder -> new ItemStack(holder.value()))));
        }
        public ItemStack dust(String preferredNamespace, int count) {
            return new Output(dust.location(), true, count).resolve(preferredNamespace);
        }
        public ItemStack nugget(String preferredNamespace, int count) {
            return new Output(nugget.location(), true, count).resolve(preferredNamespace);
        }
        public ItemStack crushed(String preferredNamespace, int count) {
            return crushedItems.stream().sorted(Comparator.comparing((Item item) ->
                    !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(preferredNamespace))
                    .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .findFirst().map(item -> new ItemStack(item, count)).orElse(ItemStack.EMPTY);
        }
    }
    public record Snapshot(Map<String, Material> materials) {
        public Optional<Material> byId(ResourceLocation id) {
            return materials.values().stream().filter(material -> material.id().equals(id)).findFirst();
        }
        public Optional<Material> source(ItemStack stack) {
            return materials.values().stream().filter(material -> material.matchesSource(stack)).findFirst();
        }
        public Optional<Material> crushed(ItemStack stack) {
            return materials.values().stream().filter(material -> material.matchesCrushed(stack)).findFirst();
        }
        public Optional<Material> dust(ItemStack stack) {
            return materials.values().stream().filter(material -> material.matchesDust(stack)).findFirst();
        }
    }

    public static Snapshot get(Level level) {
        RecipeManager current = level.getRecipeManager();
        if (current != recipeManager) {
            synchronized (MaterialProcessingCatalog.class) {
                if (current != recipeManager) {
                    snapshot = discover(level);
                    recipeManager = current;
                }
            }
        }
        return snapshot;
    }

    public static List<S2C_MaterialProfiles.Entry> profileEntries() {
        return profiles.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .map(entry -> new S2C_MaterialProfiles.Entry(entry.getKey(), entry.getValue())).toList();
    }

    public static void installProfiles(List<S2C_MaterialProfiles.Entry> entries) {
        Map<String, Profile> received = new HashMap<>();
        entries.forEach(entry -> received.put(entry.source(), entry.profile()));
        profiles = Map.copyOf(received);
        recipeManager = null;
    }

    public static FluidStack slurry(ResourceLocation material, int amount) {
        FluidStack stack = new FluidStack(CrystalnexusModFluids.MINERAL_SLURRY.get(), amount);
        stack.set(CrystalnexusModDataComponents.MATERIAL.get(), material);
        return stack;
    }

    public static Optional<ResourceLocation> slurryMaterial(FluidStack stack) {
        return stack.is(CrystalnexusModFluids.MINERAL_SLURRY.get())
            ? Optional.ofNullable(stack.get(CrystalnexusModDataComponents.MATERIAL.get())) : Optional.empty();
    }

    public static ItemStack generatedCrushingResult(Material material, ItemStack input) {
        String namespace = BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace();
        int count = material.profile().crusherMultiplier();
        return material.hasCrushed() ? material.crushed(namespace, count) : material.dust(namespace, count);
    }

    public static List<FluidChemicalReactionRecipe> generatedFluidRecipes(Level level) {
        List<FluidChemicalReactionRecipe> explicit = level.getRecipeManager()
			.getAllRecipesFor(FluidChemicalReactionRecipe.Type.INSTANCE).stream()
			.filter(holder -> !holder.id().getPath().startsWith("cryogenic_flash_freezer_")
				&& !holder.id().getPath().startsWith("titanium_carbide_circuit_press_advanced_"))
			.map(holder -> holder.value()).toList();
        List<FluidChemicalReactionRecipe> generated = new ArrayList<>();
        for (Material material : get(level).materials().values()) {
            if (material.profile().disabledStages().contains("slurry")) continue;
            Ingredient source = material.sourceIngredient();
            Profile profile = material.profile();
            boolean overridden = explicit.stream().anyMatch(recipe -> recipe.getIngredients().stream()
                .anyMatch(input -> java.util.Arrays.stream(source.getItems()).anyMatch(input::test)));
            if (overridden) continue;
            generated.add(new FluidChemicalReactionRecipe(
                Optional.of(new FluidChemicalReactionRecipe.FluidAmount(profile.reagent(), profile.reagentAmount(),
                    Optional.empty(), profile.reagentTag())), Optional.empty(), Optional.of(source), Optional.empty(),
                Optional.of(new FluidChemicalReactionRecipe.FluidAmount(
                    BuiltInRegistries.FLUID.getKey(CrystalnexusModFluids.MINERAL_SLURRY.get()), SLURRY_AMOUNT,
                    Optional.of(material.id()))), Optional.empty(), Optional.empty(), 1, 1));
        }
        return List.copyOf(generated);
    }

    public static List<RefiningRecipe> generatedRefiningRecipes(Level level) {
        List<RefiningRecipe> explicit = level.getRecipeManager().getAllRecipesFor(RefiningRecipe.Type.INSTANCE)
            .stream().map(holder -> holder.value()).toList();
        ResourceLocation slurryId = BuiltInRegistries.FLUID.getKey(CrystalnexusModFluids.MINERAL_SLURRY.get());
        List<RefiningRecipe> generated = new ArrayList<>();
        for (Material material : get(level).materials().values()) {
            if (material.profile().disabledStages().contains("refining")) continue;
            FluidChemicalReactionRecipe.FluidAmount slurry = new FluidChemicalReactionRecipe.FluidAmount(
                slurryId, SLURRY_AMOUNT, Optional.of(material.id()));
            if (explicit.stream().anyMatch(recipe -> recipe.input().matches(slurry.stack()))) continue;
            generated.add(new RefiningRecipe(slurry, Optional.empty(),
                Optional.of(new FluidChemicalReactionRecipe.TaggedItemOutput(
                    material.dust().location(), material.profile().advancedMultiplier())),
                material.profile().minimumMachineTier()));
        }
        return List.copyOf(generated);
    }

    public static List<DustSeperationRecipe> generatedSeparatorRecipes(Level level) {
        List<DustSeperationRecipe> explicit = level.getRecipeManager()
            .getAllRecipesFor(DustSeperationRecipe.Type.INSTANCE).stream().map(holder -> holder.value()).toList();
        List<DustSeperationRecipe> generated = new ArrayList<>();
        for (Material material : get(level).materials().values()) {
            if (material.profile().disabledStages().contains("separation")) continue;
            ItemStack nugget = material.nugget("crystalnexus", NUGGETS_PER_DUST);
            if (nugget.isEmpty()) continue;
            Ingredient dust = Ingredient.of(material.dust());
            boolean overridden = explicit.stream().anyMatch(recipe -> !recipe.getIngredients().isEmpty()
                && java.util.Arrays.stream(dust.getItems()).anyMatch(recipe.getIngredients().getFirst()::test));
            if (overridden) continue;
            generated.add(new DustSeperationRecipe(Optional.empty(),
                Optional.of(new FluidChemicalReactionRecipe.TaggedItemOutput(
                    material.nugget().location(), NUGGETS_PER_DUST)),
                NonNullList.of(Ingredient.EMPTY, dust), 1, Optional.empty(), Optional.empty(), Optional.empty(), 0f,
                material.profile().minimumMachineTier()));
        }
        return List.copyOf(generated);
    }

    static String materialName(ResourceLocation tag) {
        return MaterialProcessingNames.extract(tag.toString());
    }

    private static Snapshot discover(Level level) {
        Map<String, TagKey<Item>> dusts = family("dusts");
        Map<String, TagKey<Item>> raws = family("raw_materials");
        Map<String, TagKey<Item>> ores = family("ores");
        Map<String, TagKey<Item>> ingots = family("ingots");
        Map<String, TagKey<Item>> nuggets = family("nuggets");
        Map<String, TagKey<Item>> crushed = new HashMap<>();
        for (String family : List.of("crushed_ores", "crushed_raw_materials", "crushed_materials"))
            family(family).forEach(crushed::putIfAbsent);

        Map<String, Material> found = new LinkedHashMap<>();
        int[] skipped = {0};
        dusts.forEach((name, dust) -> {
            TagKey<Item> raw = raws.get(name);
            TagKey<Item> ore = ores.get(name);
            boolean dustValid = BuiltInRegistries.ITEM.getTag(dust).stream().anyMatch(set -> set.stream().findAny().isPresent());
            boolean sourceValid = raw != null && BuiltInRegistries.ITEM.getTag(raw).stream().anyMatch(set -> set.stream().findAny().isPresent())
                || ore != null && BuiltInRegistries.ITEM.getTag(ore).stream().anyMatch(set -> set.stream().findAny().isPresent());
            if (!dustValid || !sourceValid) { skipped[0]++; return; }
            Profile profile = profiles.getOrDefault(name, Profile.defaults(name));
            TagKey<Item> emptyRaw = raw == null ? itemTag("crystalnexus", "empty/" + name) : raw;
            TagKey<Item> emptyOre = ore == null ? itemTag("crystalnexus", "empty/" + name) : ore;
            TagKey<Item> ingot = ingots.getOrDefault(name, itemTag("crystalnexus", "empty_ingot/" + name));
            TagKey<Item> nugget = nuggets.getOrDefault(name, itemTag("crystalnexus", "empty_nugget/" + name));
            TagKey<Item> crushedTag = crushed.getOrDefault(name, itemTag("crystalnexus", "empty_crushed/" + name));
            List<Item> crushedItems = BuiltInRegistries.ITEM.getTag(crushedTag).stream().flatMap(set -> set.stream())
                .map(net.minecraft.core.Holder::value).toList();
            if (crushedItems.isEmpty()) crushedItems = inferCrushed(level, name, emptyRaw, emptyOre);
            found.put(name, new Material(name, profile.primaryMaterial(), emptyOre, emptyRaw, dust, ingot, nugget, crushedTag,
                List.copyOf(crushedItems), profile));
        });
        long advanced = found.values().stream().filter(Material::hasCrushed).count();
        CrystalnexusMod.LOGGER.debug("Dynamic ore processing: discovered={}, skipped={}, basic={}, advanced={}, overrides={}",
            found.size(), skipped[0], found.size() - advanced, advanced, profiles.size());
        return new Snapshot(Map.copyOf(found));
    }

    private static List<Item> inferCrushed(Level level, String material, TagKey<Item> raw, TagKey<Item> ores) {
        return level.getRecipeManager().getRecipes().stream().map(holder -> holder.value()).map(recipe -> {
            try {
                ItemStack output = recipe.getResultItem(level.registryAccess());
                if (output.isEmpty()) return ItemStack.EMPTY;
                String path = BuiltInRegistries.ITEM.getKey(output.getItem()).getPath();
                if (!path.contains("crushed") || !path.contains(material)) return ItemStack.EMPTY;
                boolean related = java.util.stream.Stream.concat(
                    recipe.getIngredients().stream().flatMap(ingredient -> java.util.Arrays.stream(ingredient.getItems())),
                    reflectedInputs(recipe).stream()).anyMatch(input -> input.is(raw) || input.is(ores));
                return related ? output : ItemStack.EMPTY;
            } catch (RuntimeException ignored) {
                return ItemStack.EMPTY;
            }
        }).filter(stack -> !stack.isEmpty()).map(ItemStack::getItem).distinct()
            .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString())).toList();
    }

    private static List<ItemStack> reflectedInputs(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        try {
            Object input = recipe.getClass().getMethod("getInput").invoke(recipe);
            Object representations = input.getClass().getMethod("getRepresentations").invoke(input);
            if (representations instanceof List<?> list)
                return list.stream().filter(ItemStack.class::isInstance).map(ItemStack.class::cast).toList();
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return List.of();
    }

    private static Map<String, TagKey<Item>> family(String family) {
        Map<String, TagKey<Item>> result = new HashMap<>();
        BuiltInRegistries.ITEM.getTagNames().filter(tag -> {
            ResourceLocation id = tag.location();
            return (id.getNamespace().equals("c") || id.getNamespace().equals("forge"))
                && id.getPath().startsWith(family + "/");
        }).forEach(tag -> result.merge(materialName(tag.location()), tag,
            (oldTag, newTag) -> oldTag.location().getNamespace().equals("c") ? oldTag : newTag));
        return result;
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new DefinitionReloadListener());
    }

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent event) {
        S2C_MaterialProfiles payload = new S2C_MaterialProfiles(profileEntries());
        if (event.getPlayer() != null) PacketDistributor.sendToPlayer(event.getPlayer(), payload);
        else event.getPlayerList().getPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    private static final class DefinitionReloadListener implements PreparableReloadListener {
        @Override
        public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resources,
                                              ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler,
                                              Executor background, Executor game) {
            return CompletableFuture.supplyAsync(() -> loadProfiles(resources), background)
                .thenCompose(barrier::wait).thenAcceptAsync(loaded -> {
                    profiles = loaded;
                    recipeManager = null;
                    CrystalnexusMod.LOGGER.debug("Loaded {} material-processing overrides", loaded.size());
                }, game);
        }

        private static Map<String, Profile> loadProfiles(ResourceManager resources) {
            Map<String, Profile> loaded = new HashMap<>();
            resources.listResources("crystalnexus/material_processing", id -> id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        Profile profile = parseProfile(json);
                        String source = json.has("source_material")
                            ? path(json.get("source_material").getAsString()) : path(profile.primaryMaterial().toString());
                        loaded.put(source, profile);
                    } catch (Exception exception) {
                        CrystalnexusMod.LOGGER.warn("Ignoring invalid material-processing definition {}", id, exception);
                    }
                });
            return Map.copyOf(loaded);
        }
    }

    static Profile parseProfile(JsonObject json) {
        ResourceLocation primary = ResourceLocation.parse(json.get("primary_material").getAsString());
        JsonObject reagent = json.has("reagent") ? json.getAsJsonObject("reagent") : new JsonObject();
        boolean reagentTag = reagent.has("tag");
        ResourceLocation reagentId = ResourceLocation.parse(reagentTag ? reagent.get("tag").getAsString()
            : reagent.has("fluid") ? reagent.get("fluid").getAsString() : "minecraft:water");
        int amount = reagent.has("amount") ? reagent.get("amount").getAsInt() : SLURRY_AMOUNT;
        int crusher = json.has("crusher_multiplier") ? json.get("crusher_multiplier").getAsInt() : 2;
        int advanced = json.has("advanced_multiplier") ? json.get("advanced_multiplier").getAsInt() : 3;
        Optional<Secondary> secondary = Optional.empty();
        if (json.has("secondary_output")) {
            JsonObject value = json.getAsJsonObject("secondary_output");
            boolean tag = value.has("tag");
            ResourceLocation output = ResourceLocation.parse(value.get(tag ? "tag" : "item").getAsString());
            secondary = Optional.of(new Secondary(new Output(output, tag,
                value.has("count") ? value.get("count").getAsInt() : 1),
                value.has("chance") ? value.get("chance").getAsFloat() : 1f));
        }
        Set<String> disabled = json.has("disabled_auto_stages")
            ? GSON.fromJson(json.get("disabled_auto_stages"), String[].class) == null ? Set.of()
                : Set.of(GSON.fromJson(json.get("disabled_auto_stages"), String[].class)) : Set.of();
        if (amount <= 0 || crusher <= 0 || advanced <= 0)
            throw new IllegalArgumentException("Amounts and multipliers must be positive");
        int minimumTier = json.has("minimum_machine_tier") ? json.get("minimum_machine_tier").getAsInt()
            : MaterialProcessingNames.requiredMachineTier(path(primary.toString()));
        if (minimumTier < 1 || minimumTier > MachineTier.HYPER.level())
            throw new IllegalArgumentException("minimum_machine_tier must be between 1 and " + MachineTier.HYPER.level());
        return new Profile(primary, reagentId, reagentTag, amount, crusher, advanced, secondary, disabled, minimumTier);
    }

    public static int defaultRequiredTier(String material) {
        return MaterialProcessingNames.requiredMachineTier(material);
    }

    private static String path(String id) {
        return MaterialProcessingNames.normalizeMaterial(id);
    }
}
