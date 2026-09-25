package net.crystalnexus.block;

import net.crystalnexus.block.entity.ChlorophyteSolarPanelBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class ChlorophyteSolarPanelBlock extends Block implements EntityBlock {
    public ChlorophyteSolarPanelBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(2.5F, 6.0F).requiresCorrectToolForDrops());
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChlorophyteSolarPanelBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        return level.isClientSide || type != CrystalnexusModBlockEntities.CHLOROPHYTE_SOLAR_PANEL.get() ? null
            : (tickLevel, pos, tickState, blockEntity) -> ((ChlorophyteSolarPanelBlockEntity) blockEntity).serverTick();
    }
}
