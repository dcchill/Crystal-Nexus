package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class HeartBlockEntity extends BlockEntity {
    private BlockPos controller;
    private Direction facing = Direction.NORTH;
    private long lastBeat = Long.MIN_VALUE;
    public HeartBlockEntity(BlockPos pos, BlockState state) { super(CrystalnexusModBlockEntities.HEART.get(), pos, state); }
    public boolean isFormed() { return controller != null; }
    public Direction getFacing() { return facing; }
    public float beatAge(float partialTick) {
        if (level == null || lastBeat == Long.MIN_VALUE) return 8;
        return (float) (level.getGameTime() - lastBeat) + partialTick;
    }
    public void bindController(BlockPos pos, Direction direction) {
        if (pos.equals(controller) && direction == facing) return;
        controller = pos.immutable();
        facing = direction;
        sync();
    }
    public void unbindController(BlockPos pos) {
        if (!pos.equals(controller)) return;
        controller = null;
        facing = Direction.NORTH;
        lastBeat = Long.MIN_VALUE;
        sync();
    }
    public void beat() { lastBeat = level.getGameTime(); sync(); }
    public void serverTick() {
        if (controller != null && (!level.hasChunkAt(controller)
            || !(level.getBlockEntity(controller) instanceof EngineeredHeartBlockEntity heart)
            || !heart.isFormed())) unbindController(controller);
    }
    private void sync() {
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controller = tag.contains("controller") ? BlockPos.of(tag.getLong("controller")) : null;
        facing = Direction.from3DDataValue(tag.getInt("facing"));
        if (facing.getAxis().isVertical()) facing = Direction.NORTH;
        lastBeat = tag.contains("lastBeat") ? tag.getLong("lastBeat") : Long.MIN_VALUE;
    }
    // Binding and animation are client updates only; the controller reconstructs them after loading.
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (controller != null) tag.putLong("controller", controller.asLong());
        tag.putInt("facing", facing.get3DDataValue());
        tag.putLong("lastBeat", lastBeat);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
