package net.crystalnexus.integration.ae2;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.CraftingRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** AE2 19 uses EMI/REI internally; JEI can use the same encoding and network path. */
public final class Ae2PatternJeiTransferHandler implements IUniversalRecipeTransferHandler<PatternEncodingTermMenu> {
    private final IRecipeTransferHandlerHelper helper;

    public Ae2PatternJeiTransferHandler(IRecipeTransferHandlerHelper helper) { this.helper = helper; }
    @Override public Class<PatternEncodingTermMenu> getContainerClass() { return PatternEncodingTermMenu.class; }
    @Override public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() { return Optional.empty(); }

    @SuppressWarnings("unchecked")
    @Override public IRecipeTransferError transferRecipe(PatternEncodingTermMenu menu, Object recipe,
            IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        var inputSlots = slots.getSlotViews(RecipeIngredientRole.INPUT);
        var inputs = inputSlots.stream().map(Ae2PatternJeiTransferHandler::stacks).toList();
        var outputs = slots.getSlotViews(RecipeIngredientRole.OUTPUT).stream()
            .map(Ae2PatternJeiTransferHandler::stacks).filter(list -> !list.isEmpty()).map(List::getFirst).toList();
        if (inputs.stream().allMatch(List::isEmpty) || outputs.isEmpty()) return helper.createInternalError();
        if (recipe instanceof net.crystalnexus.jei_recipes.MultiblockStructureRecipe
                || recipe instanceof net.crystalnexus.jei_recipes.ReactorMultiblockGuideRecipe
                || recipe instanceof net.crystalnexus.jei_recipes.ReactionMultiblockGuideRecipe)
            return helper.createInternalError();

        if (recipe instanceof RecipeHolder<?> holder && holder.value() instanceof CraftingRecipe
                && EncodingHelper.isSupportedCraftingRecipe(holder.value())) {
            // AE2 expects all nine crafting positions, including empty cells.
            var positions = helper.getGuiSlotIndexToIngredientMap((RecipeHolder<CraftingRecipe>) holder);
            var craftingInputs = new ArrayList<List<GenericStack>>();
            for (int i = 0; i < 9; i++) {
                var ingredient = positions.get(i);
                craftingInputs.add(ingredient == null ? List.of() : java.util.Arrays.stream(ingredient.getItems())
                    .filter(stack -> !stack.isEmpty()).map(stack -> new GenericStack(AEItemKey.of(stack), 1)).toList());
            }
            if (doTransfer) EncodingHelper.encodeCraftingRecipe(menu, holder, craftingInputs, stack -> true);
            return null;
        }

        var processingInputs = inputs.stream().filter(list -> !list.isEmpty()).toList();
        if (processingInputs.size() != inputs.size()) return helper.createInternalError();
        // JEI displays one item for bulk compression; a pattern needs the full batch.
        if (recipe instanceof net.crystalnexus.jei_recipes.SingularityCompressionRecipe compression)
            processingInputs = List.of(inputs.getFirst().stream()
                .map(stack -> new GenericStack(stack.what(), compression.getInputCount(0))).toList());
        long outputCount = outputs.stream().map(GenericStack::what).distinct().count();
        if (processingInputs.size() > menu.getProcessingInputSlots().length
                || outputCount > menu.getProcessingOutputSlots().length)
            return helper.createUserErrorWithTooltip(Component.translatable("jei.crystalnexus.pattern_too_large"));
        if (doTransfer) EncodingHelper.encodeProcessingRecipe(menu, processingInputs, outputs);
        return null;
    }

    static List<GenericStack> stacks(IRecipeSlotView slot) {
        var stacks = new ArrayList<GenericStack>();
        slot.getItemStacks().filter(stack -> !stack.isEmpty()).forEach(stack ->
            stacks.add(new GenericStack(AEItemKey.of(stack), stack.getCount())));
        slot.getIngredients(NeoForgeTypes.FLUID_STACK).filter(stack -> !stack.isEmpty()).forEach(stack ->
            stacks.add(new GenericStack(AEFluidKey.of(stack), stack.getAmount())));
        return stacks;
    }
}
