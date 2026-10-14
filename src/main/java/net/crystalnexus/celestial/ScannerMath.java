package net.crystalnexus.celestial;

public final class ScannerMath {
    public record Parameters(int range, double precision, int identificationTicks) {}
    public static final Parameters BASE = new Parameters(10000, 1, 40);
    private ScannerMath() {}
    public static int angularError(int a, int b) {
        int delta = Math.floorMod(a - b + 180, 360) - 180;
        return Math.abs(delta);
    }
    public static double tolerance(int difficulty, boolean known, Parameters parameters) {
        return (7.0 - difficulty * 2) * parameters.precision() * (known ? 1.25 : 1);
    }
    public static int strength(int bearingError, int elevationError, int distance, int difficulty, Parameters parameters) {
        double error = Math.hypot(bearingError, elevationError);
        return (int) Math.clamp(100 - error * (0.5 + difficulty * 0.25) - 10.0 * distance / parameters.range(), 0, 100);
    }
    public static boolean aligned(int bearingError, int elevationError, int frequencyError, int difficulty, boolean known, Parameters parameters) {
        double tolerance = tolerance(difficulty, known, parameters);
        return bearingError <= tolerance && elevationError <= tolerance && frequencyError <= tolerance;
    }
    public static int identificationTicks(boolean known, Parameters parameters) {
        return known ? Math.max(1, parameters.identificationTicks() / 2) : parameters.identificationTicks();
    }
}
