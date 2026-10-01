package net.crystalnexus.jei_recipes;

import java.util.List;
import java.util.Locale;
import net.crystalnexus.reactor.ReactorPlanner;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;

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
		return 300;
	}

	@Override
	public int getHeight() {
		return 226;
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, ReactorMultiblockGuideRecipe recipe, IFocusGroup focuses) {
        PlannerWidget widget = new PlannerWidget();
        builder.addWidget(widget);
        builder.addGuiEventListener(widget);
    }

    private static final class PlannerWidget implements IRecipeWidget, IJeiGuiEventListener {
        private final MultiblockStructurePreview preview = new MultiblockStructurePreview("reactor_guide", CrystalnexusModBlocks.REACTOR_COMPUTER.get());
        private final AbstractSliderButton[] sliders = new AbstractSliderButton[3];
        private int efficiency = 50, speed = 100, size = 5;
        private int selected = -1;
        private boolean dirty = true;
        private List<ReactorPlanner.Build> candidates = List.of();
        private ReactorPlanner.Estimate estimate;
        private List<ItemStack> materials = List.of();

        private PlannerWidget() {
            sliders[0] = slider(0, "Efficiency priority", 0, 100, efficiency);
            sliders[1] = slider(1, "Speed", 0, 100, speed);
            sliders[2] = slider(2, "Size", ReactorPlanner.MIN_SIZE, ReactorPlanner.MAX_SIZE, size);
        }

        private AbstractSliderButton slider(int index, String label, int min, int max, int initial) {
            return new AbstractSliderButton(140, index * 22, 158, 20,
                    Component.empty(), (initial - min) / (double) (max - min)) {
                { updateMessage(); }
                private int setting() { return min + (int) Math.round(value * (max - min)); }
                @Override protected void updateMessage() {
                    int setting = setting();
                    setMessage(Component.literal(label + ": " + (index == 2 ? setting + "x" + setting + "x" + setting : setting + "%")));
                }
                @Override protected void applyValue() {
                    int setting = setting();
                    if (index == 0) efficiency = setting;
                    if (index == 1) speed = setting;
                    if (index == 2 && size != setting) { size = setting; candidates = List.of(); }
                    dirty = true;
                }
            };
        }

        @Override public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }
        @Override public ScreenRectangle getArea() {
            return new ScreenRectangle(0, 0, 300, 226);
        }

        @Override public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) return;
            if (dirty) {
                if (candidates.isEmpty()) candidates = ReactorPlanner.candidates(size);
                estimate = ReactorPlanner.optimize(candidates, efficiency, speed);
                preview.setBlocks(estimate.build().blocks(), size);
                materials = preview.getRequiredBlocks(minecraft.level.registryAccess());
                dirty = false;
            }
            preview.render(graphics, minecraft.font, minecraft.level.registryAccess(), 0, 0, (int) mouseX, (int) mouseY);
            for (var slider : sliders) slider.render(graphics, (int) mouseX, (int) mouseY, 0);
            var layout = estimate.build().layout();
            String[] lines = {
                String.format(Locale.ROOT, "Efficiency: %.0f%%", layout.fuelEfficiency * 100),
                String.format(Locale.ROOT, "Output: %,d FE/t", estimate.fePerTick()),
                "Cooling: " + estimate.coolantDemand() + "/" + layout.coolantCapacityMbT + " mB/t",
                "Rod insertion: " + (100 - speed) + "%",
                "Cores: " + layout.fuelRods + " (" + layout.fuelColumns + " rods)",
                String.format(Locale.ROOT, "Fuel wear/t: %.3f", estimate.burnPerTick()),
                estimate.coolantDemand() > layout.coolantCapacityMbT ? "Cooling capacity limited" : "Cooling capacity sufficient"
            };
            for (int i = 0; i < lines.length; i++) graphics.drawString(minecraft.font, lines[i], 140, 72 + i * 11, 0x404040, false);
            graphics.drawString(minecraft.font, "Best candidate build - scroll preview to see layers", 4, 154, 0x404040, false);
            for (int i = 0; i < materials.size(); i++) {
                ItemStack stack = materials.get(i);
                int x = 4 + i * 26;
                graphics.renderItem(stack, x, 171);
                graphics.drawString(minecraft.font, Integer.toString(stack.getCount()), x, 190, 0x404040, false);
                if (mouseX >= x && mouseX < x + 24 && mouseY >= 171 && mouseY < 200)
                    graphics.renderTooltip(minecraft.font, Component.literal(stack.getCount() + " x ").append(stack.getHoverName()), (int) mouseX, (int) mouseY);
            }
            graphics.drawString(minecraft.font, "Estimate: full Blutonium cells, water, 700 C", 4, 205, 0x404040, false);
            graphics.drawString(minecraft.font, "No upgrades; supply all floor fluid inputs.", 4, 216, 0x404040, false);
            preview.renderHoverTooltip(graphics, minecraft.font, minecraft.level.registryAccess(), 0, 0, (int) mouseX, (int) mouseY);
        }

        @Override public boolean mouseClicked(double x, double y, int button) {
            for (int i = 0; i < sliders.length; i++) {
                if (sliders[i].mouseClicked(x, y, button)) {
                    if (selected >= 0) sliders[selected].setFocused(false);
                    selected = i;
                    sliders[i].setFocused(true);
                    return true;
                }
            }
            if (selected >= 0) sliders[selected].setFocused(false);
            selected = -1;
            var level = Minecraft.getInstance().level;
            return level != null && preview.mouseClicked(x, y, button, level.registryAccess(), 0, 0);
        }
        @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
            return selected >= 0 ? sliders[selected].mouseDragged(x, y, button, dx, dy) : preview.mouseDragged(x, y, button);
        }
        @Override public boolean mouseReleased(double x, double y, int button) {
            if (selected >= 0) { sliders[selected].mouseReleased(x, y, button); return true; }
            preview.mouseReleased(x, y, button);
            return button == 0;
        }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
            var level = Minecraft.getInstance().level;
            return level != null && dy != 0 && preview.mouseScrolled(x, y, dy, level.registryAccess(), 0, 0);
        }
        @Override public boolean keyPressed(double x, double y, int key, int scanCode, int modifiers) {
            return selected >= 0 && sliders[selected].keyPressed(key, scanCode, modifiers);
        }
    }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ReactorMultiblockGuideRecipe recipe, IFocusGroup focuses) {
		for (var ingredient : recipe.getIngredients())
			builder.addSlot(RecipeIngredientRole.INPUT, 1400, 1400).addIngredients(ingredient);
	}
}
