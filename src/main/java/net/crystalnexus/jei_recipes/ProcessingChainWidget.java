package net.crystalnexus.jei_recipes;

import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

import static net.crystalnexus.jei_recipes.ProcessingChainLayout.*;

final class ProcessingChainWidget implements ISlottedRecipeWidget, IJeiGuiEventListener {
    private record Slot(IRecipeSlotDrawable drawable, int contentX) {}
    private final ProcessingChainLayout layout;
    private final List<Slot> slots;
    private double dragOffset = -1;

    ProcessingChainWidget(ProcessingChainLayout layout, List<IRecipeSlotDrawable> slots) {
        this.layout = layout;
        this.slots = slots.stream().map(slot -> new Slot(slot, slot.getRect().getX() - 1)).toList();
        updateSlots();
    }

    @Override public ScreenPosition getPosition() { return new ScreenPosition(0, 0); }
    @Override public ScreenRectangle getArea() { return new ScreenRectangle(0, 0, WIDTH, HEIGHT); }

    private void updateSlots() {
        for (Slot slot : slots) slot.drawable().setPosition(layout.screenX(slot.contentX()) + 1, SLOT_Y);
    }

    @Override public void draw(GuiGraphics graphics, double mouseX, double mouseY) {
        updateSlots();
        arrow(graphics, layout.screenX(SLOT), SLOT_Y + 7);
        for (var step : layout.steps()) {
            int x = layout.screenX(step.x());
            clippedFill(graphics, x - 3, SLOT_Y - 4, x + step.width() + 3, SLOT_Y + 21, 0xffb4b4b4);
            clippedFill(graphics, x - 2, SLOT_Y - 3, x + step.width() + 2, SLOT_Y + 20, 0xffeeeeee);
            arrow(graphics, x + step.width(), SLOT_Y + 7);
        }
        for (Slot slot : slots) if (layout.visible(slot.contentX())) slot.drawable().draw(graphics);
        if (layout.scrollable()) {
            graphics.fill(LEFT, TRACK_Y, LEFT + VIEW_WIDTH, TRACK_Y + TRACK_HEIGHT, 0xffd4d4d4);
            int thumb = layout.thumbLeft();
            boolean hovered = mouseX >= thumb && mouseX < thumb + layout.thumbWidth()
                && mouseY >= TRACK_Y && mouseY < TRACK_Y + TRACK_HEIGHT;
            graphics.fill(thumb, TRACK_Y, thumb + layout.thumbWidth(), TRACK_Y + TRACK_HEIGHT,
                hovered || dragOffset >= 0 ? 0xff707070 : 0xff949494);
        }
    }

    private static void arrow(GuiGraphics graphics, int x, int y) {
        clippedFill(graphics, x + 4, y, x + 13, y + 2, 0xff606060);
        for (int i = 0; i < 4; i++) clippedFill(graphics, x + 12 + i, y - 3 + i, x + 13 + i, y + 5 - i, 0xff606060);
    }

    private static void clippedFill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int left = Math.max(LEFT, x1), right = Math.min(LEFT + VIEW_WIDTH, x2);
        if (left < right) graphics.fill(left, y1, right, y2, color);
    }

    @Override public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
        updateSlots();
        return slots.stream().filter(slot -> layout.containsSlot(slot.contentX(), mouseX, mouseY))
            .findFirst().map(slot -> new RecipeSlotUnderMouse(slot.drawable(), getPosition()));
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!hovered(mouseX, mouseY) || !layout.scrollable()) return false;
        layout.scrollBy((int) Math.round((scrollX != 0 ? scrollX : -scrollY) * SCROLL_STEP));
        updateSlots();
        return true;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !layout.scrollable() || mouseX < LEFT || mouseX >= LEFT + VIEW_WIDTH
                || mouseY < TRACK_Y || mouseY >= TRACK_Y + TRACK_HEIGHT) return false;
        int thumb = layout.thumbLeft();
        if (mouseX >= thumb && mouseX < thumb + layout.thumbWidth()) dragOffset = mouseX - thumb;
        else {
            dragOffset = layout.thumbWidth() / 2.0;
            layout.moveThumb(mouseX - dragOffset);
            updateSlots();
        }
        return true;
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || dragOffset < 0) return false;
        layout.moveThumb(mouseX - dragOffset);
        updateSlots();
        return true;
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0 || dragOffset < 0) return false;
        dragOffset = -1;
        return true;
    }

    @Override public boolean keyPressed(double mouseX, double mouseY, int keyCode, int scanCode, int modifiers) {
        if (!hovered(mouseX, mouseY) || !layout.scrollable()) return false;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> layout.scrollBy(-SCROLL_STEP);
            case GLFW.GLFW_KEY_RIGHT -> layout.scrollBy(SCROLL_STEP);
            case GLFW.GLFW_KEY_HOME -> layout.scrollTo(0);
            case GLFW.GLFW_KEY_END -> layout.scrollTo(layout.maxScroll());
            default -> { return false; }
        }
        updateSlots();
        return true;
    }

    private static boolean hovered(double x, double y) { return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT; }
}
