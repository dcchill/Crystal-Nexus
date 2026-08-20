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
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.crystalnexus.recipe.GravitationalArrayRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public final class GravitationalArrayRecipeCategory implements IRecipeCategory<GravitationalArrayRecipe> {
    public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:gravitational_array");
    private static final int[] INPUT_X = {53, 107, 80, 80};
    private static final int[] INPUT_Y = {39, 39, 11, 65};
    private final IDrawable background;
    private final IDrawable icon;

    public GravitationalArrayRecipeCategory(IGuiHelper helper) {
        background = helper.createDrawable(ResourceLocation.parse("crystalnexus:textures/screens/gravitational_array_jei.png"), 0, 0, 176, 92);
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
            new ItemStack(CrystalnexusModBlocks.GRAVITATIONAL_ARRAY_CONTROLLER.get()));
    }

    @Override public mezz.jei.api.recipe.RecipeType<GravitationalArrayRecipe> getRecipeType() {
        return CrystalnexusModJeiPlugin.GravitationalArray_Type;
    }
    @Override public Component getTitle() { return Component.literal("Gravitational Confinement Array"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return background.getWidth(); }
    @Override public int getHeight() { return background.getHeight(); }

    @Override public void draw(GravitationalArrayRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                               double mouseX, double mouseY) {
        background.draw(graphics);
        graphics.drawString(Minecraft.getInstance().font,
            Component.literal(String.format("%,d FE  |  %.1f s", recipe.energy(), recipe.duration() / 20.0F)),
            4, 81, 0xffe8dcff, false);
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, GravitationalArrayRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.inputs().size(); i++) {
            GravitationalArrayRecipe.ItemInput input = recipe.inputs().get(i);
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack match : input.ingredient().getItems()) {
                ItemStack copy = match.copy();
                copy.setCount(input.count());
                stacks.add(copy);
            }
            builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X[i], INPUT_Y[i]).addItemStacks(stacks);
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 7, 29)
            .setFluidRenderer(recipe.temporalFluid(), false, 16, 34)
            .addIngredient(NeoForgeTypes.FLUID_STACK,
                new FluidStack(CrystalnexusModFluids.TEMPORAL_ESSENCE.get(), recipe.temporalFluid()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 80, 39).addItemStack(recipe.output());
    }
}
