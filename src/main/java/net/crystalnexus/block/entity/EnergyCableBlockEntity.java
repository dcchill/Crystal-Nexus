package net.crystalnexus.block.entity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import net.crystalnexus.init.CrystalnexusModBlockEntities;

public class EnergyCableBlockEntity extends BlockEntity implements WorldlyContainer {

    // Tune these
    private static final int BUFFER_CAPACITY = 100000;
    private static final int MAX_IO_PER_TICK = 5000;

    // MCreator-style EnergyStorage + proper update notifications
    private final EnergyStorage energyStorage = new EnergyStorage(
        BUFFER_CAPACITY,
        MAX_IO_PER_TICK,
        MAX_IO_PER_TICK,
        0
    ) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int ret = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && ret > 0) markEnergyChanged();
            return ret;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int ret = super.extractEnergy(maxExtract, simulate);
            if (!simulate && ret > 0) markEnergyChanged();
            return ret;
        }
    };

    private void markEnergyChanged() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.ENERGY_CABLE.get(), pos, state);
    }

    // MCreator registers this capability in the locked file
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // -----------------------
    // Capability lookup helpers
    // -----------------------

    /** For YOUR blocks: null-side FIRST (matches your working TestProcedure). */
    private @Nullable IEnergyStorage getEnergyNullFirst(ILevelExtension ext, BlockPos pos, @Nullable Direction sided) {
        IEnergyStorage s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (s != null) return s;

        if (sided != null) {
            s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, sided);
            if (s != null) return s;
        }

        for (Direction d : Direction.values()) {
            s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, d);
            if (s != null) return s;
        }
        return null;
    }

    /** For OTHER mods: sided FIRST (some mods care about face). */
    private @Nullable IEnergyStorage getEnergySideFirst(ILevelExtension ext, BlockPos pos, @Nullable Direction sided) {
        IEnergyStorage s = null;

        if (sided != null) {
            s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, sided);
            if (s != null) return s;
        }

        s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (s != null) return s;

        for (Direction d : Direction.values()) {
            s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, d);
            if (s != null) return s;
        }
        return null;
    }

    private boolean isCable(BlockEntity be) {
        return be instanceof EnergyCableBlockEntity;
    }

    /**
     * Server tick:
     * 1) Pull into our buffer (side-first, good for generators / modded outputs)
     * 2) Push out of our buffer (null-first, good for your MCreator machines)
     * 3) Allow cable->cable propagation without ping-pong (deterministic direction rule)
     */
public void serverTick() {
    Level lvl = level;
    if (lvl == null || lvl.isClientSide) return;
    if (!(lvl instanceof ILevelExtension ext)) return;

    // ----------------------------
    // 1) PULL into this cable
    //    ONLY from "sources": canExtract && !canReceive
    // ----------------------------
    int space = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
    if (space > 0) {
        for (Direction dir : Direction.values()) {
            if (space <= 0) break;

            BlockPos nPos = worldPosition.relative(dir);

            // sided-first for other mods, then null for your stuff
            IEnergyStorage src = ext.getCapability(Capabilities.EnergyStorage.BLOCK, nPos, dir.getOpposite());
            if (src == null) src = ext.getCapability(Capabilities.EnergyStorage.BLOCK, nPos, null);
            if (src == null) continue;

            // IMPORTANT: don't drain machines/batteries; only drain true sources
            if (!src.canExtract() || src.canReceive()) continue;

            int want = Math.min(MAX_IO_PER_TICK, space);
            int pulledSim = src.extractEnergy(want, true);
            if (pulledSim <= 0) continue;

            int accepted = energyStorage.receiveEnergy(pulledSim, false);
            if (accepted > 0) {
                src.extractEnergy(accepted, false);
                space -= accepted;
            }
        }
    }

    // Nothing to do if empty
    if (energyStorage.getEnergyStored() <= 0) return;

    // ----------------------------
    // 2) PUSH to MACHINES FIRST (non-cable neighbors)
    //    Use YOUR proven null-capability pattern
    // ----------------------------
    for (Direction dir : Direction.values()) {
        if (energyStorage.getEnergyStored() <= 0) break;

        BlockPos nPos = worldPosition.relative(dir);
        BlockEntity be = lvl.getBlockEntity(nPos);

        // Skip other cables in this phase
        if (be instanceof EnergyCableBlockEntity) continue;

        IEnergyStorage sink = ext.getCapability(Capabilities.EnergyStorage.BLOCK, nPos, null);
        if (sink == null || !sink.canReceive()) continue;

        int offer = Math.min(MAX_IO_PER_TICK, energyStorage.getEnergyStored());
        int acceptedSim = sink.receiveEnergy(offer, true);
        if (acceptedSim <= 0) continue;

        int extracted = energyStorage.extractEnergy(acceptedSim, false);
        if (extracted > 0) {
            sink.receiveEnergy(extracted, false);
        }
    }

    // ----------------------------
    // 3) EQUALIZE with NEIGHBOR CABLES (propagate down lines)
    // ----------------------------
    for (Direction dir : Direction.values()) {
        int stored = energyStorage.getEnergyStored();
        if (stored <= 1) break;

        BlockPos nPos = worldPosition.relative(dir);
        BlockEntity be = lvl.getBlockEntity(nPos);
        if (!(be instanceof EnergyCableBlockEntity otherCable)) continue;

        IEnergyStorage other = otherCable.getEnergyStorage();

        int otherStored = other.getEnergyStored();
        if (otherStored >= stored) continue;

        int diff = stored - otherStored;
        int move = Math.min(MAX_IO_PER_TICK, diff / 2);
        if (move <= 0) continue;

        int otherSpace = other.getMaxEnergyStored() - otherStored;
        move = Math.min(move, otherSpace);
        if (move <= 0) continue;

        int extracted = energyStorage.extractEnergy(move, false);
        if (extracted > 0) {
            other.receiveEnergy(extracted, false);
        }
    }
}



    // ---- NBT (MATCHES MCreator STYLE) ----

    @Override
    protected void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
        super.loadAdditional(compound, lookupProvider);
        if (compound.get("energyStorage") instanceof IntTag intTag) {
            energyStorage.deserializeNBT(lookupProvider, intTag);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
        super.saveAdditional(compound, lookupProvider);
        compound.put("energyStorage", energyStorage.serializeNBT(lookupProvider));
    }

    // ---- WorldlyContainer (empty inventory) ----
    // This exists ONLY to satisfy MCreator's auto ItemHandler registration.

    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int i, ItemStack s, Direction d) { return false; }
    @Override public boolean canTakeItemThroughFace(int i, ItemStack s, Direction d) { return false; }
    @Override public int getContainerSize() { return 0; }
    @Override public boolean isEmpty() { return true; }
    @Override public ItemStack getItem(int i) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int i, int c) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int i) { return ItemStack.EMPTY; }
    @Override public void setItem(int i, ItemStack s) {}
    @Override public boolean stillValid(Player p) { return true; }
    @Override public void clearContent() {}
}
