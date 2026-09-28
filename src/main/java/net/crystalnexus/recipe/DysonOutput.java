package net.crystalnexus.recipe;


public final class DysonOutput {
    public static final int MAX_INTEGRITY = 1_000;
    public static final int CARBON_SHEETS_PER_STRUCTURE = 4;
    public static final int SOLAR_SHEETS_PER_STRUCTURE = 2;
    private DysonOutput() { }

    public record Result(int carbonSheets, int solarSheets, int fePerTick) { }

    public static Result calculate(int structures, int carbon, int solar, int starMultiplier) {
        return calculate(structures, carbon, solar, starMultiplier, MAX_INTEGRITY);
    }

    public static Result calculate(int structures, int carbon, int solar, int starMultiplier, int integrity) {
        long structureCount = Math.max(0, structures);
        int activeCarbon = (int) Math.min((long) CARBON_SHEETS_PER_STRUCTURE * structureCount, Math.max(0, carbon));
        int activeSolar = (int) Math.min((long) SOLAR_SHEETS_PER_STRUCTURE * structureCount, Math.max(0, solar));
        long rate = ((long) activeSolar * 512 + (long) activeCarbon * 4096) * Math.max(0, starMultiplier);
        rate = rate * Math.max(0, Math.min(MAX_INTEGRITY, integrity)) / MAX_INTEGRITY;
        return new Result(activeCarbon, activeSolar, (int) Math.min(Integer.MAX_VALUE, rate));
    }
}
