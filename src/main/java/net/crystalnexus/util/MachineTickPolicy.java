package net.crystalnexus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared five-tick, position-staggered polling policy for idle machines. */
public final class MachineTickPolicy {
    private static final int IDLE_INTERVAL = 5;

    private MachineTickPolicy() {}

    public static boolean shouldTick(Level level, BlockPos pos, BlockEntity machine) {
        if (machine.getPersistentData().getDouble("progress") > 0) return true;
        return Math.floorMod(level.getGameTime() + pos.asLong(), IDLE_INTERVAL) == 0;
    }

    public static boolean shouldTick(Level level, BlockPos pos, boolean active) {
        return active || Math.floorMod(level.getGameTime() + pos.asLong(), IDLE_INTERVAL) == 0;
    }
}
