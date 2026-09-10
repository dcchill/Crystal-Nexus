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
import net.minecraft.client.Minecraft;
import net.crystalnexus.processing.MachineTier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class RefiningRecipeCategory implements IRecipeCategory<RefiningRecipe> {
    public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:refining");
    private final IDrawable background;
    private final IDrawable icon;
    public RefiningRecipeCategory(IGuiHelper helper) {
        background = helper.createDrawable(ResourceLocation.parse("crystalnexus:textures/screens/refinery_jei.png"), 0, 0, 176, 80);
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.REFINERY.get()));
    }
    @Override public mezz.jei.api.recipe.RecipeType<RefiningRecipe> getRecipeType() { return CrystalnexusModJeiPlugin.Refining_Type; }
    @Override public Component getTitle() { return Component.literal("Refining"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return background.getWidth(); }
    @Override public int getHeight() { return background.getHeight(); }
    @Override public void draw(RefiningRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);
        graphics.drawString(Minecraft.getInstance().font,
            Component.literal("Minimum: " + MachineTier.forLevel(recipe.minimumMachineTier()).displayName()),
            5, 5, 0xff404040, false);
    }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, RefiningRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 52, 26).setFluidRenderer(recipe.input().amount(), false, 16, 34)
            .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.input().stack());
        recipe.itemInput().ifPresent(ingredient ->
            builder.addSlot(RecipeIngredientRole.INPUT, 52, 64).addIngredients(ingredient));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 115, 64).addItemStack(recipe.output());
    }
}
