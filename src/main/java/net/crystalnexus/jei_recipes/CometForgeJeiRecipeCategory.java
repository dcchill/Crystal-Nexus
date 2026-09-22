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
import net.crystalnexus.init.*;
import net.crystalnexus.block.entity.CometForgeControllerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;

public final class CometForgeJeiRecipeCategory implements IRecipeCategory<CometForgeJeiRecipe> {
    public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:comet_forge");
    private final IDrawable icon;
    public CometForgeJeiRecipeCategory(IGuiHelper helper) {
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModItems.COMET_FORGE_CONTROLLER.get()));
    }
    @Override public mezz.jei.api.recipe.RecipeType<CometForgeJeiRecipe> getRecipeType() { return CrystalnexusModJeiPlugin.CometForge_Type; }
    @Override public Component getTitle() { return Component.translatable("block.crystalnexus.comet_forge_controller"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 94; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, CometForgeJeiRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < 3; i++) builder.addSlot(RecipeIngredientRole.INPUT, 8 + 20 * i, 8).addItemStacks(List.of(
            new ItemStack(CrystalnexusModItems.DIAMOND_SINGULARITY.get()),
            new ItemStack(CrystalnexusModItems.ENERGY_SINGULARITY.get()),
            new ItemStack(CrystalnexusModItems.EMERALD_SINGULARITY.get())));
        builder.addSlot(RecipeIngredientRole.INPUT, 28, 34).addItemStack(recipe.material());
        builder.addSlot(RecipeIngredientRole.INPUT, 78, 20).setFluidRenderer(CometForgeControllerBlockEntity.FLUID, false, 16, 32)
            .addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(CrystalnexusModFluids.TEMPORAL_ESSENCE.get(), CometForgeControllerBlockEntity.FLUID));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 140, 24).addItemStack(recipe.comet());
    }
    @Override public void draw(CometForgeJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, "Any mix of 3 singularities", 4, 57, 0xff404040, false);
        graphics.drawString(font, "500,000 FE / 25,000 mB / 10 s", 4, 70, 0xff404040, false);
        graphics.drawString(font, "1 full stack -> reusable comet", 4, 83, 0xff404040, false);
    }
}
