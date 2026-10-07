package net.crystalnexus.processing;

import net.crystalnexus.item.GradientItemName;
import net.crystalnexus.item.GradientItemName.Palette;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;


public enum MachineTier {
    IRON(0, 1.25, 0.50, "Iron"),
    CRYSTAL(1, 1.00, 1.00, "Crystal"),
    CHLOROPHYTE(2, 0.75, 2.00, "Chlorophyte"),
    INVERTIUM(3, 0.50, 4.00, "Invertium"),
    TITANIUM(4, 0.40, 8.00, "Azurine"),
    CARBON(5, 0.35, 16.00, "Carbon"),
    TITANIUM_CARBIDE(6, 0.30, 32.00, "Ferrosteel"),
    TUNGSTEN(7, 0.25, 64.00, "Obsidrax"),
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
    public int itemSlotCapacity() { return 64 << Math.max(0, upgradeSlots() - 1); }
    public int upgradeSlots() {
        return switch (level) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            case 3, 4 -> 3;
            case 5, 6 -> 4;
            case 7 -> 5;
            default -> 5;
        };
    }
    public int displayNumber() { return level + 1; }
    public Palette primaryPalette() {
        return switch (this) {
            case IRON -> null;
            case CRYSTAL -> Palette.ANCIENT_CRYSTAL;
            case CHLOROPHYTE -> Palette.CHLOROPHYTE;
            case INVERTIUM -> Palette.INVERTIUM;
            case TITANIUM -> Palette.AZURINE;
            case CARBON -> Palette.CARBON_FIBER;
            case TITANIUM_CARBIDE -> Palette.FERROSTEEL;
            case TUNGSTEN -> Palette.OBSIDRAX;
            case HYPER -> Palette.METEORITE_ALLOY;
        };
    }
    public int displayColor() {
        Palette palette = primaryPalette();
        return palette == null ? 0xD0D0D0 : palette.midpoint();
    }
    public MutableComponent tierLabel() {
        MutableComponent label = Component.translatable("tooltip.crystalnexus.machine_tier", displayNumber());
        return this == HYPER ? GradientItemName.gradient(label, Palette.METEORITE_ALLOY)
            : label.withStyle(style -> style.withColor(displayColor()));
    }
    public double processingTimeMultiplier() { return processingTimeMultiplier; }
    public double energyMultiplier() { return energyMultiplier; }
    public String displayName() { return displayName; }
    public boolean supports(int requiredTier) { return level >= requiredTier; }
    public double processingTime(double baseTicks) { return Math.max(1, Math.ceil(baseTicks * processingTimeMultiplier)); }
    public int energyCost(int baseEnergy) { return Math.max(1, (int) Math.ceil(baseEnergy * energyMultiplier)); }
    public int energyInput(int inputRate) {
        return this == HYPER ? (int) Math.min(Integer.MAX_VALUE, (long) inputRate * 2) : inputRate;
    }
    public int minimumCapacity(int configuredCapacity, int baseEnergy) { return Math.max(configuredCapacity, energyCost(baseEnergy)); }

    public static MachineTier forLevel(int level) {
        for (MachineTier tier : values()) if (tier.level == level) return tier;
        return CRYSTAL;
    }

    public static MachineTier from(BlockState state) {
        return state.getBlock() instanceof TieredMachineBlock machine ? machine.machineTier() : CRYSTAL;
    }
}
