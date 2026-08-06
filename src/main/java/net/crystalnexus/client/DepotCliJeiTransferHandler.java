package net.crystalnexus.client;

import net.crystalnexus.world.inventory.DepotCliMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.RecipeType;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DepotCliJeiTransferHandler implements IUniversalRecipeTransferHandler<DepotCliMenu> {
    public static final AtomicReference<String> pendingCommand = new AtomicReference<>();
    private static final Set<ResourceLocation> PROGRAMMED_OUTPUTS = new java.util.concurrent.ConcurrentSkipListSet<>();
        private static final Map<String, ResourceLocation> RECIPE_MACHINES = new java.util.concurrent.ConcurrentHashMap<>();
        private static final Map<String, ResourceLocation> RECIPE_CATEGORIES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void markProgrammed(ResourceLocation output) {
        if (output != null) PROGRAMMED_OUTPUTS.add(output);
    }

    /** Associates a JEI recipe instance with its category's machine catalyst. */
    public static void registerMachine(Object recipe, ResourceLocation machine) {
        if (recipe != null && machine != null) RECIPE_MACHINES.put(recipeKey(recipe), machine);
    }

    public static void registerCategory(Object recipe, ResourceLocation category) {
        if (recipe != null && category != null) RECIPE_CATEGORIES.put(recipeKey(recipe), category);
    }

    @Override
    public Class<? extends DepotCliMenu> getContainerClass() {
        return DepotCliMenu.class;
    }

    @Override
    public Optional<MenuType<DepotCliMenu>> getMenuType() {
        // JEI needs the concrete menu type to make this handler eligible for its
        // plus button across third-party recipe categories.
        return Optional.of(CrystalnexusModMenus.DEPOT_CLI.get());
    }

    @Override
    public IRecipeTransferError transferRecipe(DepotCliMenu container, Object recipe,
            IRecipeSlotsView slotsView, Player player, boolean maxTransfer, boolean doTransfer) {
        try {
            ItemStack output = primaryOutput(slotsView);
            // Never block JEI's plus button during its dry-run availability check.
            // The Depot uses its own storage rather than the visible player slots.
            if (output.isEmpty()) return null;
            if (doTransfer) {
                queueFromSlots(slotsView, recipe, machineFor(recipe), RECIPE_CATEGORIES.get(recipeKey(recipe)));
            }
        } catch (RuntimeException ignored) {
            // A modded category must never disable the Depot CLI transfer button.
            return null;
        }
        return null;
    }

        private static void queueFromSlots(IRecipeSlotsView slotsView, Object recipe,
            ResourceLocation machine, ResourceLocation category) {
        ItemStack output = primaryOutput(slotsView);
        if (output.isEmpty()) return;
        ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(output.getItem());
        int count = Math.max(1, output.getCount());
        // Category identity is authoritative. A crafting-table recipe can have a
        // catalyst in another mod's JEI view, so slot count/catalyst alone cannot
        // distinguish it from a machine recipe.
        if (ResourceLocation.fromNamespaceAndPath("minecraft", "crafting").equals(category)
            || isCraftingTableRecipe(recipe)) {
            pendingCommand.set("craft " + outputId + " " + count);
            return;
        }
        if (PROGRAMMED_OUTPUTS.contains(outputId)) {
            pendingCommand.set("process " + outputId + " " + count);
            return;
        }
        List<String> parts = new ArrayList<>();
        for (var slot : slotsView.getSlotViews(RecipeIngredientRole.INPUT)) {
            ItemStack input = slot.getItemStacks().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
            if (input.isEmpty()) continue;
            parts.add(BuiltInRegistries.ITEM.getKey(input.getItem()).toString());
            parts.add(Integer.toString(Math.max(1, input.getCount())));
        }
        if (!parts.isEmpty()) {
            String machineId = machine == null ? "<machine_id>" : machine.toString();
            pendingCommand.set("recipe add " + machineId + " " + outputId + " " + count + " "
                    + String.join(" ", parts));
        }
    }

    private static ResourceLocation machineFor(Object recipe) {
        ResourceLocation synced = RECIPE_MACHINES.get(recipeKey(recipe));
        if (synced != null) return synced;
        String description = (recipe.getClass().getName() + " " + recipe).toLowerCase(java.util.Locale.ROOT);
        if (description.contains("appliedenergistics") || description.contains("inscriber")) {
            return ResourceLocation.parse("ae2:inscriber");
        }
        if (description.contains("metallurgic") || description.contains("infusing")) {
            return ResourceLocation.parse("mekanism:metallurgic_infuser");
        }
        if (description.contains("enrich")) return ResourceLocation.parse("mekanism:enrichment_chamber");
        if (description.contains("purif")) return ResourceLocation.parse("mekanism:purification_chamber");
        if (description.contains("inject")) return ResourceLocation.parse("mekanism:chemical_injection_chamber");
        if (description.contains("compress")) return ResourceLocation.parse("mekanism:osmium_compressor");
        if (description.contains("crush")) return ResourceLocation.parse("mekanism:crusher");
        if (description.contains("smelt")) return ResourceLocation.parse("mekanism:energized_smelter");
        return null;
    }

    private static boolean isCraftingTableRecipe(Object recipe) {
        if (recipe instanceof RecipeHolder<?> holder && holder.value() instanceof CraftingRecipe) return true;
        if (recipe instanceof CraftingRecipe) return true;
        // Create and similar mods wrap vanilla CraftingRecipe objects in their
        // own JEI category class. Use the class hierarchy as a reliable indicator.
        for (Class<?> cls = recipe.getClass(); cls != null && !cls.equals(Object.class); cls = cls.getSuperclass()) {
            for (Class<?> iface : cls.getInterfaces()) {
                if (iface.getName().contains("CraftingRecipe")) return true;
            }
        }
        return craftlikeClassName(recipe);
    }

    private static boolean craftlikeClassName(Object recipe) {
        String name = recipe.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("craftingrecipe") || name.contains("shapedrecipe") || name.contains("shapelessrecipe");
    }

    private static String recipeKey(Object recipe) {
        return recipe.getClass().getName() + "|" + recipe;
    }

    /** Exact-category wrapper: JEI checks these before universal handlers. */
    public static final class CategoryHandler implements IRecipeTransferHandler<DepotCliMenu, Object> {
        private final RecipeType<Object> type;
        private final DepotCliJeiTransferHandler delegate;

        @SuppressWarnings("unchecked")
        public CategoryHandler(ResourceLocation id, Class<?> recipeClass, DepotCliJeiTransferHandler delegate) {
            this.type = new RecipeType<>(id, (Class<Object>) recipeClass);
            this.delegate = delegate;
        }

        @Override
        public Class<? extends DepotCliMenu> getContainerClass() { return DepotCliMenu.class; }

        @Override
        public Optional<MenuType<DepotCliMenu>> getMenuType() {
            return Optional.of(CrystalnexusModMenus.DEPOT_CLI.get());
        }

        @Override
        public RecipeType<Object> getRecipeType() { return type; }

        @Override
        public IRecipeTransferError transferRecipe(DepotCliMenu container, Object recipe,
                IRecipeSlotsView slotsView, Player player, boolean maxTransfer, boolean doTransfer) {
            return delegate.transferRecipe(container, recipe, slotsView, player, maxTransfer, doTransfer);
        }
    }


    private static ItemStack primaryOutput(IRecipeSlotsView slotsView) {
        return slotsView.getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                .flatMap(slot -> slot.getItemStacks().findFirst().stream())
                .filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
    }

}