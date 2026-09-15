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
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.crystalnexus.item.PrisonCubeItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class GeneSplicingRecipeCategory implements IRecipeCategory<GeneSplicingRecipe> {
	public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:gene_splicing");
	private final IDrawable background;
	private final IDrawable icon;

	public GeneSplicingRecipeCategory(IGuiHelper helper) {
		background = helper.createDrawable(ResourceLocation.parse("crystalnexus:textures/screens/gene_splicer_jei.png"), 0, 0, 176, 85);
		icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.MASTICATOR.get()));
	}

	@Override public mezz.jei.api.recipe.RecipeType<GeneSplicingRecipe> getRecipeType() { return CrystalnexusModJeiPlugin.GeneSplicing_Type; }
	@Override public Component getTitle() { return Component.translatable("jei.crystalnexus.gene_splicing"); }
	@Override public IDrawable getIcon() { return icon; }
	@Override public int getWidth() { return background.getWidth(); }
	@Override public int getHeight() { return background.getHeight(); }

	@Override public void draw(GeneSplicingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		background.draw(graphics);
	}

	@Override public void setRecipe(IRecipeLayoutBuilder builder, GeneSplicingRecipe recipe, IFocusGroup focuses) {
		ItemStack fuel = recipe.fuel().getItems()[0].copy();
		fuel.setCount(recipe.fuelCount());
		builder.addSlot(RecipeIngredientRole.INPUT, 32, 34).addItemStack(fuel);
		ItemStack prisonCube = new ItemStack(CrystalnexusModItems.PRISON_CUBE.get());
		prisonCube.setCount(recipe.prisonCubeCount());
		PrisonCubeItem.setStoredEntityType(prisonCube, recipe.mob());
		builder.addSlot(RecipeIngredientRole.INPUT, 72, 34).addItemStack(prisonCube);
		builder.addSlot(RecipeIngredientRole.OUTPUT, 124, 34).addItemStack(recipe.getResultItem(null));
	}
}
