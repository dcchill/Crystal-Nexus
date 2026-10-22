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

    public static double renderStartTime(int segment, double beltMoveTime, double incomingHeadTime) {
        return segment == 0 && incomingHeadTime != Long.MIN_VALUE ? incomingHeadTime : beltMoveTime;
    }

    public static float renderPosition(int segment, float progress, float distance) {
        return Math.max(0.0F, segment + 1 - distance * (1 - progress));
    }

    public static boolean shouldDropOffFront(boolean movedToNextBelt, boolean hasFrontInventory) {
        return !movedToNextBelt && !hasFrontInventory;
    }
}
