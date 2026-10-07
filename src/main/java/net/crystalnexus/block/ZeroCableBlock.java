package net.crystalnexus.block;

import net.crystalnexus.block.entity.ZeroCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;

public class ZeroCableBlock extends EnergyCableMk2Block {
    public ZeroCableBlock(ResourceLocation id) { super(id); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZeroCableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
