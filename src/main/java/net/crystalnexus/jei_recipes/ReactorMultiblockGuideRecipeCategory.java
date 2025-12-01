package net.crystalnexus.jei_recipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.crystalnexus.init.CrystalnexusModBlocks;

import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.constants.VanillaTypes;

public class ReactorMultiblockGuideRecipeCategory implements IRecipeCategory<ReactorMultiblockGuideRecipe> {
	public final static ResourceLocation UID = ResourceLocation.parse("crystalnexus:reactor_multiblock_guide");
	public final static ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/reactorstructure_jei.png");
	private final IDrawable background;
	private final IDrawable icon;

	public ReactorMultiblockGuideRecipeCategory(IGuiHelper helper) {
		this.background = helper.createDrawable(TEXTURE, 0, 0, 200, 135);
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.REACTOR_COMPUTER.get().asItem()));
	}

	@Override
	public mezz.jei.api.recipe.RecipeType<ReactorMultiblockGuideRecipe> getRecipeType() {
		return CrystalnexusModJeiPlugin.ReactorMultiblockGuide_Type;
	}

	@Override
	public Component getTitle() {
		return Component.literal("Reactor Multiblock Guide");
	}

	@Override
	public IDrawable getIcon() {
		return this.icon;
	}

	@Override
	public int getWidth() {
		return this.background.getWidth();
	}

	@Override
	public int getHeight() {
		return this.background.getHeight();
	}

	@Override
	public void draw(ReactorMultiblockGuideRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
		this.background.draw(guiGraphics);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ReactorMultiblockGuideRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 1400, 1400).addIngredients(recipe.getIngredients().get(0));
		builder.addSlot(RecipeIngredientRole.INPUT, 1400, 1400).addIngredients(recipe.getIngredients().get(1));
		builder.addSlot(RecipeIngredientRole.INPUT, 1400, 1400).addIngredients(recipe.getIngredients().get(2));
		builder.addSlot(RecipeIngredientRole.INPUT, 1400, 1400).addIngredients(recipe.getIngredients().get(3));
		builder.addSlot(RecipeIngredientRole.INPUT, 1400, 1400).addIngredients(recipe.getIngredients().get(4));
	}
}