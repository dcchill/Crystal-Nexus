package net.crystalnexus.block;

import net.crystalnexus.block.entity.CraftingUpgradeBlockEntity;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class CraftingUpgradeBlock extends Block implements EntityBlock {
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public CraftingUpgradeBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(3f, 20f).requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(CONNECTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraftingUpgradeBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide || type != CrystalnexusModBlockEntities.CRAFTING_UPGRADE.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> CraftingUpgradeBlockEntity.tick(
                (net.minecraft.server.level.ServerLevel) tickLevel, pos, tickState,
                (CraftingUpgradeBlockEntity) blockEntity);
    }
}
