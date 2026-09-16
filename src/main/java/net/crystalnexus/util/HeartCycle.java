package net.crystalnexus.util;

/** A formed heart attempts one beat every twenty server ticks; missed beats are never banked. */
public final class HeartCycle {
    public static final int PERIOD = 20;
    private int ticks;

    public boolean tick(boolean ready) {
        if (++ticks < PERIOD) return false;
        reset();
        return ready;
    }

    public void reset() { ticks = 0; }
}
