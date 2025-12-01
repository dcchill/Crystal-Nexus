package net.crystalnexus.block;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.PushReaction;

public class RubberBlockBlock extends Block {
    public RubberBlockBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.SLIME_BLOCK) // more fitting sound
                .strength(0.75f, 8.5f)
                .noOcclusion());
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 15;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        // Prevent fall damage
        entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (!entity.isSuppressingBounce()) {
            double bounceStrength = 0.9; // adjust bounce height (1.0 = equal to fall)
            entity.setDeltaMovement(
                    entity.getDeltaMovement().x,
                    -entity.getDeltaMovement().y * bounceStrength,
                    entity.getDeltaMovement().z
            );
        } else {
            super.updateEntityAfterFallOn(level, entity);
        }
    }
    @Override
	public PushReaction getPistonPushReaction(BlockState state) {
    // Allow piston to move it normally
    return PushReaction.NORMAL;
	}
	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
    super.onPlace(state, level, pos, oldState, isMoving);

    if (isMoving) { // means piston pushed it
        var entities = level.getEntities(null, new net.minecraft.world.phys.AABB(pos).inflate(0.5));
        for (Entity entity : entities) {
            // Push entities slightly upwards when the rubber block moves
            if (!entity.isPassenger() && !entity.isVehicle() && entity.isPushable()) {
                entity.push(0, 0.5, 0); // tweak this value
            }
        }
    }
}


}
