package net.crystalnexus.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import java.util.List;

/** GUI geometry shared by the logical menu and its client renderer. No client classes here. */
public final class MachineGuiLayout {
    public static final int SLOT_SIZE = 18;
    public static final int INVENTORY_GAP = 18;
    public static final int HEADER_HEIGHT = 44;
    public static final int TANK_WIDTH = 16;
    public static final int TANK_HEIGHT = 34;
    public static final int TANK_Y = 30;
    public static final int TANK_SLOT_Y = 66;
    public static final int PURGE_Y = 11;
    public static final int PURGE_HEIGHT = 18;
    public static final int[] TWO_TANK_X = {44, 116};
    public static final int[] THREE_TANK_X = {26, 80, 134};
    public static final int TWO_TANK_SLOT_Y = 48;

    private MachineGuiLayout() {}

    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }
        public boolean intersects(Rect other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
        public Rect union(Rect other) {
            int left = Math.min(x, other.x), top = Math.min(y, other.y);
            return new Rect(left, top, Math.max(right(), other.right()) - left, Math.max(bottom(), other.bottom()) - top);
        }
    }

    public static Rect slot(int x, int y) { return new Rect(x - 1, y - 1, SLOT_SIZE, SLOT_SIZE); }
    /** Move the inventory by whole rows, preserving the work area's grid phase. Bottom includes control borders. */
    public static int inventoryY(int originalY, int workBottom) {
        int missing = workBottom + INVENTORY_GAP - (originalY - 1);
        return originalY + Math.max(0, (missing + SLOT_SIZE - 1) / SLOT_SIZE) * SLOT_SIZE;
    }
    public static Rect tank(int x) { return new Rect(x, TANK_Y, TANK_WIDTH, TANK_HEIGHT); }
    /** Compact two-tank work area, centred above the inventory with room for refinery purge buttons. */
    public static Rect twoTank(int index, boolean purge) {
        return new Rect(TWO_TANK_X[index], purge ? 30 : 12, TANK_WIDTH, purge ? 16 : TANK_HEIGHT);
    }
    public static Rect blueprintName() { return new Rect(79, 12, 90, 18); }
    public static Rect blueprintButton(boolean clear) { return new Rect(clear ? 115 : 79, 47, clear ? 54 : 36, 18); }
    /** Assembler rows: slots at y 12, one clear row, modes at y 47, one clear row above the inventory. */
    public static final int ASSEMBLER_SLOT_Y = 12;
    public static Rect assemblerMode(int mode) { return new Rect(61 + mode * 18, 47, 18, 18); }
    public static int grid(int value, int origin) { return origin + Math.round((value - origin) / 18f) * SLOT_SIZE; }
    public static int gridSize(int size) { return Math.max(SLOT_SIZE, Math.round(size / 18f) * SLOT_SIZE); }
    public static double fraction(double current, double maximum) {
        return Double.isFinite(current) && Double.isFinite(maximum) && maximum > 0 ? Math.clamp(current / maximum, 0, 1) : 0;
    }
    public record Grid(int x, int y) {
        public Rect control(int left, int top, int width, int height) {
            return new Rect(grid(left + 1, x) - 1, grid(top + 1, y) - 1, gridSize(width), gridSize(height));
        }
        public Rect tank(int left, int top, int width, int height) {
            return tank(left, top, width, height, y);
        }
        public Rect tank(int left, int top, int width, int height, int limit) {
            int h = gridSize(height + 2);
            int alignedY = grid(top, y);
            if (top < limit && alignedY + h > limit) alignedY -= ((alignedY + h - limit + 17) / 18) * 18;
            return new Rect(grid(left, x), alignedY, gridSize(width + 2) - 2, h - 2);
        }
    }
    public static Grid grid(Iterable<Slot> slots) {
        for (var slot : slots) if (slot.isActive() && slot.container instanceof Inventory && slot.getSlotIndex() == 9)
            return new Grid(slot.x, slot.y);
        return new Grid(8, 84);
    }
    public static Rect energy(int bottom) { return new Rect(-27, 0, 28, bottom); }
    public static Rect energy(Iterable<Slot> slots, int imageHeight) {
        int bottom = 0;
        for (var slot : slots) {
            if (slot.isActive() && slot.container instanceof Inventory
                    && slot.getSlotIndex() >= 9 && slot.getSlotIndex() < 36)
                bottom = Math.max(bottom, slot(slot.x, slot.y).bottom());
        }
        return energy(bottom > 0 ? bottom : imageHeight - 4);
    }
    /*
     * Side-panel design rules: the FE panel (left) and the upgrade bay (right) are mirror images. Both are
     * 28px wide, overlap the body border by one pixel, start at body y 0 and end at the bottom of the main
     * inventory rows. Upgrade slots sit centred in the bay with equal gaps above, between and below them.
     */
    public static final int SIDE_PANEL_WIDTH = 28;
    public static final int BODY_WIDTH = 176;
    public static final int UPGRADES_PER_COLUMN = 5;
    public static int sidePanelBottom(int inventoryY) { return slot(8, inventoryY + 36).bottom(); }
    /** Margin between the bay border and its slots; also the gap between stacked slots. */
    public static final int SIDE_MARGIN = (SIDE_PANEL_WIDTH - SLOT_SIZE) / 2;
    /**
     * Side-bay slot position (upgrades, catalysts, etc.). Slots stack from the top with even 5px gaps,
     * so the bay grows with the number of active slots instead of stretching to the inventory.
     */
    public static int[] upgradeSlot(int ordinal) {
        int column = ordinal / UPGRADES_PER_COLUMN, row = ordinal % UPGRADES_PER_COLUMN;
        int frameY = SIDE_MARGIN + row * (SLOT_SIZE + SIDE_MARGIN);
        int frameX = BODY_WIDTH - 1 + SIDE_MARGIN + column * (SLOT_SIZE + SIDE_MARGIN);
        return new int[]{frameX + 1, frameY + 1};
    }
    public static int sideX(int ordinal) { return upgradeSlot(ordinal)[0]; }
    public static int sideY(int ordinal) { return upgradeSlot(ordinal)[1]; }
    /** Bay framing exactly the active side slots, with the same margin on every edge. */
    public static Rect upgradeBay(Iterable<Slot> slots, int imageWidth, int imageHeight) {
        int left = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = 0;
        for (var slot : slots) if (slot.isActive() && !(slot.container instanceof Inventory) && slot.x >= imageWidth) {
            left = Math.min(left, slot.x - 1); right = Math.max(right, slot.x - 1 + SLOT_SIZE);
            bottom = Math.max(bottom, slot.y - 1 + SLOT_SIZE);
        }
        if (left == Integer.MAX_VALUE) return null;
        int x = Math.min(imageWidth - 1, left - SIDE_MARGIN);
        return new Rect(x, 0, right + SIDE_MARGIN - x, bottom + SIDE_MARGIN);
    }
    public static Rect progress(int width, int index, int count) {
        return new Rect(8, -12 + index * 14, width - 16, 8);
    }
    /** Processing machines have one header bar; stored resources are numeric readings. */
    public static List<String> readings(String menu) {
        return List.of("progress");
    }

    /** Matter Transmutation's original menu coordinates: inputs 0-7 clockwise from the top, output 8 centred. */
    private static final int[][] TRANSMUTATION_ORIGINAL = {
        {79, 11}, {106, 20}, {115, 47}, {106, 74}, {79, 83}, {52, 74}, {43, 47}, {52, 20}, {79, 47}};
    public static final int TRANSMUTATION_OUTPUT = 8;
    /** One whole row below the original 108 keeps the lower inputs clear of the player inventory. */
    public static final int TRANSMUTATION_INVENTORY_Y = 126;
    /** The octagon moves as one group so its centre output lands on the inventory grid. */
    public static int[] transmutationSlot(int index) {
        int[] centre = TRANSMUTATION_ORIGINAL[TRANSMUTATION_OUTPUT];
        int dx = grid(centre[0], 8) - centre[0], dy = grid(centre[1], TRANSMUTATION_INVENTORY_Y) - centre[1];
        return new int[]{TRANSMUTATION_ORIGINAL[index][0] + dx, TRANSMUTATION_ORIGINAL[index][1] + dy};
    }
    public static int[] transmutationOriginal(int index) { return TRANSMUTATION_ORIGINAL[index].clone(); }
    public static int centeredOrigin(int available, int minimum, int size) {
        return Math.max(2, (available - size) / 2) - minimum;
    }
    public static float fitScale(int viewportWidth, int viewportHeight, Rect bounds) {
        return Math.min(1f, Math.min((viewportWidth - 4f) / bounds.width(), (viewportHeight - 4f) / bounds.height()));
    }
}
