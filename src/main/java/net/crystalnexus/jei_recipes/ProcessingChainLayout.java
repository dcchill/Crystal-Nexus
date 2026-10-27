package net.crystalnexus.jei_recipes;

import java.util.ArrayList;
import java.util.List;

/** Shared chain geometry for drawing, hit testing and horizontal scrolling. */
final class ProcessingChainLayout {
    static final int WIDTH = 240;
    static final int HEIGHT = 64;
    static final int LEFT = 8;
    static final int VIEW_WIDTH = WIDTH - LEFT * 2;
    static final int SLOT = 18;
    static final int SLOT_Y = 23;
    static final int ARROW_GAP = 20;
    static final int TRACK_Y = 50;
    static final int TRACK_HEIGHT = 6;
    static final int SCROLL_STEP = 40;

    record Step(int x, boolean hasMaterial) {
        int width() { return hasMaterial ? SLOT * 2 + 4 : SLOT; }
        int materialX() { return x + SLOT + 4; }
    }

    private final List<Step> steps;
    private final int outputX;
    private final int contentWidth;
    private int scroll;

    ProcessingChainLayout(boolean... materials) {
        var steps = new ArrayList<Step>();
        int x = SLOT + ARROW_GAP;
        for (boolean material : materials) {
            var step = new Step(x, material);
            steps.add(step);
            x += step.width() + ARROW_GAP;
        }
        this.steps = List.copyOf(steps);
        outputX = x;
        contentWidth = outputX + SLOT;
    }

    List<Step> steps() { return steps; }
    int outputX() { return outputX; }
    int contentWidth() { return contentWidth; }
    int scroll() { return scroll; }
    int maxScroll() { return Math.max(0, contentWidth - VIEW_WIDTH); }
    boolean scrollable() { return maxScroll() > 0; }
    int screenX(int contentX) { return LEFT + Math.max(0, (VIEW_WIDTH - contentWidth) / 2) + contentX - scroll; }
    boolean visible(int contentX) { return screenX(contentX) >= LEFT && screenX(contentX) + SLOT <= LEFT + VIEW_WIDTH; }
    boolean containsSlot(int contentX, double mouseX, double mouseY) {
        int x = screenX(contentX) + 1;
        return visible(contentX) && mouseX >= x && mouseX < x + 16 && mouseY >= SLOT_Y && mouseY < SLOT_Y + 16;
    }
    void scrollTo(int value) { scroll = Math.clamp(value, 0, maxScroll()); }
    void scrollBy(int amount) { scrollTo(scroll + amount); }
    int thumbWidth() { return Math.min(VIEW_WIDTH, Math.max(SLOT, VIEW_WIDTH * VIEW_WIDTH / contentWidth)); }
    int thumbLeft() {
        return LEFT + (scrollable() ? (int) Math.round((double) scroll / maxScroll() * (VIEW_WIDTH - thumbWidth())) : 0);
    }
    void moveThumb(double left) {
        if (scrollable()) scrollTo((int) Math.round((left - LEFT) / (VIEW_WIDTH - thumbWidth()) * maxScroll()));
    }
}
