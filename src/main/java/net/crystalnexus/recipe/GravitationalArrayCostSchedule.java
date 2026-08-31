package net.crystalnexus.recipe;

public final class GravitationalArrayCostSchedule {
    private GravitationalArrayCostSchedule() {
    }

    public static long cumulative(long total, int completedTicks, int duration) {
        if (total < 0 || completedTicks < 0 || duration <= 0 || completedTicks > duration)
            throw new IllegalArgumentException("Invalid gravitational array cost schedule");
        return total / duration * completedTicks + total % duration * completedTicks / duration;
    }

    public static long next(long total, int completedTicks, int duration) {
        return cumulative(total, completedTicks + 1, duration) - cumulative(total, completedTicks, duration);
    }

    public static int maximumStep(long total, int duration) {
        return Math.toIntExact(next(total, duration - 1, duration));
    }
}
