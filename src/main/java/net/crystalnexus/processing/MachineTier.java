package net.crystalnexus.processing;

import net.minecraft.world.level.block.state.BlockState;

/** Shared progression and balance values for ore-processing machines. */
public enum MachineTier {
    CRYSTAL(1, 1.00, 1.00, "Crystal"),
    CHLOROPHYTE(2, 0.75, 0.80, "Chlorophyte"),
    INVERTIUM_TITANIUM(3, 0.50, 0.60, "Titanium"),
    HYPER_CARBON(4, 0.30, 0.40, "Hyper/Carbon Fiber");

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

    public static MachineTier from(BlockState state) {
        return state.getBlock() instanceof TieredMachineBlock machine ? machine.machineTier() : CRYSTAL;
    }
}
