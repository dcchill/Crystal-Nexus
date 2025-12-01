package net.crystalnexus.block;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;

public class AncientCrystalOreStoneBlock extends Block {
	public AncientCrystalOreStoneBlock() {
		super(BlockBehaviour.Properties.of().strength(4f, 6f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.PLING));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}
}