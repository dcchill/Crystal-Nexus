package net.crystalnexus.block;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class FleshBlock extends Block {
	private static final int SPREAD_INTERVAL_TICKS = 20;

	public FleshBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.MUD).strength(0.5f, 0.5f).randomTicks());
	}

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		spread(level, pos, random);
		level.scheduleTick(pos, this, SPREAD_INTERVAL_TICKS);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(state, level, pos, oldState, moving);
		if (!level.isClientSide) {
			level.scheduleTick(pos, this, SPREAD_INTERVAL_TICKS);
		}
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		spread(level, pos, random);
		level.scheduleTick(pos, this, SPREAD_INTERVAL_TICKS);
	}

	private static void spread(ServerLevel level, BlockPos pos, RandomSource random) {
		Direction[] directions = Direction.values();
		int start = random.nextInt(directions.length);
		for (int offset = 0; offset < directions.length; offset++) {
			BlockPos target = pos.relative(directions[(start + offset) % directions.length]);
			if (level.getBlockState(target).is(CrystalnexusModBlocks.MACHINE_FRAME.get())) {
				level.setBlock(target, CrystalnexusModBlocks.FLESH_MACHINE_FRAME.get().defaultBlockState()
						.setValue(FleshMachineFrameBlock.TAKEOVER_STAGE, 0)
						.setValue(FleshMachineFrameBlock.TAKEOVER_FACE, directions[(start + offset) % directions.length].getOpposite()), Block.UPDATE_ALL);
				return;
			}
		}
	}
}
