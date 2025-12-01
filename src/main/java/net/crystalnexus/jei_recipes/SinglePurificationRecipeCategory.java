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

public class SinglePurificationRecipeCategory implements IRecipeCategory<SinglePurificationRecipe> {
	public final static ResourceLocation UID = ResourceLocation.parse("crystalnexus:single_purification");
	public final static ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/crystal_purifier_gui_jei.png");
	private final IDrawable background;
	private final IDrawable icon;

	public SinglePurificationRecipeCategory(IGuiHelper helper) {
		this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 80);
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.CRYSTAL_PURIFIER.get().asItem()));
	}

	@Override
	public mezz.jei.api.recipe.RecipeType<SinglePurificationRecipe> getRecipeType() {
		return CrystalnexusModJeiPlugin.SinglePurification_Type;
	}

	@Override
	public Component getTitle() {
		return Component.literal("Single Purification");
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
	public void draw(SinglePurificationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
		this.background.draw(guiGraphics);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SinglePurificationRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 61, 17).addIngredients(recipe.getIngredients().get(0));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 79, 53).addItemStack(recipe.getResultItem(null));
	}
}