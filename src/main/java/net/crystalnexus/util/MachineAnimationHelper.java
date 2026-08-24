package net.crystalnexus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Prevents a completed operation from showing one idle frame before the next operation starts. */
public final class MachineAnimationHelper {
    private static final String IDLE_GRACE = "animationIdleGrace";

    private MachineAnimationHelper() {}

    public static boolean shouldIdle(LevelAccessor level, BlockPos pos, double progress) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return progress <= 0;
        var property = level.getBlockState(pos).getBlock().getStateDefinition().getProperty("blockstate");
        if (!(property instanceof IntegerProperty stateProperty)) return true;
        int state = level.getBlockState(pos).getValue(stateProperty);
        MachineAnimationState.Decision decision = MachineAnimationState.decide(progress, state == 2 || state == 4,
            blockEntity.getPersistentData().getBoolean(IDLE_GRACE));
        if (decision.grace()) blockEntity.getPersistentData().putBoolean(IDLE_GRACE, true);
        else blockEntity.getPersistentData().remove(IDLE_GRACE);
        return decision.idle();
    }

}
