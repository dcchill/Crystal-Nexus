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
import net.crystalnexus.recipe.CelestialGearForgeRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class CelestialGearForgeJeiRecipeCategory implements IRecipeCategory<CelestialGearForgeRecipe> {
	public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("crystalnexus", "celestial_gear_forge");
	private final IDrawable icon;

	public CelestialGearForgeJeiRecipeCategory(IGuiHelper helper) {
		icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get()));
	}
	@Override public mezz.jei.api.recipe.RecipeType<CelestialGearForgeRecipe> getRecipeType() { return CrystalnexusModJeiPlugin.CelestialGearForge_Type; }
	@Override public Component getTitle() { return Component.translatable("block.crystalnexus.celestial_gear_forge"); }
	@Override public IDrawable getIcon() { return icon; }
	@Override public int getWidth() { return 162; }
	@Override public int getHeight() { return 102; }

	@Override public void setRecipe(IRecipeLayoutBuilder builder, CelestialGearForgeRecipe recipe, IFocusGroup focuses) {
		int[][] positions = {{55, 0}, {27, 27}, {55, 27}, {83, 27}, {55, 55}};
		for (int index = 0; index < positions.length; index++)
			builder.addSlot(RecipeIngredientRole.INPUT, positions[index][0], positions[index][1]).addIngredients(recipe.getIngredients().get(index));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 131, 27).addItemStack(recipe.output());
	}

	@Override public void draw(CelestialGearForgeRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		graphics.fillGradient(0, 0, getWidth(), getHeight(), 0xff302d38, 0xff1b1921);
		graphics.fill(0, 0, getWidth(), 1, 0xff88734f);
		graphics.fill(0, getHeight() - 1, getWidth(), getHeight(), 0xff88734f);
		graphics.fill(0, 0, 1, getHeight(), 0xff88734f);
		graphics.fill(getWidth() - 1, 0, getWidth(), getHeight(), 0xff88734f);
		graphics.fill(110, 43, 126, 47, 0xffb89c64);
		graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, Component.literal("200 ticks"), 109, 76, 0xffded5c4, false);
	}
}
