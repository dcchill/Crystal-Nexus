package net.crystalnexus.block;

import net.crystalnexus.block.entity.MultiblockItemInputBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class MultiblockItemInputBlock extends Block implements EntityBlock {
    public MultiblockItemInputBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5F, 16F).requiresCorrectToolForDrops());
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockItemInputBlockEntity(pos, state);
    }

    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
            && level.getBlockEntity(pos) instanceof MultiblockItemInputBlockEntity input) serverPlayer.openMenu(input, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof MultiblockItemInputBlockEntity input) {
            Containers.dropContents(level, pos, input);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MultiblockItemInputBlockEntity input
            ? AbstractContainerMenu.getRedstoneSignalFromContainer(input) : 0;
    }
}
