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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public class DepotUploaderBlock extends Block implements EntityBlock {

    // ✅ No-arg constructor so DepotUploaderBlock::new works in your registry
    public DepotUploaderBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.0f, 6.0f)
                .noOcclusion()
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DepotUploaderBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof DepotUploaderBlockEntity uploader) {
            uploader.setOwner(player.getUUID());
        }
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
