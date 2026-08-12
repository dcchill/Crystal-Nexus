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

public class FluidChemicalReactionRecipeCategory implements IRecipeCategory<FluidChemicalReactionRecipe> {
    public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:fluid_chemical_reaction");
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/fluid_chemical_reaction_chamber_jei.png");
    private final IDrawable background;
    private final IDrawable icon;

    public FluidChemicalReactionRecipeCategory(IGuiHelper helper) {
        background = helper.createDrawable(TEXTURE, 0, 0, 176, 80);
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
            new ItemStack(CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get()));
    }

    @Override public mezz.jei.api.recipe.RecipeType<FluidChemicalReactionRecipe> getRecipeType() {
        return CrystalnexusModJeiPlugin.FluidChemicalReaction_Type;
    }
    @Override public Component getTitle() { return Component.literal("Fluid Chemical Reaction"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return background.getWidth(); }
    @Override public int getHeight() { return background.getHeight(); }
    @Override public void draw(FluidChemicalReactionRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, FluidChemicalReactionRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < 2; i++) {
            int x = i == 0 ? 28 : 52;
            recipe.fluidInput(i).ifPresent(input -> builder.addSlot(RecipeIngredientRole.INPUT, x, 26)
                .setFluidRenderer(input.amount(), false, 16, 34)
                .addIngredient(NeoForgeTypes.FLUID_STACK, input.stack()));
            recipe.itemInput(i).ifPresent(input -> builder.addSlot(RecipeIngredientRole.INPUT, x, 64).addIngredients(input));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 115, 26)
            .setFluidRenderer(recipe.output().amount(), false, 16, 34)
            .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.output().stack());
    }
}
