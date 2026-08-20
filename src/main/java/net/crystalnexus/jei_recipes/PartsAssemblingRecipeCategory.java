package net.crystalnexus.jei_recipes;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class PartsAssemblingRecipeCategory implements IRecipeCategory<PartsAssemblingRecipe> {
    public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:parts_assembling");
    private final IDrawable background;
    private final IDrawable icon;

    public PartsAssemblingRecipeCategory(IGuiHelper helper) {
        background = helper.createDrawable(ResourceLocation.parse("crystalnexus:textures/screens/iron_smelter_jei.png"), 0, 0, 176, 80);
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.PARTS_ASSEMBLER.get()));
    }

    @Override public mezz.jei.api.recipe.RecipeType<PartsAssemblingRecipe> getRecipeType() { return CrystalnexusModJeiPlugin.PartsAssembling_Type; }
    @Override public Component getTitle() { return Component.translatable("jei.crystalnexus.parts_assembling"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return background.getWidth(); }
    @Override public int getHeight() { return background.getHeight(); }

    @Override
    public void draw(PartsAssemblingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);
        graphics.drawString(Minecraft.getInstance().font,
            Component.translatable("gui.crystalnexus.parts_assembler.mode." + recipe.mode().serializedName()),
            75, 58, 0xff404040, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PartsAssemblingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 36).addIngredients(recipe.ingredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 36).addItemStack(recipe.getResultItem(null));
    }
}
