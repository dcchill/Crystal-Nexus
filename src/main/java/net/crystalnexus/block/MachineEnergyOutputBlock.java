package net.crystalnexus.block;

import net.crystalnexus.block.entity.MachineEnergyOutputBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class MachineEnergyOutputBlock extends Block implements EntityBlock {
	public MachineEnergyOutputBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops());
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MachineEnergyOutputBlockEntity(pos, state); }
	@Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
		super.onRemove(state, level, pos, next, moving);
		if (state.getBlock() != next.getBlock()) level.updateNeighborsAt(pos, next.getBlock());
	}
}
