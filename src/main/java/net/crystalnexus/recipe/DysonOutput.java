package net.crystalnexus.recipe;

/** Pure capacity and rate calculations for reusable Dyson construction supplies. */
public final class DysonOutput {
    private DysonOutput() { }

    public record Result(int carbonSheets, int solarSheets, int fePerTick) { }

    public static Result calculate(int structures, int carbon, int solar, int starMultiplier) {
        long capacity = 6L * Math.max(0, structures);
        int activeCarbon = (int) Math.min(capacity, Math.max(0, carbon));
        int activeSolar = (int) Math.min(Math.max(0, capacity - activeCarbon), Math.max(0, solar));
        long rate = ((long) activeSolar * 512 + (long) activeCarbon * 4096) * Math.max(0, starMultiplier);
        return new Result(activeCarbon, activeSolar, (int) Math.min(Integer.MAX_VALUE, rate));
    }
}
