package net.crystalnexus.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class ToolEnergy {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 10_000;

    private ToolEnergy() {}

    public static boolean consume(Player player, ItemStack tool, int amount, boolean simulate) {
        if (player.getAbilities().instabuild || amount <= 0) return true;
        IEnergyStorage energy = tool.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy == null || energy.extractEnergy(amount, true) < amount) return false;
        return simulate || energy.extractEnergy(amount, false) == amount;
    }

    public static boolean isTool(ItemStack stack) {
        var item = stack.getItem();
        return item instanceof CompoundPickaxeItem || item instanceof CompoundSwordItem
            || item instanceof MiningLaserItem || item instanceof OreScannerItem
            || item instanceof GeigerCounterItem || item instanceof GravityGunItem
            || item instanceof BuildGunItem || item instanceof HoverPackItem
            || item instanceof LaserSaberItem || item instanceof FlorathaneWandItem
            || item instanceof StructureTrackerItem;
    }

    public static int barWidth(ItemStack stack) {
        return Math.round(13f * Math.max(0, Math.min(CAPACITY, BatteryData.getEnergy(stack))) / CAPACITY);
    }
}
