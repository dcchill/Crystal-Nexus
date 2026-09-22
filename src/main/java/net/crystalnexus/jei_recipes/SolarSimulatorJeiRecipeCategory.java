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
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class SolarSimulatorJeiRecipeCategory implements IRecipeCategory<SolarSimulatorJeiRecipe> {
    public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:solar_simulator");
    private final IDrawable background;
    private final IDrawable icon;

    public SolarSimulatorJeiRecipeCategory(IGuiHelper helper) {
        background = helper.createDrawable(ResourceLocation.parse("crystalnexus:textures/screens/solar_sim_jei.png"), 0, 0, 176, 92);
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
            new ItemStack(CrystalnexusModBlocks.SOLAR_SIMULATOR_CONTROLLER.get()));
    }

    @Override public mezz.jei.api.recipe.RecipeType<SolarSimulatorJeiRecipe> getRecipeType() {
        return CrystalnexusModJeiPlugin.SolarSimulator_Type;
    }
    @Override public Component getTitle() { return Component.literal("Solar Simulator"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return background.getWidth(); }
    @Override public int getHeight() { return background.getHeight(); }

    @Override public void draw(SolarSimulatorJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                               double mouseX, double mouseY) {
        background.draw(graphics);
        graphics.drawString(Minecraft.getInstance().font, "Star: " + (net.crystalnexus.block.entity.SolarSimulatorControllerBlockEntity.DURATION / 20.0) + " s / 200,000 FE x star", 4, 80, 0xffe8dcff, false);
        if (recipe.fluidOutput().isPresent())
            graphics.drawString(Minecraft.getInstance().font, "Fluid output requires a fluid port", 4, 4, 0xffe8dcff, false);
        else graphics.drawString(Minecraft.getInstance().font, net.crystalnexus.item.ResourceCometItem.material(recipe.planet()).isEmpty() ? "One random material per planet" : "Exact material; reusable comet", 4, 4, 0xffe8dcff, false);
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, SolarSimulatorJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 53, 37).addItemStacks(recipe.planetInput());
        builder.addSlot(RecipeIngredientRole.INPUT, 80, 37).addItemStacks(java.util.List.of(
            new ItemStack(CrystalnexusModItems.YELLOW_DWARF_STAR.get()), new ItemStack(CrystalnexusModItems.ORANGE_STAR.get()),
            new ItemStack(CrystalnexusModItems.BLUE_STAR.get()), new ItemStack(CrystalnexusModItems.PINK_STAR.get())));
        if (recipe.fluidOutput().isPresent()) builder.addSlot(RecipeIngredientRole.OUTPUT, 134, 37)
            .setFluidRenderer(recipe.fluidOutput().get().getAmount(), false, 16, 16)
            .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.fluidOutput().get());
        else builder.addSlot(RecipeIngredientRole.OUTPUT, 134, 37).addItemStacks(recipe.materialOutputs());
    }
}
