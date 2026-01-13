package net.crystalnexus.block;

import net.crystalnexus.block.entity.DepotUploaderBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;

import org.jetbrains.annotations.Nullable;

public class DepotUploaderBlock extends Block implements EntityBlock {

    // ✅ No-arg constructor so DepotUploaderBlock::new works in your registry
    public DepotUploaderBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.0f, 6.0f)
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DepotUploaderBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // only tick our own BE type
        if (type != CrystalnexusModBlockEntities.DEPOT_UPLOADER.get()) return null;

        return (lvl, pos, st, be) -> {
            if (be instanceof DepotUploaderBlockEntity uploader) {
                DepotUploaderBlockEntity.tick(lvl, pos, st, uploader);
            }
        };
    }
}
