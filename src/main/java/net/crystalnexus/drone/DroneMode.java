package net.crystalnexus.drone;

public enum DroneMode {
    TRANSPORT, MINING, BUILDER, COURIER, FLUID_TRANSPORT, COMBAT;

    public DroneMode next() { return values()[(ordinal() + 1) % values().length]; }
    public static DroneMode read(String name) {
        try { return valueOf(name); } catch (IllegalArgumentException e) { return TRANSPORT; }
    }
}
