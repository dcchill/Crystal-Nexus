package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.util.DepotNetwork;
import net.crystalnexus.world.inventory.DepotCableConnectionMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public final class DepotCableBlockEntity extends BlockEntity implements MenuProvider {
    private final EnumMap<Direction, DepotCableConnectionConfig> connections = new EnumMap<>(Direction.class);
    private Direction openingSide = Direction.NORTH;

    public DepotCableBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.DEPOT_CABLE.get(), pos, state);
    }

    public Map<Direction, DepotCableConnectionConfig> connections() { return Map.copyOf(connections); }
    public @Nullable DepotCableConnectionConfig connection(Direction side) { return connections.get(side); }

    public boolean refreshConnections() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        boolean changed = false;
        for (Direction side : Direction.values()) {
            BlockPos target = worldPosition.relative(side);
            if (!serverLevel.hasChunkAt(target)) continue;
            boolean connected = DepotNetwork.isMachineConnection(serverLevel, worldPosition, side);
            if (connected && !connections.containsKey(side)) {
                connections.put(side, new DepotCableConnectionConfig(side));
                changed = true;
            } else if (!connected && connections.remove(side) != null) changed = true;
        }
        if (changed) changed();
        return changed;
    }

    public void setOpeningSide(Direction side) {
        openingSide = connections.containsKey(side) ? side
                : connections.keySet().stream().findFirst().orElse(Direction.NORTH);
    }

    public Direction openingSide() { return openingSide; }

    public void update(Direction side, int priority, DepotCableConnectionConfig.FilterMode mode,
            java.util.List<net.minecraft.world.item.ItemStack> filters) {
        DepotCableConnectionConfig config = connections.get(side);
        if (config == null) return;
        config.setPriority(priority);
        config.setFilterMode(mode);
        for (int i = 0; i < DepotCableConnectionConfig.FILTER_SLOTS; i++) {
            config.setFilter(i, i < filters.size() ? filters.get(i) : net.minecraft.world.item.ItemStack.EMPTY);
        }
        changed();
    }

    private void changed() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            DepotNetwork.invalidate(serverLevel);
        }
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) refreshConnections();
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        connections.clear();
        ListTag list = tag.getList("connections", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            DepotCableConnectionConfig config = DepotCableConnectionConfig.load(list.getCompound(i), registries);
            if (config != null) connections.put(config.side(), config);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        connections.values().forEach(config -> list.add(config.save(registries)));
        tag.put("connections", list);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithFullMetadata(registries);
    }

    @Override public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public Component getDisplayName() { return Component.literal("Depot Cable Connection"); }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DepotCableConnectionMenu(id, inventory, this);
    }
}
