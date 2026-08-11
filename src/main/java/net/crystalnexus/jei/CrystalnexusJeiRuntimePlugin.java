package net.crystalnexus.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.crystalnexus.cli.DepotJeiRecipeCache;
import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.network.payload.C2S_DepotJeiRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JeiPlugin
public class CrystalnexusJeiRuntimePlugin implements IModPlugin {
    private static volatile IJeiRuntime runtime;
    private static final List<Work> work = new ArrayList<>();
    private static final List<DepotJeiRecipeCache.Recipe> outgoing = new ArrayList<>();
    private static boolean listenerRegistered;
    private static boolean needsSync;
    private static int workIndex;
    private static int generation;
    private static Object connection;
    private static int synced;
    private static boolean syncing;
    private static final Map<ResourceLocation, Class<?>> CATEGORY_RECIPE_CLASSES = new LinkedHashMap<>();

    private record Work(IRecipeCategory<Object> category, Object recipe, List<ResourceLocation> machines) {}

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.parse("crystalnexus:jei_runtime_bridge");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        needsSync = true;
        if (!listenerRegistered) {
            listenerRegistered = true;
            NeoForge.EVENT_BUS.addListener(CrystalnexusJeiRuntimePlugin::clientTick);
        }
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        work.clear();
        outgoing.clear();
    }

    public static boolean showRecipesFor(ItemStack stack) {
        IJeiRuntime jeiRuntime = runtime;
        if (jeiRuntime == null || stack.isEmpty()) {
            return false;
        }

        IFocus<ItemStack> focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
        jeiRuntime.getRecipesGui().show(focus);
        return true;
    }

    private static void clientTick(ClientTickEvent.Post ignored) {
        IJeiRuntime jeiRuntime = runtime;
        Minecraft minecraft = Minecraft.getInstance();
        Object currentConnection = minecraft.getConnection();
        if (jeiRuntime == null || minecraft.player == null || currentConnection == null) {
            connection = null;
            return;
        }
        if (connection != currentConnection) {
            connection = currentConnection;
            needsSync = true;
        }
        if (needsSync) beginSync(jeiRuntime);
        // Send external-machine recipes promptly. A large pack otherwise takes
        // several minutes to reach categories at the end of JEI's registry.
        int remaining = 128;
        while (remaining-- > 0 && workIndex < work.size()) {
            Work current = work.get(workIndex++);
            DepotJeiRecipeCache.Recipe recipe = extract(jeiRuntime, current);
            if (recipe != null) {
                outgoing.add(recipe);
                synced++;
            }
            current.machines().stream().findFirst().ifPresent(machine ->
                    net.crystalnexus.client.DepotCliJeiTransferHandler.registerMachine(current.recipe(), machine));
                net.crystalnexus.client.DepotCliJeiTransferHandler.registerCategory(current.recipe(),
                    current.category().getRecipeType().getUid());
            if (outgoing.size() >= DepotJeiRecipeCache.MAX_CHUNK) flush(false);
        }
        if (workIndex >= work.size() && !outgoing.isEmpty()) flush(false);
        if (syncing && workIndex >= work.size()) {
            syncing = false;
            CrystalnexusMod.LOGGER.info("Synced {} of {} JEI machine recipes to the depot", synced, work.size());
        }
    }

    private static void beginSync(IJeiRuntime jeiRuntime) {
        needsSync = false;
        work.clear();
        outgoing.clear();
        workIndex = 0;
        synced = 0;
        syncing = true;
        generation++;
        IRecipeManager manager = jeiRuntime.getRecipeManager();
        manager.createRecipeCategoryLookup().get().sorted(Comparator.comparingInt(
            (IRecipeCategory<?> category) -> categoryPriority(category.getRecipeType().getUid()))
            .thenComparing(category -> category.getRecipeType().getUid().toString()))
                .forEach(category -> addCategory(manager, category));
        PacketDistributor.sendToServer(new C2S_DepotJeiRecipes(generation, true, List.of()));
    }

    private static int categoryPriority(ResourceLocation id) {
        return switch (id.toString()) {
            case "crystalnexus:ore_crushing_jei" -> 0;
            case "mekanism:metallurgic_infusing" -> 0;
            case "ae2:inscriber" -> 1;
            default -> id.getNamespace().equals("mekanism") ? 2 : 10;
        };
    }

    @SuppressWarnings("unchecked")
    private static void addCategory(IRecipeManager manager, IRecipeCategory<?> rawCategory) {
        IRecipeCategory<Object> category = (IRecipeCategory<Object>) rawCategory;
        ResourceLocation categoryId = category.getRecipeType().getUid();
        CATEGORY_RECIPE_CLASSES.putIfAbsent(categoryId, category.getRecipeType().getRecipeClass());
        if (categoryId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "crafting"))) return;
        // Skip JEI tag/lookup categories (Item Tags, Block Tags, Fluid Tags, etc.)
        // These are informational groupings, not real machine recipes.
        String path = categoryId.getPath().toLowerCase(java.util.Locale.ROOT);
        if (path.contains("tag") || path.contains("lookup")) return;
        List<ResourceLocation> discoveredMachines = manager.createRecipeCatalystLookup(category.getRecipeType()).getItemStack()
                .map(ItemStack::getItem).filter(BlockItem.class::isInstance).map(BlockItem.class::cast)
                .map(BlockItem::getBlock).map(BuiltInRegistries.BLOCK::getKey).distinct().toList();
        // Several integrations expose extra or incorrect catalysts. For categories
        // with a single known machine, use the authoritative mapping rather than
        // allowing a visually similar Crystal Nexus machine to receive the recipe.
        List<ResourceLocation> knownMachines = inferredMachines(categoryId);
        final List<ResourceLocation> machines = knownMachines.isEmpty()
            ? discoveredMachines : knownMachines;
        manager.createRecipeLookup(category.getRecipeType()).get()
                .forEach(recipe -> work.add(new Work(category, recipe, machines)));
    }

    /** Called after JEI categories are available; registers exact handlers so they
     * override third-party category handlers that would otherwise disable +. */
    public static void registerCategoryTransferHandlers(IRecipeTransferRegistration registration) {
        net.crystalnexus.client.DepotCliJeiTransferHandler delegate = new net.crystalnexus.client.DepotCliJeiTransferHandler();
        CATEGORY_RECIPE_CLASSES.forEach((id, recipeClass) -> registration.addRecipeTransferHandler(
                new net.crystalnexus.client.DepotCliJeiTransferHandler.CategoryHandler(id, recipeClass, delegate),
                new mezz.jei.api.recipe.RecipeType<>(id, (Class) recipeClass)));
    }

    private static List<ResourceLocation> inferredMachines(ResourceLocation categoryId) {
        return switch (categoryId.toString()) {
            case "crystalnexus:ore_crushing_jei" -> List.of(ResourceLocation.parse("crystalnexus:crystal_crusher"));
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

    private static DepotJeiRecipeCache.Recipe extract(IJeiRuntime jeiRuntime, Work work) {
        try {
            IRecipeManager manager = jeiRuntime.getRecipeManager();
            IRecipeLayoutDrawable<Object> layout = manager.createRecipeLayoutDrawable(work.category(), work.recipe(),
                    jeiRuntime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElse(null);
            if (layout == null) return null;
            List<DepotJeiRecipeCache.Slot> inputs = layout.getRecipeSlotsView()
                    .getSlotViews(RecipeIngredientRole.INPUT).stream().map(CrystalnexusJeiRuntimePlugin::slot)
                    .filter(slot -> !slot.alternatives().isEmpty())
                    .limit(DepotJeiRecipeCache.MAX_SLOTS).toList();
            List<DepotJeiRecipeCache.StackRef> outputs = layout.getRecipeSlotsView()
                    .getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                    .map(CrystalnexusJeiRuntimePlugin::firstStack).filter(java.util.Objects::nonNull).limit(1).toList();
            if (inputs.isEmpty() || outputs.isEmpty()) return null;
            ResourceLocation categoryId = work.category().getRecipeType().getUid();
            ResourceLocation registryName = work.category().getRegistryName(work.recipe());
            String signature = categoryId + "/" + (registryName == null
                    ? inputs + "->" + outputs : registryName.toString());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("crystalnexus",
                    "jei/" + UUID.nameUUIDFromBytes(signature.getBytes(StandardCharsets.UTF_8)));
            List<ResourceLocation> limitedMachines = work.machines().size() > DepotJeiRecipeCache.MAX_ALTERNATIVES
                    ? work.machines().subList(0, DepotJeiRecipeCache.MAX_ALTERNATIVES)
                    : work.machines();
            return new DepotJeiRecipeCache.Recipe(id, categoryId, work.category().getTitle().getString(),
                    inputs, outputs, limitedMachines);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static DepotJeiRecipeCache.Slot slot(IRecipeSlotView slot) {
        Map<String, DepotJeiRecipeCache.StackRef> alternatives = new LinkedHashMap<>();
        slot.getItemStacks().filter(stack -> !stack.isEmpty()).forEach(stack -> {
            if (alternatives.size() >= DepotJeiRecipeCache.MAX_ALTERNATIVES) return;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            int count = Math.max(1, stack.getCount());
            alternatives.putIfAbsent(id + "#" + count, new DepotJeiRecipeCache.StackRef(id, count));
        });
        return new DepotJeiRecipeCache.Slot(List.copyOf(alternatives.values()));
    }

    private static DepotJeiRecipeCache.StackRef firstStack(IRecipeSlotView slot) {
        return slot.getItemStacks().filter(stack -> !stack.isEmpty()).findFirst()
                .map(stack -> new DepotJeiRecipeCache.StackRef(BuiltInRegistries.ITEM.getKey(stack.getItem()),
                        Math.max(1, stack.getCount()))).orElse(null);
    }

    private static void flush(boolean reset) {
        PacketDistributor.sendToServer(new C2S_DepotJeiRecipes(generation, reset, List.copyOf(outgoing)));
        outgoing.clear();
    }
}
