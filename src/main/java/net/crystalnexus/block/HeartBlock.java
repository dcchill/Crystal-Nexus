package net.crystalnexus.block;

import net.crystalnexus.block.entity.HeartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class HeartBlock extends Block implements EntityBlock {
    public HeartBlock() { super(BlockBehaviour.Properties.of().sound(SoundType.MUD).strength(0.5F, 0.5F).noOcclusion()); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HeartBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != CrystalnexusModBlockEntities.HEART.get() ? null
            : (world, pos, blockState, entity) -> ((HeartBlockEntity) entity).serverTick();
    }
}
