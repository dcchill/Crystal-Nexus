package net.crystalnexus.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class BatteryEnergyStorage implements IEnergyStorage {
    private final ItemStack stack;
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;

    public BatteryEnergyStorage(ItemStack stack, int capacity, int maxTransfer) {
        this(stack, capacity, maxTransfer, maxTransfer);
    }

    public BatteryEnergyStorage(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
        this.stack = stack;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) return 0;

        int stored = BatteryData.getEnergy(stack);
        int received = Math.min(capacity - stored, Math.min(this.maxReceive, maxReceive));

        if (!simulate && received > 0) {
            BatteryData.setEnergy(stack, stored + received, capacity);
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) return 0;

        int stored = BatteryData.getEnergy(stack);
        int extracted = Math.min(stored, Math.min(this.maxExtract, maxExtract));

        if (!simulate && extracted > 0) {
            BatteryData.setEnergy(stack, stored - extracted, capacity);
        }
        return extracted;
    }

    @Override public int getEnergyStored() { return Math.min(BatteryData.getEnergy(stack), capacity); }
    @Override public int getMaxEnergyStored() { return capacity; }
    @Override public boolean canExtract() { return true; }
    @Override public boolean canReceive() { return true; }
}
