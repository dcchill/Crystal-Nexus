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

public class ExtractinatorJEIRecipeCategory implements IRecipeCategory<ExtractinatorJEIRecipe> {
	public final static ResourceLocation UID = ResourceLocation.parse("crystalnexus:extractination");
	public final static ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/extractionatorjei.png");
	private final IDrawable background;
	private final IDrawable icon;

	public ExtractinatorJEIRecipeCategory(IGuiHelper helper) {
		this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 80);
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.EXTRACTINATOR.get().asItem()));
	}

	@Override
	public mezz.jei.api.recipe.RecipeType<ExtractinatorJEIRecipe> getRecipeType() {
		return CrystalnexusModJeiPlugin.ExtractinatorJEI_Type;
	}

	@Override
	public Component getTitle() {
		return Component.literal("Extractination");
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
	public void draw(ExtractinatorJEIRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
		this.background.draw(guiGraphics);
	}

	@Override
public void setRecipe(IRecipeLayoutBuilder builder, ExtractinatorJEIRecipe recipe, IFocusGroup focuses) {
    builder.addSlot(RecipeIngredientRole.INPUT, 79, 17)
           .addIngredients(recipe.getIngredients().get(0));

    int[][] positions = {
        {61, 44}, {79, 44}, {97, 44},
        {61, 62}, {79, 62}, {97, 62}
    };

    var results = recipe.getResults();
    for (int i = 0; i < results.size() && i < positions.length; i++) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, positions[i][0], positions[i][1])
               .addItemStack(results.get(i));
    }
}

}