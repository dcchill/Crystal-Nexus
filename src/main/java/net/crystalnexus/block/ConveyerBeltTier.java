package net.crystalnexus.block;

public enum ConveyerBeltTier {
    BASIC(0, 8),
    TITANIUM(1, 4),
    METEORITE(2, 2);

    private final int index;
    private final int ticksPerMove;

    ConveyerBeltTier(int index, int ticksPerMove) {
        this.index = index;
        this.ticksPerMove = ticksPerMove;
    }

    public int index() {
        return index;
    }

    public int ticksPerMove() {
        return ticksPerMove;
    }

    public static ConveyerBeltTier fromIndex(int index) {
        return switch (index) {
            case 1 -> TITANIUM;
            case 2 -> METEORITE;
            default -> BASIC;
        };
    }
}
