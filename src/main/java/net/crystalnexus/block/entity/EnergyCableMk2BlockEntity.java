package net.crystalnexus.block.entity;

import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A cable is an instantaneous routed link with no internal energy storage. */
public class EnergyCableMk2BlockEntity extends BlockEntity implements WorldlyContainer {
    private final int maxTransfer;
    private final IEnergyStorage energyLink = new NetworkEnergyStorage(null);
    private int automaticInputSides;
    private int automaticOutputSides;
    private int legacyEnergy;

    public EnergyCableMk2BlockEntity(BlockPos pos, BlockState state) {
        this(CrystalnexusModBlockEntities.ENERGY_CABLE_MK_2.get(), pos, state,
            CrystalnexusConfig.MACHINES.ENERGY_CABLE_MK2);
    }

    protected EnergyCableMk2BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        CrystalnexusConfig.EnergyValues values) {
        super(type, pos, state);
        maxTransfer = Math.min(values.maxReceive(), values.maxExtract());
    }

    public IEnergyStorage getEnergyStorage() {
        return energyLink;
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return side == null ? energyLink : new NetworkEnergyStorage(side);
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (legacyEnergy > 0 && moveLegacyEnergy()) return;

        for (Direction direction : Direction.values()) {
            if (!canPull(direction)) continue;
            BlockPos sourcePos = worldPosition.relative(direction);
            if (level.getBlockEntity(sourcePos) instanceof EnergyCableMk2BlockEntity) continue;
            IEnergyStorage source = energyAt(sourcePos, direction.getOpposite());
            if (source == null || !source.canExtract()) continue;
            int offered = source.extractEnergy(maxTransfer, true);
            if (offered > 0 && routeEnergy(sourcePos, direction, offered, false) > 0) return;
        }
    }

    private int routeEnergy(@Nullable BlockPos sourcePos, @Nullable Direction sourceSide,
                            int offered, boolean simulate) {
        for (Endpoint endpoint : endpoints(sourceSide)) {
            if (endpoint.pos.equals(sourcePos) || !endpoint.pipe.canPush(endpoint.side)) continue;
            int amount = Math.min(Math.min(offered, maxTransfer), endpoint.limit);
            int accepted = endpoint.storage.receiveEnergy(amount, true);
            if (accepted <= 0) continue;
            if (simulate) return accepted;
            int moved = accepted;
            if (sourcePos != null) {
                IEnergyStorage source = energyAt(sourcePos, sourceSide.getOpposite());
                if (source == null || !source.canExtract()) continue;
                moved = source.extractEnergy(accepted, false);
                if (moved <= 0) continue;
            }
            moved = endpoint.storage.receiveEnergy(moved, false);
            if (moved <= 0) continue;
            setAutomaticInput(sourceSide);
            endpoint.pipe.setAutomaticOutput(endpoint.side);
            return moved;
        }
        return 0;
    }

    private boolean moveLegacyEnergy() {
        for (Endpoint endpoint : endpoints(null)) {
            if (!endpoint.pipe.canPush(endpoint.side)) continue;
            int moved = endpoint.storage.receiveEnergy(
                Math.min(Math.min(legacyEnergy, maxTransfer), endpoint.limit), false);
            if (moved <= 0) continue;
            legacyEnergy -= moved;
            endpoint.pipe.setAutomaticOutput(endpoint.side);
            setChanged();
            return true;
        }
        return false;
    }

    private int receiveNetworkEnergy(int amount, boolean simulate, @Nullable Direction ingress) {
        if (amount <= 0 || ingress != null && !canPull(ingress)) return 0;
        return routeEnergy(null, ingress, amount, simulate);
    }

    private List<Endpoint> endpoints(@Nullable Direction excludedSide) {
        List<Endpoint> result = new ArrayList<>();
        if (level == null) return result;
        ArrayDeque<NetworkNode> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(new NetworkNode(worldPosition, maxTransfer));
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            NetworkNode node = queue.removeFirst();
            if (!(level.getBlockEntity(node.pos) instanceof EnergyCableMk2BlockEntity cable)) continue;
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = node.pos.relative(direction);
                if (level.getBlockEntity(neighborPos) instanceof EnergyCableMk2BlockEntity neighbor) {
                    if (visited.add(neighborPos)) {
                        queue.addLast(new NetworkNode(neighborPos,
                            Math.min(node.limit, neighbor.maxTransfer)));
                    }
                    continue;
                }
                if (node.pos.equals(worldPosition) && direction == excludedSide) continue;
                IEnergyStorage storage = cable.energyAt(neighborPos, direction.getOpposite());
                if (storage != null && storage.canReceive()) {
                    result.add(new Endpoint(neighborPos, direction, cable, storage, node.limit));
                }
            }
        }
        return result;
    }

    private @Nullable IEnergyStorage energyAt(BlockPos pos, @Nullable Direction preferredSide) {
        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, preferredSide);
        if (storage != null) return storage;
        storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (storage != null) return storage;
        for (Direction side : Direction.values()) {
            storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
            if (storage != null) return storage;
        }
        return null;
    }

    private boolean canPull(Direction direction) {
        return (automaticOutputSides & 1 << direction.ordinal()) == 0;
    }

    private boolean canPush(Direction direction) {
        return (automaticInputSides & 1 << direction.ordinal()) == 0;
    }

    private void setAutomaticInput(@Nullable Direction direction) {
        if (direction == null) return;
        int side = 1 << direction.ordinal();
        automaticInputSides |= side;
        automaticOutputSides &= ~side;
        setChanged();
    }

    private void setAutomaticOutput(Direction direction) {
        int side = 1 << direction.ordinal();
        automaticOutputSides |= side;
        automaticInputSides &= ~side;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        automaticInputSides = tag.getInt("automaticInputSides");
        automaticOutputSides = tag.getInt("automaticOutputSides");
        legacyEnergy = tag.getInt("legacyEnergy");
        if (legacyEnergy == 0 && tag.get("energyStorage") instanceof IntTag oldEnergy) {
            legacyEnergy = oldEnergy.getAsInt();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("automaticInputSides", automaticInputSides);
        tag.putInt("automaticOutputSides", automaticOutputSides);
        if (legacyEnergy > 0) tag.putInt("legacyEnergy", legacyEnergy);
    }

    private record NetworkNode(BlockPos pos, int limit) {}
    private record Endpoint(BlockPos pos, Direction side, EnergyCableMk2BlockEntity pipe,
                            IEnergyStorage storage, int limit) {}
    private final class NetworkEnergyStorage implements IEnergyStorage {
        private final Direction side;
        private NetworkEnergyStorage(@Nullable Direction side) { this.side = side; }
        @Override public int receiveEnergy(int amount, boolean simulate) {
            return receiveNetworkEnergy(amount, simulate, side);
        }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return 0; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    }

    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) { return false; }
    @Override public int getContainerSize() { return 0; }
    @Override public boolean isEmpty() { return true; }
    @Override public ItemStack getItem(int index) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int index, int count) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int index) { return ItemStack.EMPTY; }
    @Override public void setItem(int index, ItemStack stack) {}
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() {}
}
