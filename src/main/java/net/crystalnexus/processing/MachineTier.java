package net.crystalnexus.processing;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;

/** Shared progression and balance values for ore-processing machines. */
public enum MachineTier {
    IRON(0, 1.25, 0.50, "Iron"),
    CRYSTAL(1, 1.00, 1.00, "Crystal"),
    CHLOROPHYTE(2, 0.75, 2.00, "Chlorophyte"),
    INVERTIUM(3, 0.50, 4.00, "Invertium"),
    TITANIUM(4, 0.40, 8.00, "Titanium"),
    CARBON(5, 0.35, 16.00, "Carbon"),
    TITANIUM_CARBIDE(6, 0.30, 32.00, "Titanium Carbide"),
    TUNGSTEN(7, 0.25, 64.00, "Tungsten"),
    HYPER(8, 0.20, 128.00, "Hyper");

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
    public int displayNumber() { return level + 1; }
    public int displayColor() {
        return switch (this) {
            case IRON -> 0xD0D0D0;
            case CRYSTAL -> 0x55FFFF;
            case CHLOROPHYTE -> 0x55FF55;
            case INVERTIUM -> 0xFF55FF;
            case TITANIUM -> 0x80AAFF;
            case CARBON -> 0x9999AA;
            case TITANIUM_CARBIDE -> 0xAA88FF;
            case TUNGSTEN -> 0xFFAA55;
            case HYPER -> 0xFF5555;
        };
    }
    public MutableComponent tierLabel() {
        return Component.translatable("tooltip.crystalnexus.machine_tier", displayNumber())
            .withStyle(style -> style.withColor(displayColor()));
    }
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
