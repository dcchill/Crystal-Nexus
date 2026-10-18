package net.crystalnexus.world.inventory;

/** GUI geometry shared by the logical menu and its client renderer. No client classes here. */
public final class MachineGuiLayout {
    public static final int SLOT_SIZE = 18;
    public static final int HEADER_HEIGHT = 44;
    public static final int TANK_WIDTH = 16;
    public static final int TANK_HEIGHT = 34;
    public static final int TANK_Y = 24;
    public static final int TANK_SLOT_Y = 64;
    public static final int PURGE_Y = 9;
    public static final int PURGE_HEIGHT = 12;
    public static final int[] TWO_TANK_X = {48, 112};
    public static final int[] THREE_TANK_X = {32, 80, 128};

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
    public static Rect tank(int x) { return new Rect(x, TANK_Y, TANK_WIDTH, TANK_HEIGHT); }
    public static Rect assemblerMode(int mode) { return new Rect(57 + mode * 22, 59, 18, 18); }
    public static Rect energy() { return new Rect(-34, 0, 28, 48); }
    public static Rect progress(int width, int index, int count) {
        int barWidth = (width - 16 - (count - 1) * 6) / count;
        return new Rect(8 + index * (barWidth + 6), -12, barWidth, 8);
    }
    public static int centeredOrigin(int available, int minimum, int size) {
        return Math.max(2, (available - size) / 2) - minimum;
    }
    public static float fitScale(int viewportWidth, int viewportHeight, Rect bounds) {
        return Math.min(1f, Math.min((viewportWidth - 4f) / bounds.width(), (viewportHeight - 4f) / bounds.height()));
    }
}
