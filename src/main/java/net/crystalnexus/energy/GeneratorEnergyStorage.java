package net.crystalnexus.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

public class GeneratorEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;

    public GeneratorEnergyStorage(int capacity, int maxExtract, Runnable onChanged) {
        super(capacity, capacity, maxExtract, 0);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    public int generateEnergy(int amount, boolean simulate) {
        int received = Math.min(capacity - energy, Math.max(0, amount));
        if (!simulate && received > 0) {
            energy += received;
            onChanged.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (!simulate && extracted > 0) {
            onChanged.run();
        }
        return extracted;
    }
}
