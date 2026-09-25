package net.crystalnexus.block.entity;

import net.crystalnexus.energy.GeneratorEnergyStorage;
import net.crystalnexus.energy.SolarPanelOutput;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ChlorophyteSolarPanelBlockEntity extends BlockEntity {
    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage(10_000, SolarPanelOutput.DAYTIME_RATE, this::sync);

    public ChlorophyteSolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.CHLOROPHYTE_SOLAR_PANEL.get(), pos, state);
    }

    public void serverTick() {
        if (level != null) energy.generateEnergy(SolarPanelOutput.rate(level.isDay(), level.isRaining()), false);
    }

    public GeneratorEnergyStorage getEnergyStorage() { return energy; }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.get("energy") instanceof IntTag stored) energy.deserializeNBT(registries, stored);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energy.serializeNBT(registries));
    }

    private void sync() { setChanged(); }
}
