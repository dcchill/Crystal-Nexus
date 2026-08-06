package net.crystalnexus.block;

import net.crystalnexus.block.entity.CraftingCoreBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/** A cabled 1x1-to-2x2 multiblock that adds Depot crafting throughput. */
public class CraftingCoreBlock extends Block implements EntityBlock {
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public CraftingCoreBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(4f, 20f).requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(CONNECTED, false).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED, ACTIVE);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(ACTIVE) ? 10 : 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraftingCoreBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide || type != CrystalnexusModBlockEntities.CRAFTING_CORE.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> CraftingCoreBlockEntity.tick(
                (ServerLevel) tickLevel, pos, tickState, (CraftingCoreBlockEntity) blockEntity);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) refreshNearby(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) refreshNearby(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void refreshNearby(Level level, BlockPos pos) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos check = pos.offset(x, 0, z);
            if (level.getBlockEntity(check) instanceof CraftingCoreBlockEntity core) core.refreshSoon();
        }
    }
}
