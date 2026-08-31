package net.crystalnexus.block;

public final class ConveyerBeltMovement {
    private static final int SEGMENTS = 4;

    private ConveyerBeltMovement() {}

    public static boolean canAdvance(int occupiedSegments, int segment, boolean tailCanAdvance) {
        if (segment < 0 || segment >= SEGMENTS || (occupiedSegments & 1 << segment) == 0) return false;
        for (int next = segment + 1; next < SEGMENTS; next++) {
            if ((occupiedSegments & 1 << next) == 0) return true;
        }
        return tailCanAdvance;
    }

    public static long renderStartTime(int segment, long beltMoveTime, long incomingHeadTime) {
        return segment == 0 && incomingHeadTime != Long.MIN_VALUE ? incomingHeadTime : beltMoveTime;
    }

    public static boolean shouldDropOffFront(boolean movedToNextBelt, boolean hasFrontInventory) {
        return !movedToNextBelt && !hasFrontInventory;
    }
}
