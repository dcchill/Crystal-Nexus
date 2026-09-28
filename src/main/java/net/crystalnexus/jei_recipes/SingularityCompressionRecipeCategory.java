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

import java.util.List;
import java.util.ArrayList;

public class SingularityCompressionRecipeCategory implements IRecipeCategory<SingularityCompressionRecipe> {
	public final static ResourceLocation UID = ResourceLocation.parse("crystalnexus:singularity_compression");
	public final static ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/singularityjei.png");
	private final IDrawable background;
	private final IDrawable icon;

	public SingularityCompressionRecipeCategory(IGuiHelper helper) {
		this.background = helper.createDrawable(TEXTURE, 0, 0, 158, 74);
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.SINGULARITY_COMPRESSOR.get().asItem()));
	}

	@Override
	public mezz.jei.api.recipe.RecipeType<SingularityCompressionRecipe> getRecipeType() {
		return CrystalnexusModJeiPlugin.SingularityCompression_Type;
	}

	@Override
	public Component getTitle() {
		return Component.literal("Singularity Compression");
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
	public void draw(SingularityCompressionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
		this.background.draw(guiGraphics);
		guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
			String.format(java.util.Locale.ROOT, "%,d items", recipe.getInputCount(0)), 38, 43, 0xff404040, false);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SingularityCompressionRecipe recipe, IFocusGroup focuses) {
		List<ItemStack> stacks = new ArrayList<>();
		for (ItemStack item : recipe.getIngredients().get(0).getItems())
			stacks.add(item.copyWithCount(1));
		builder.addSlot(RecipeIngredientRole.INPUT, 38, 21).addItemStacks(stacks);
		builder.addSlot(RecipeIngredientRole.OUTPUT, 105, 21).addItemStack(recipe.getResultItem(null));
	}
}
