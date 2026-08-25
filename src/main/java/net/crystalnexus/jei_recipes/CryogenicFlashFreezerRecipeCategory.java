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

public final class CryogenicFlashFreezerRecipeCategory implements IRecipeCategory<FluidChemicalReactionRecipe> {
	public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:cryogenic_flash_freezer");
	private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/cryo_chamber_jei.png");
	private final IDrawable background;
	private final IDrawable icon;

	public CryogenicFlashFreezerRecipeCategory(IGuiHelper helper) {
		background = helper.createDrawable(TEXTURE, 0, 0, 176, 80);
		icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
			new ItemStack(CrystalnexusModBlocks.CRYOGENIC_FLASH_FREEZER_HATCH.get()));
	}

	@Override public mezz.jei.api.recipe.RecipeType<FluidChemicalReactionRecipe> getRecipeType() {
		return CrystalnexusModJeiPlugin.CryogenicFlashFreezer_Type;
	}
	@Override public Component getTitle() { return Component.literal("Cryogenic Flash Freezer"); }
	@Override public IDrawable getIcon() { return icon; }
	@Override public int getWidth() { return background.getWidth(); }
	@Override public int getHeight() { return background.getHeight(); }
	@Override public void draw(FluidChemicalReactionRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
			double mouseX, double mouseY) { background.draw(graphics); }

	@Override public void setRecipe(IRecipeLayoutBuilder builder, FluidChemicalReactionRecipe recipe, IFocusGroup focuses) {
		recipe.fluidInput(0).ifPresent(input -> builder.addSlot(RecipeIngredientRole.INPUT, 50, 16)
			.setFluidRenderer(input.amount(), false, 16, 34)
			.addIngredient(NeoForgeTypes.FLUID_STACK, input.stack()));
		recipe.itemInput(0).ifPresent(input -> builder.addSlot(RecipeIngredientRole.INPUT, 50, 54).addIngredients(input));
		recipe.fluidOutput().ifPresent(output -> builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 16)
			.setFluidRenderer(output.amount(), false, 16, 34)
			.addIngredient(NeoForgeTypes.FLUID_STACK, output.stack()));
		recipe.itemOutput().ifPresent(output -> builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 54).addItemStack(output));
	}
}
