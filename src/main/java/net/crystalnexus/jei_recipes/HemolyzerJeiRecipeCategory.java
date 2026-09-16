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
import net.crystalnexus.block.entity.HemolyzerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class HemolyzerJeiRecipeCategory implements IRecipeCategory<HemolyzerRecipe> {
    public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:hemolyzer");
    private final IDrawable background;
    private final IDrawable icon;

    public HemolyzerJeiRecipeCategory(IGuiHelper helper) {
        background = helper.createDrawable(ResourceLocation.parse("crystalnexus:textures/screens/hemolyzer_jei.png"), 0, 0, 176, 85);
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.HEMOLYZER.get()));
    }

    @Override public mezz.jei.api.recipe.RecipeType<HemolyzerRecipe> getRecipeType() { return CrystalnexusModJeiPlugin.Hemolyzer_Type; }
    @Override public Component getTitle() { return Component.translatable("jei.crystalnexus.hemolyzer"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return background.getWidth(); }
    @Override public int getHeight() { return background.getHeight(); }
    @Override public void draw(HemolyzerRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);
        graphics.drawString(Minecraft.getInstance().font, HemolyzerBlockEntity.FE_PER_ITEM + " FE", 18, 64, 0xffFFFFFF, false);
    }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, HemolyzerRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 80, 35).addIngredients(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 134, 26).setFluidRenderer(recipe.blood().amount(), false, 15, 33)
            .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.blood().stack());
    }
}
