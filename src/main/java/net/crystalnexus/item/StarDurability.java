package net.crystalnexus.item;

public final class StarDurability {
    public static final int YELLOW = 1 << 10;
    public static final int ORANGE = 1 << 11;
    public static final int BLUE = 1 << 12;
    public static final int PINK = 1 << 13;

    private StarDurability() {}

    public static boolean consumedBy(int stress, int roll) { return roll < stress; }
}
