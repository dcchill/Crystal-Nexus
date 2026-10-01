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
import net.crystalnexus.world.inventory.CelestialGearForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class CelestialGearForgeJeiRecipeCategory implements IRecipeCategory<CelestialGearForgeRecipe> {
	public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("crystalnexus", "celestial_gear_forge");
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "textures/screens/gear_forge_jei.png");
	private final IDrawable icon;

	public CelestialGearForgeJeiRecipeCategory(IGuiHelper helper) {
		icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get()));
	}
	@Override public mezz.jei.api.recipe.RecipeType<CelestialGearForgeRecipe> getRecipeType() { return CrystalnexusModJeiPlugin.CelestialGearForge_Type; }
	@Override public Component getTitle() { return Component.translatable("block.crystalnexus.celestial_gear_forge"); }
	@Override public IDrawable getIcon() { return icon; }
	@Override public int getWidth() { return 176; }
	@Override public int getHeight() { return 92; }

	@Override public void setRecipe(IRecipeLayoutBuilder builder, CelestialGearForgeRecipe recipe, IFocusGroup focuses) {
		int[][] positions = CelestialGearForgeMenu.SLOT_POSITIONS;
		for (int index = 0; index < CelestialGearForgeRecipe.INPUT_COUNT; index++)
			builder.addSlot(RecipeIngredientRole.INPUT, positions[index][0], positions[index][1]).addIngredients(recipe.getIngredients().get(index));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 134, 37).addItemStack(recipe.output());
	}

	@Override public void draw(CelestialGearForgeRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		graphics.blit(TEXTURE, 0, 0, 0, 0, getWidth(), getHeight(), 256, 256);
		graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, Component.literal("200 ticks / 100 FE/t"), 60, 81, 0xffded5c4, false);
	}
}
