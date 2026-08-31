package net.crystalnexus.processing;

import net.minecraft.world.level.block.state.BlockState;

/** Shared progression and balance values for ore-processing machines. */
public enum MachineTier {
    IRON(0, 1.25, 0.50, "Iron"),
    CRYSTAL(1, 1.00, 1.00, "Crystal"),
    CHLOROPHYTE(2, 0.75, 2.00, "Chlorophyte"),
    INVERTIUM_TITANIUM(3, 0.50, 4.00, "Titanium"),
    HYPER_CARBON(4, 0.35, 8.00, "Hyper/Carbon Fiber"),
    TITANIUM_CARBIDE(5, 0.30, 16.00, "Titanium Carbide"),
    TUNGSTEN(6, 0.25, 32.00, "Tungsten");

    private final int level;
    private final double processingTimeMultiplier;
    private final double energyMultiplier;
    private final String displayName;

    MachineTier(int level, double processingTimeMultiplier, double energyMultiplier, String displayName) {
        this.level = level;
        this.processingTimeMultiplier = processingTimeMultiplier;
        this.energyMultiplier = energyMultiplier;
        this.displayName = displayName;
    }

    public int level() { return level; }
    public double processingTimeMultiplier() { return processingTimeMultiplier; }
    public double energyMultiplier() { return energyMultiplier; }
    public String displayName() { return displayName; }
    public boolean supports(int requiredTier) { return level >= requiredTier; }
    public double processingTime(double baseTicks) { return Math.max(1, Math.ceil(baseTicks * processingTimeMultiplier)); }
    public int energyCost(int baseEnergy) { return Math.max(1, (int) Math.ceil(baseEnergy * energyMultiplier)); }
    public int minimumCapacity(int configuredCapacity, int baseEnergy) { return Math.max(configuredCapacity, energyCost(baseEnergy)); }

    public static MachineTier forLevel(int level) {
        for (MachineTier tier : values()) if (tier.level == level) return tier;
        return CRYSTAL;
    }

    public static MachineTier from(BlockState state) {
        return state.getBlock() instanceof TieredMachineBlock machine ? machine.machineTier() : CRYSTAL;
    }
}
