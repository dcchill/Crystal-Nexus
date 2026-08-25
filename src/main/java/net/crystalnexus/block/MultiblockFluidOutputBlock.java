package net.crystalnexus.block;

import net.crystalnexus.block.entity.MultiblockFluidOutputBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class MultiblockFluidOutputBlock extends Block implements EntityBlock {
	public MultiblockFluidOutputBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.2f, 13f).requiresCorrectToolForDrops());
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MultiblockFluidOutputBlockEntity(pos, state);
	}
}
