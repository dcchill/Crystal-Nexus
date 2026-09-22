package net.crystalnexus.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;

public class ReactorCoreBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
	public ReactorCoreBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.75f, 18f).requiresCorrectToolForDrops());
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}

	@Override
	public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new net.crystalnexus.block.entity.ReactorCoreBlockEntity(pos, state);
	}

	@Override
	public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState next, boolean moving) {
		if (state.getBlock() != next.getBlock()) {
			if (level.getBlockEntity(pos) instanceof net.crystalnexus.block.entity.ReactorCoreBlockEntity core)
				net.minecraft.world.Containers.dropContents(level, pos, core);
			super.onRemove(state, level, pos, next, moving);
		}
	}
}
