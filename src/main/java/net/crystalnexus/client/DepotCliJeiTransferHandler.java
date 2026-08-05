package net.crystalnexus.client;

import net.crystalnexus.world.inventory.DepotCliMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class DepotCliJeiTransferHandler implements IUniversalRecipeTransferHandler<DepotCliMenu> {
    public static final AtomicReference<String> pendingCommand = new AtomicReference<>();

    @Override
    public Class<? extends DepotCliMenu> getContainerClass() {
        return DepotCliMenu.class;
    }

    @Override
    public Optional<MenuType<DepotCliMenu>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public IRecipeTransferError transferRecipe(DepotCliMenu container, Object recipe,
            IRecipeSlotsView slotsView, Player player, boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            try {
                var outputs = slotsView.getSlotViews(RecipeIngredientRole.OUTPUT);
                ItemStack output = outputs.stream()
                        .flatMap(slot -> slot.getItemStacks().findFirst().stream())
                        .filter(stack -> !stack.isEmpty())
                        .findFirst()
                        .orElse(ItemStack.EMPTY);

                if (!output.isEmpty()) {
                    ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(output.getItem());
                    int count = Math.max(1, output.getCount());
                    pendingCommand.set("craft " + outputId + " " + count);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}