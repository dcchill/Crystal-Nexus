package net.crystalnexus.jei_recipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.Ingredient;

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

import java.util.ArrayList;
import java.util.List;

public class MatterTransmutationRecipeCategory
	implements IRecipeCategory<MatterTransmutationRecipe> {

	public static final ResourceLocation UID =
		ResourceLocation.parse("crystalnexus:matter_transmutation");

	public static final ResourceLocation TEXTURE =
		ResourceLocation.parse("crystalnexus:textures/screens/matter_transmutation_jei_color.png");

	private final IDrawable background;
	private final IDrawable icon;

	public MatterTransmutationRecipeCategory(IGuiHelper helper) {
		this.background = helper.createDrawable(TEXTURE, 0, 0, 173, 105);
		this.icon = helper.createDrawableIngredient(
			VanillaTypes.ITEM_STACK,
			new ItemStack(CrystalnexusModBlocks.MATTER_TRANSMUTATION_TABLE.get())
		);
	}

	@Override
	public mezz.jei.api.recipe.RecipeType<MatterTransmutationRecipe> getRecipeType() {
		return CrystalnexusModJeiPlugin.MatterTransmutation_Type;
	}

	@Override
	public Component getTitle() {
		return Component.literal("Matter Transmutation");
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public int getWidth() {
		return background.getWidth();
	}

	@Override
	public int getHeight() {
		return background.getHeight();
	}

	@Override
	public void draw(MatterTransmutationRecipe recipe,
	                 IRecipeSlotsView view,
	                 GuiGraphics gfx,
	                 double mouseX,
	                 double mouseY) {
		background.draw(gfx);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder,
	                      MatterTransmutationRecipe recipe,
	                      IFocusGroup focuses) {

		int[] xs = {79, 106, 115, 106, 79, 52, 43, 52};
		int[] ys = {11, 20, 47, 74, 83, 74, 47, 20};

		for (int i = 0; i < 8 && i < recipe.getIngredients().size(); i++) {
			Ingredient ing = recipe.getIngredients().get(i);
			List<ItemStack> stacks = toStacks(ing, recipe.getInputCount(i));

			builder.addSlot(RecipeIngredientRole.INPUT, xs[i], ys[i])
				.addItemStacks(stacks);
		}

		builder.addSlot(RecipeIngredientRole.OUTPUT, 79, 47)
			.addItemStack(recipe.getResultItem(null));
	}

	private static List<ItemStack> toStacks(Ingredient ing, int count) {
		ItemStack[] matches = ing.getItems();
		List<ItemStack> out = new ArrayList<>();

		for (ItemStack s : matches) {
			if (s.isEmpty()) continue;
			ItemStack copy = s.copy();
			copy.setCount(count);
			out.add(copy);
		}

		if (out.isEmpty()) {
			out.add(ItemStack.EMPTY);
		}

		return out;
	}
}
