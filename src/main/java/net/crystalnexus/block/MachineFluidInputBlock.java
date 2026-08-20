package net.crystalnexus.block;

import net.crystalnexus.block.entity.MachineFluidInputBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class MachineFluidInputBlock extends Block implements EntityBlock {
    public MachineFluidInputBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.2F, 13F).requiresCorrectToolForDrops());
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFluidInputBlockEntity(pos, state);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (state.getBlock() != next.getBlock()) level.updateNeighbourForOutputSignal(pos, this);
    }
}
