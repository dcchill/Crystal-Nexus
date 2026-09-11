package net.crystalnexus.processing;

import net.minecraft.world.level.block.state.BlockState;

/** Shared progression and balance values for powered processing machines. */
public enum MachineAge {
    STARTER(0, 1.25, 0.50, "Starter"),
    AGE_1(1, 1.00, 1.00, "Age 1 (Crystal)"),
    AGE_2(2, 0.75, 2.00, "Age 2 (Titanium / Invertium)"),
    AGE_3(3, 0.50, 4.00, "Age 3 (Carbon Fiber / Titanium Carbide / Tungsten)"),
    AGE_4(4, 0.30, 8.00, "Age 4 (Solar / Hyper / Zero Point)"),
    ;

    private final int number;
    private final double processingTimeMultiplier;
    private final double energyMultiplier;
    private final String displayName;

    MachineAge(int number, double processingTimeMultiplier, double energyMultiplier, String displayName) {
        this.number = number;
        this.processingTimeMultiplier = processingTimeMultiplier;
        this.energyMultiplier = energyMultiplier;
        this.displayName = displayName;
    }

    public int number() { return number; }
    public double processingTimeMultiplier() { return processingTimeMultiplier; }
    public double energyMultiplier() { return energyMultiplier; }
    public String displayName() { return displayName; }
    public boolean supports(int requiredAge) { return number >= requiredAge; }
    public double processingTime(double baseTicks) { return Math.max(1, Math.ceil(baseTicks * processingTimeMultiplier)); }
    public int energyCost(int baseEnergy) { return Math.max(1, (int) Math.ceil(baseEnergy * energyMultiplier)); }
    public int minimumCapacity(int configuredCapacity, int baseEnergy) { return Math.max(configuredCapacity, energyCost(baseEnergy)); }

    public static MachineAge forNumber(int number) {
        for (MachineAge age : values()) if (age.number == number) return age;
        return AGE_1;
    }

    public static int requireNumber(int number) {
        if (number < AGE_1.number || number > AGE_4.number)
            throw new IllegalArgumentException("minimum_age must be between 1 and 4");
        return number;
    }

    public static MachineAge from(BlockState state) {
        return state.getBlock() instanceof AgedMachineBlock machine ? machine.machineAge() : STARTER;
    }
}
