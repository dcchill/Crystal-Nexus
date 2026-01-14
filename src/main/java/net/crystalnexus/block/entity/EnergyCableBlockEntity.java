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

    private static final int BUFFER_CAPACITY = 100000;
    private static final int MAX_IO_PER_TICK = 5000;

    // Optional: set to true temporarily while debugging
    private static final boolean DEBUG_TICK_PROOF = false;

    private final EnergyStorage energyStorage = new EnergyStorage(
        BUFFER_CAPACITY, MAX_IO_PER_TICK, MAX_IO_PER_TICK, 0
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

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // ----------------------------
    // Capability lookup:
    // Mekanism prefers SIDED. Your blocks prefer NULL.
    // We try BOTH in a stable order:
    //   1) sided (neighbor face looking back)
    //   2) null (your blocks)
    //   3) any side fallback
    // ----------------------------
    private @Nullable IEnergyStorage getEnergyCompat(ILevelExtension ext, BlockPos pos, @Nullable Direction preferredSide) {
        if (preferredSide != null) {
            IEnergyStorage s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, preferredSide);
            if (s != null) return s;
        }

        IEnergyStorage sNull = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (sNull != null) return sNull;

        for (Direction d : Direction.values()) {
            IEnergyStorage s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, d);
            if (s != null) return s;
        }

        return null;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (!(level instanceof ILevelExtension ext)) return;

        // TICK PROOF (optional): gives the cable 1 FE once to prove ticking + enable pushing
        if (DEBUG_TICK_PROOF && energyStorage.getEnergyStored() == 0) {
            energyStorage.receiveEnergy(1, false);
        }

        // ----------------------------
        // PUSH: cable -> receivers
        // ----------------------------
        if (energyStorage.getEnergyStored() > 0) {
            for (Direction dir : Direction.values()) {
                if (energyStorage.getEnergyStored() <= 0) break;

                BlockPos nPos = worldPosition.relative(dir);

                // Neighbor should expose energy on the face that points back to the cable
                IEnergyStorage recv = getEnergyCompat(ext, nPos, dir.getOpposite());
                if (recv == null || !recv.canReceive()) continue;

                int offer = Math.min(MAX_IO_PER_TICK, energyStorage.getEnergyStored());
                int acceptedSim = recv.receiveEnergy(offer, true);
                if (acceptedSim > 0) {
                    int extracted = energyStorage.extractEnergy(acceptedSim, false);
                    if (extracted > 0) {
                        recv.receiveEnergy(extracted, false);
                    }
                }
            }
        }

        // ----------------------------
        // PULL: extractors -> cable
        // ----------------------------
        int space = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        if (space <= 0) return;

        for (Direction dir : Direction.values()) {
            if (space <= 0) break;

            BlockPos nPos = worldPosition.relative(dir);

            IEnergyStorage src = getEnergyCompat(ext, nPos, dir.getOpposite());
            if (src == null || !src.canExtract()) continue;

            int want = Math.min(MAX_IO_PER_TICK, space);
            int pulledSim = src.extractEnergy(want, true);

            if (pulledSim > 0) {
                int accepted = energyStorage.receiveEnergy(pulledSim, false);
                if (accepted > 0) {
                    src.extractEnergy(accepted, false);
                    space -= accepted;
                }
            }
        }
    }

    // ---- NBT (MCreator-compatible) ----
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

    // ---- Empty inventory (MCreator workaround) ----
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
