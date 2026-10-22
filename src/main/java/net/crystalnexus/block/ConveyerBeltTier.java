package net.crystalnexus.block;

public enum ConveyerBeltTier {
    BASIC(0, 2.0),
    TITANIUM(1, 1.0),
    METEORITE(2, 0.5);

    private final int index;
    private final double ticksPerMove;

    ConveyerBeltTier(int index, double ticksPerMove) {
        this.index = index;
        this.ticksPerMove = ticksPerMove;
    }

    public int index() {
        return index;
    }

    public double ticksPerMove() {
        return ticksPerMove;
    }

    public int batchSize() {
        return 16 << index;
    }

    public static ConveyerBeltTier fromIndex(int index) {
        return switch (index) {
            case 1 -> TITANIUM;
            case 2 -> METEORITE;
            default -> BASIC;
        };
    }
}
