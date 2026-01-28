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

public class AcceleratorJeiRecipeCategory implements IRecipeCategory<AcceleratorJeiRecipe> {
	public final static ResourceLocation UID = ResourceLocation.parse("crystalnexus:accelerator_jei");
	public final static ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/separator_gui_jei.png");
	private final IDrawable background;
	private final IDrawable icon;

	public AcceleratorJeiRecipeCategory(IGuiHelper helper) {
		this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 85);
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.PARTICLE_ACCELERATOR_CONTROLLER.get().asItem()));
	}

	@Override
	public mezz.jei.api.recipe.RecipeType<AcceleratorJeiRecipe> getRecipeType() {
		return CrystalnexusModJeiPlugin.AcceleratorJei_Type;
	}

	@Override
	public Component getTitle() {
		return Component.literal("Inversion");
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
	public void draw(AcceleratorJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
		this.background.draw(guiGraphics);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, AcceleratorJeiRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 52, 34).addIngredients(recipe.getIngredients().get(0));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 115, 34).addItemStack(recipe.getResultItem(null));
	}
}