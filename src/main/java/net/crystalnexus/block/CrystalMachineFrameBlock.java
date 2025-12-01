package net.crystalnexus.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;

public class CrystalMachineFrameBlock extends Block {
	public CrystalMachineFrameBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.25f, 13f));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}
}