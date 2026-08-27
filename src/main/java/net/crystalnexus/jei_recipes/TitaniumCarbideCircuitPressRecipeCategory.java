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

import java.util.Arrays;

public final class TitaniumCarbideCircuitPressRecipeCategory implements IRecipeCategory<TitaniumCarbideCircuitPressRecipe> {
    public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:titanium_carbide_circuit_press");
    private final IDrawable background;
    private final IDrawable icon;

    public TitaniumCarbideCircuitPressRecipeCategory(IGuiHelper helper) {
        background = helper.createDrawable(ResourceLocation.parse("crystalnexus:textures/screens/circuit_press_gui_jei.png"), 0, 0, 176, 80);
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
            new ItemStack(CrystalnexusModBlocks.TITANIUM_CARBIDE_CIRCUIT_PRESS.get()));
    }

    @Override public mezz.jei.api.recipe.RecipeType<TitaniumCarbideCircuitPressRecipe> getRecipeType() { return CrystalnexusModJeiPlugin.TitaniumCarbideCircuitPress_Type; }
    @Override public Component getTitle() { return Component.literal("Titanium Carbide Circuit Press"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return background.getWidth(); }
    @Override public int getHeight() { return background.getHeight(); }
    @Override public void draw(TitaniumCarbideCircuitPressRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) { background.draw(graphics); }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, TitaniumCarbideCircuitPressRecipe recipe, IFocusGroup focuses) {
        recipe.itemInput(0).ifPresent(input -> builder.addSlot(RecipeIngredientRole.INPUT, 61, 17)
            .addItemStacks(Arrays.stream(input.getItems())
                .map(stack -> stack.copyWithCount(recipe.itemInputCount(0))).toList()));
        recipe.itemInput(1).ifPresent(input -> builder.addSlot(RecipeIngredientRole.INPUT, 97, 17)
            .addItemStacks(Arrays.stream(input.getItems())
                .map(stack -> stack.copyWithCount(recipe.itemInputCount(1))).toList()));
        recipe.fluidInput(0).ifPresent(input -> builder.addSlot(RecipeIngredientRole.INPUT, 115, 16)
            .setFluidRenderer(input.amount(), false, 16, 34)
            .addIngredient(NeoForgeTypes.FLUID_STACK, input.stack()));
        recipe.itemOutput().ifPresent(output -> builder.addSlot(RecipeIngredientRole.OUTPUT, 79, 53).addItemStack(output));
    }
}
