package net.crystalnexus.block.entity;

import net.crystalnexus.block.DepotControllerBlock;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

import java.util.UUID;

public class DepotControllerBlockEntity extends BlockEntity {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int ENERGY_PER_TICK = 20;
    public static final int ENERGY_PER_COMPONENT = 2;

    private UUID owner;
    private boolean active;
    private final ControllerEnergyStorage energyStorage = new ControllerEnergyStorage();

    public DepotControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.DEPOT_CONTROLLER.get(), pos, state);
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, DepotControllerBlockEntity controller) {
        DepotSavedData depot = controller.owner == null ? null : DepotSavedData.get(level, controller.owner);
        if (depot != null) depot.setControllerIfAbsent(level, pos);
        int powerDraw = ENERGY_PER_TICK + DepotNetwork.poweredComponentCount(level, pos) * ENERGY_PER_COMPONENT;
        controller.active = depot != null && depot.isController(level, pos)
            && controller.energyStorage.consume(powerDraw);
        if (state.getValue(DepotControllerBlock.POWERED) != controller.active) {
            level.setBlock(pos, state.setValue(DepotControllerBlock.POWERED, controller.active), 3);
        }
    }

    public boolean isPowered() {
        // Network discovery calls this method, so it must not calculate its own
        // component draw here (that would recurse through the cable scan).
        return energyStorage.getEnergyStored() >= ENERGY_PER_TICK;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public int getPowerDraw() {
        if (!(level instanceof ServerLevel serverLevel)) return ENERGY_PER_TICK;
        return ENERGY_PER_TICK + DepotNetwork.poweredComponentCount(serverLevel, worldPosition) * ENERGY_PER_COMPONENT;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (tag.get("Energy") instanceof IntTag energy) energyStorage.deserializeNBT(provider, energy);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.put("Energy", energyStorage.serializeNBT(provider));
    }

    private final class ControllerEnergyStorage extends EnergyStorage {
        private ControllerEnergyStorage() {
            super(CAPACITY, MAX_RECEIVE, 0, 0);
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (!simulate && received > 0) setChanged();
            return received;
        }

        private boolean consume(int amount) {
            if (energy < amount) return false;
            energy -= amount;
            setChanged();
            return true;
        }
    }
}
