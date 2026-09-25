package net.crystalnexus.energy;

public final class SolarPanelOutput {
    public static final int DAYTIME_RATE = 128;
    public static final int RAIN_RATE = 64;

    private SolarPanelOutput() {}

    public static int rate(boolean daytime, boolean raining) {
        return daytime ? (raining ? RAIN_RATE : DAYTIME_RATE) : 0;
    }
}
