package net.crystalnexus.jei_recipes;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.crystalnexus.block.entity.NeutronFluxChamberHatchBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class NeutronFluxChamberJeiRecipeCategory implements IRecipeCategory<NeutronFluxChamberJeiRecipe> {
	public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:neutron_flux_chamber");
	private final IDrawable background;
	private final IDrawable icon;
	private final IDrawable slot;

	public NeutronFluxChamberJeiRecipeCategory(IGuiHelper helper) {
		background = helper.createDrawable(ResourceLocation.parse("crystalnexus:textures/screens/neutron_chamber_jei.png"), 0, 0, 176, 85);
		icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
				new ItemStack(CrystalnexusModBlocks.NEUTRON_FLUX_CHAMBER_HATCH.get()));
		slot = helper.getSlotDrawable();
	}

	@Override public RecipeType<NeutronFluxChamberJeiRecipe> getRecipeType() {
		return CrystalnexusModJeiPlugin.NeutronFluxChamber_Type;
	}
	@Override public Component getTitle() { return Component.translatable("jei.crystalnexus.neutron_flux_chamber"); }
	@Override public IDrawable getIcon() { return icon; }
	@Override public int getWidth() { return background.getWidth(); }
	@Override public int getHeight() { return background.getHeight(); }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, NeutronFluxChamberJeiRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 43, 35).addItemStack(recipe.core());
		builder.addSlot(RecipeIngredientRole.OUTPUT, 125, 35)
				.setBackground(slot, -1, -1)
				.setFluidRenderer(recipe.flux().getAmount(), false, 16, 16)
				.addIngredient(NeoForgeTypes.FLUID_STACK, recipe.flux());
	}

	@Override
	public void draw(NeutronFluxChamberJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		background.draw(graphics);
		var font = Minecraft.getInstance().font;
		graphics.drawString(font, Component.translatable("jei.crystalnexus.neutron_flux_chamber.core"), 8, 8, 0xFF404040, false);
		graphics.drawString(font, Component.translatable("jei.crystalnexus.neutron_flux_chamber.reusable"), 8, 20, 0xFF404040, false);
		// Arrow connects the supplied texture's core slot to the fluid output slot.
		graphics.fill(76, 42, 107, 44, 0xFF686868);
		for (int i = 0; i < 6; i++) graphics.fill(107 - i, 37 + i, 108 - i, 49 - i, 0xFF686868);
		graphics.drawString(font, Component.translatable("jei.crystalnexus.neutron_flux_chamber.rate",
				NeutronFluxChamberHatchBlockEntity.FLUX_PER_TICK), 8, 59, 0xFF404040, false);
		graphics.drawString(font, Component.translatable("jei.crystalnexus.neutron_flux_chamber.energy",
				NeutronFluxChamberHatchBlockEntity.ENERGY_PER_FLUX_MB), 8, 71, 0xFF404040, false);
	}
}