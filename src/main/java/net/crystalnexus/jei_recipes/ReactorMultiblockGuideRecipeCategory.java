package net.crystalnexus.jei_recipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.crystalnexus.client.gui.MultiblockStructurePreview;
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
	private final IDrawable icon;
	private final MultiblockStructurePreview preview = new MultiblockStructurePreview("reactor_guide", CrystalnexusModBlocks.REACTOR_COMPUTER.get());

	public ReactorMultiblockGuideRecipeCategory(IGuiHelper helper) {
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
		return 200;
	}

	@Override
	public int getHeight() {
		return 135;
	}

	@Override
	public void draw(ReactorMultiblockGuideRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
		Minecraft minecraft = Minecraft.getInstance();
		preview.render(guiGraphics, minecraft.font, minecraft.level.registryAccess(), 0, 0, (int) mouseX, (int) mouseY);
		guiGraphics.drawString(minecraft.font, "5x5x5 example", 140, 9, 0x404040, false);
		guiGraphics.drawString(minecraft.font, "Core + coolant", 140, 25, 0x404040, false);
		guiGraphics.drawString(minecraft.font, "moderator + reflector", 140, 37, 0x404040, false);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ReactorMultiblockGuideRecipe recipe, IFocusGroup focuses) {
		for (var ingredient : recipe.getIngredients())
			builder.addSlot(RecipeIngredientRole.INPUT, 1400, 1400).addIngredients(ingredient);
	}
}
