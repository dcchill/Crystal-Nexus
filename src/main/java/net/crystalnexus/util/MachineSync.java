package net.crystalnexus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Persists changes immediately, but batches routine client snapshots. Owned by one block entity. */
public final class MachineSync {
    private final BlockEntity machine;
    private boolean pending;
    private long lastSent = Long.MIN_VALUE;

    public MachineSync(BlockEntity machine) {
        this.machine = machine;
    }

    public void changed() {
        Level level = machine.getLevel();
        if (level == null || level.isClientSide()) return;
        machine.setChanged();
        pending = true;
    }

    public void setDouble(String key, double value) {
        if (machine.getPersistentData().getDouble(key) == value) return;
        machine.getPersistentData().putDouble(key, value);
        changed();
    }

    /** Call before processing/idle guards so the last change is delivered even after work stops. */
    public void tick() {
        Level level = machine.getLevel();
        if (level == null || level.isClientSide() || !takeUpdate(level.getGameTime(), machine.getBlockPos())) return;
        var state = machine.getBlockState();
        level.sendBlockUpdated(machine.getBlockPos(), state, state, 2);
    }

    boolean takeUpdate(long now, BlockPos pos) {
        if (!pending || now == lastSent || !isUpdateTick(now, pos)) return false;
        pending = false;
        lastSent = now;
        return true;
    }

    public static boolean isUpdateTick(long gameTime, BlockPos pos) {
        return Math.floorMod(gameTime + pos.asLong(), 5) == 0;
    }
}
