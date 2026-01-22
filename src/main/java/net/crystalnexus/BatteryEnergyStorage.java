package net.crystalnexus.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class BatteryEnergyStorage implements IEnergyStorage {
    private final ItemStack stack;
    private final int capacity;
    private final int maxTransfer;

    public BatteryEnergyStorage(ItemStack stack, int capacity, int maxTransfer) {
        this.stack = stack;
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) return 0;

        int stored = BatteryData.getEnergy(stack);
        int received = Math.min(capacity - stored, Math.min(maxTransfer, maxReceive));

        if (!simulate && received > 0) {
            BatteryData.setEnergy(stack, stored + received, capacity);
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) return 0;

        int stored = BatteryData.getEnergy(stack);
        int extracted = Math.min(stored, Math.min(maxTransfer, maxExtract));

        if (!simulate && extracted > 0) {
            BatteryData.setEnergy(stack, stored - extracted, capacity);
        }
        return extracted;
    }

    @Override public int getEnergyStored() { return BatteryData.getEnergy(stack); }
    @Override public int getMaxEnergyStored() { return capacity; }
    @Override public boolean canExtract() { return true; }
    @Override public boolean canReceive() { return true; }
}
