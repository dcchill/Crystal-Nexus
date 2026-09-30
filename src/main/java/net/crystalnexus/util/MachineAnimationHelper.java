package net.crystalnexus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;


public final class MachineAnimationHelper {
    private static final String IDLE_GRACE = "animationIdleGrace";

    private MachineAnimationHelper() {}

    public static boolean shouldIdle(LevelAccessor level, BlockPos pos, double progress) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return progress <= 0;
        return shouldIdle(blockEntity, level.getBlockState(pos), progress);
    }

    public static boolean shouldIdle(BlockEntity blockEntity, BlockState blockState, double progress) {
        var property = blockState.getBlock().getStateDefinition().getProperty("blockstate");
        if (!(property instanceof IntegerProperty stateProperty)) return true;
        int state = blockState.getValue(stateProperty);
        boolean grace = blockEntity.getPersistentData().getBoolean(IDLE_GRACE);
        MachineAnimationState.Decision decision = MachineAnimationState.decide(progress, state == 2 || state == 4, grace);
        if (decision.grace() != grace) {
            if (decision.grace()) blockEntity.getPersistentData().putBoolean(IDLE_GRACE, true);
            else blockEntity.getPersistentData().remove(IDLE_GRACE);
        }
        return decision.idle();
    }

}
