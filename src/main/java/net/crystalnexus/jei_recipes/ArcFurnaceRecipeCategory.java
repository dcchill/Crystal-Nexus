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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ArcFurnaceRecipeCategory implements IRecipeCategory<ArcFurnaceRecipe> {
	public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:arc_furnace");
	private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/arc_smelter_jei.png");
	private final IDrawable background;
	private final IDrawable icon;

	public ArcFurnaceRecipeCategory(IGuiHelper helper) {
		background = helper.createDrawable(TEXTURE, 0, 0, 176, 83);
		icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.ARC_FURNACE.get()));
	}

	@Override public mezz.jei.api.recipe.RecipeType<ArcFurnaceRecipe> getRecipeType() { return CrystalnexusModJeiPlugin.ArcFurnace_Type; }
	@Override public Component getTitle() { return Component.literal("Arc Blast Furnace Alloying"); }
	@Override public IDrawable getIcon() { return icon; }
	@Override public int getWidth() { return background.getWidth(); }
	@Override public int getHeight() { return background.getHeight(); }
	@Override public void draw(ArcFurnaceRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) { background.draw(graphics); }
	@Override public void setRecipe(IRecipeLayoutBuilder builder, ArcFurnaceRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 44, 35).addIngredients(recipe.getIngredients().get(0));
		if (recipe.getIngredients().size() > 1)
			builder.addSlot(RecipeIngredientRole.INPUT, 26, 35).addIngredients(recipe.getIngredients().get(1));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 115, 35).addItemStack(recipe.getResultItem(null));
	}
}
