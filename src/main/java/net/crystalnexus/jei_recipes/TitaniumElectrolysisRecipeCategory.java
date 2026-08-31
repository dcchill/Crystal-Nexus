package net.crystalnexus.jei_recipes;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class TitaniumElectrolysisRecipeCategory implements IRecipeCategory<TitaniumElectrolysisRecipe> {
    public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:titanium_electrolysis");
    private final IDrawable background;
    private final IDrawable icon;

    public TitaniumElectrolysisRecipeCategory(IGuiHelper helper) {
        background = helper.createDrawable(ResourceLocation.parse("crystalnexus:textures/screens/refinery_jei.png"), 0, 0, 176, 80);
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
            new ItemStack(CrystalnexusModBlocks.TITANIUM_ELECTROLYSIS_CELL.get()));
    }

    @Override public mezz.jei.api.recipe.RecipeType<TitaniumElectrolysisRecipe> getRecipeType() {
        return CrystalnexusModJeiPlugin.TitaniumElectrolysis_Type;
    }
    @Override public Component getTitle() { return Component.translatable("jei.crystalnexus.titanium_electrolysis"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return background.getWidth(); }
    @Override public int getHeight() { return background.getHeight(); }
    @Override public void draw(TitaniumElectrolysisRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) { background.draw(graphics); }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, TitaniumElectrolysisRecipe recipe, IFocusGroup focuses) {
        recipe.fluidInput().ifPresent(input -> builder.addSlot(RecipeIngredientRole.INPUT, 52, 26)
            .setFluidRenderer(input.amount(), false, 16, 34)
            .addIngredient(NeoForgeTypes.FLUID_STACK, input.stack()));
        recipe.itemInput().ifPresent(input -> builder.addSlot(RecipeIngredientRole.INPUT, 52, 64)
            .addItemStacks(java.util.Arrays.stream(input.getItems())
                .map(stack -> stack.copyWithCount(recipe.itemInputCount())).toList()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 115, 26)
            .setFluidRenderer(recipe.fluidOutput().amount(), false, 16, 34)
            .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.fluidOutput().stack());
    }
}
