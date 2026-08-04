package net.crystalnexus.init;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import net.crystalnexus.world.inventory.DepotMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

@EmiEntrypoint
public class CrystalnexusModEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(CrystalnexusModMenus.DEPOT.get(), new EmiRecipeHandler<DepotMenu>() {
            @Override
            public EmiPlayerInventory getInventory(AbstractContainerScreen<DepotMenu> screen) {
                return new EmiPlayerInventory(Minecraft.getInstance().player);
            }

            @Override
            public boolean supportsRecipe(EmiRecipe recipe) {
                return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && craftingRecipe(recipe) != null;
            }

            @Override
            public boolean canCraft(EmiRecipe recipe, EmiCraftContext<DepotMenu> context) {
                return context.getScreenHandler().hasCraftingUpgrade() && craftingRecipe(recipe) != null;
            }

            @Override
            public boolean craft(EmiRecipe recipe, EmiCraftContext<DepotMenu> context) {
                RecipeHolder<?> holder = craftingRecipe(recipe);
                if (holder == null || Minecraft.getInstance().gameMode == null) return false;
                Minecraft.getInstance().gameMode.handlePlaceRecipe(
                        context.getScreenHandler().containerId, holder, context.getAmount() > 1);
                return true;
            }
        });
    }

    private static RecipeHolder<?> craftingRecipe(EmiRecipe recipe) {
        RecipeHolder<?> holder = recipe.getBackingRecipe();
        if (holder == null && recipe.getId() != null && Minecraft.getInstance().level != null) {
            holder = Minecraft.getInstance().level.getRecipeManager().byKey(recipe.getId()).orElse(null);
        }
        return holder != null && holder.value() instanceof CraftingRecipe ? holder : null;
    }
}
