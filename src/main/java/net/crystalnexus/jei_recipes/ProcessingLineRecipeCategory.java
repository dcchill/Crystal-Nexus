package net.crystalnexus.jei_recipes;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.ISlottedWidgetFactory;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.crystalnexus.block.ProcessingModuleBlock.Operation;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.crystalnexus.recipe.ProcessingLineRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

public final class ProcessingLineRecipeCategory implements IRecipeCategory<ProcessingLineRecipe> {
    private final IDrawable icon;

    public ProcessingLineRecipeCategory(IGuiHelper helper) {
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.PROCESSING_PRESS_MODULE.get()));
    }

    @Override public mezz.jei.api.recipe.RecipeType<ProcessingLineRecipe> getRecipeType() { return CrystalnexusModJeiPlugin.ProcessingLine_Type; }
    @Override public Component getTitle() { return Component.translatable("jei.crystalnexus.processing_line"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return ProcessingChainLayout.WIDTH; }
    @Override public int getHeight() { return ProcessingChainLayout.HEIGHT; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, ProcessingLineRecipe recipe, IFocusGroup focuses) {
        boolean[] materials = new boolean[recipe.steps().size()];
        for (int i = 0; i < materials.length; i++) materials[i] = !recipe.steps().get(i).ingredient().isEmpty();
        var layout = new ProcessingChainLayout(materials);
        ISlottedWidgetFactory<ProcessingLineRecipe> factory = (extras, displayedRecipe, slots) -> {
            var widget = new ProcessingChainWidget(layout, slots);
            extras.addSlottedWidget(widget, slots);
            extras.addGuiEventListener(widget);
        };

        builder.addSlotToWidget(RecipeIngredientRole.INPUT, factory).setPosition(1, ProcessingChainLayout.SLOT_Y)
            .setStandardSlotBackground().addIngredients(recipe.input())
            .addRichTooltipCallback((slot, tooltip) -> tooltip.add(text("input")));
        for (int i = 0; i < recipe.steps().size(); i++) {
            var step = recipe.steps().get(i);
            var position = layout.steps().get(i);
            builder.addSlotToWidget(RecipeIngredientRole.CATALYST, factory)
                .setPosition(position.x() + 1, ProcessingChainLayout.SLOT_Y).setStandardSlotBackground()
                .addItemStack(new ItemStack(module(step.operation())))
                .addRichTooltipCallback((slot, tooltip) -> {
                    tooltip.add(text("time", step.time(), String.format(Locale.ROOT, "%.1f", step.time() / 20.0)));
                    tooltip.add(text("energy", step.energyPerTick(), (long) step.time() * step.energyPerTick()));
                });
            if (position.hasMaterial()) builder.addSlotToWidget(RecipeIngredientRole.INPUT, factory)
                .setPosition(position.materialX() + 1, ProcessingChainLayout.SLOT_Y).setStandardSlotBackground()
                .addIngredients(step.ingredient())
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(text("material")));
        }
        builder.addSlotToWidget(RecipeIngredientRole.OUTPUT, factory)
            .setPosition(layout.outputX() + 1, ProcessingChainLayout.SLOT_Y).setStandardSlotBackground()
            .addItemStack(recipe.getResultItem(null))
            .addRichTooltipCallback((slot, tooltip) -> tooltip.add(text("output")));
    }

    private static Block module(Operation operation) {
        return switch (operation) {
            case PRESS -> CrystalnexusModBlocks.PROCESSING_PRESS_MODULE.get();
            case ENGRAVE -> CrystalnexusModBlocks.PROCESSING_ENGRAVER_MODULE.get();
            case DRILL -> CrystalnexusModBlocks.PROCESSING_DRILL_MODULE.get();
            case NANOFABRICATE -> CrystalnexusModBlocks.NANOFABRICATOR.get();
            case WELD -> CrystalnexusModBlocks.PROCESSING_WELDER_MODULE.get();
        };
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("jei.crystalnexus.processing_line." + key, args);
    }
}
