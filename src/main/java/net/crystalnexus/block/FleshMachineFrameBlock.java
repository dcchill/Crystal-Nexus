package net.crystalnexus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class FleshMachineFrameBlock extends Block {
	public static final IntegerProperty TAKEOVER_STAGE = IntegerProperty.create("takeover_stage", 0, FleshTakeover.COMPLETE_STAGE);
	public static final DirectionProperty TAKEOVER_FACE = BlockStateProperties.FACING;
	private static final int ANIMATION_TICKS = 8;

	public FleshMachineFrameBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.MUD).strength(0.8f, 2f));
		registerDefaultState(stateDefinition.any().setValue(TAKEOVER_STAGE, FleshTakeover.COMPLETE_STAGE).setValue(TAKEOVER_FACE, Direction.UP));
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(state, level, pos, oldState, moving);
		if (!level.isClientSide && state.getValue(TAKEOVER_STAGE) < FleshTakeover.COMPLETE_STAGE) {
			level.scheduleTick(pos, this, ANIMATION_TICKS);
		}
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int nextStage = FleshTakeover.nextStage(state.getValue(TAKEOVER_STAGE));
		level.setBlock(pos, state.setValue(TAKEOVER_STAGE, nextStage), Block.UPDATE_CLIENTS);
		if (nextStage < FleshTakeover.COMPLETE_STAGE) {
			level.scheduleTick(pos, this, ANIMATION_TICKS);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TAKEOVER_STAGE, TAKEOVER_FACE);
	}
}
