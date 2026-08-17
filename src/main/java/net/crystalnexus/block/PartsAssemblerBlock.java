package net.crystalnexus.block;

import net.crystalnexus.block.entity.PartsAssemblerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public final class PartsAssemblerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public PartsAssemblerBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(2.5f, 15f).requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, LIT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 15; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof PartsAssemblerBlockEntity assembler) {
            serverPlayer.openMenu(assembler, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PartsAssemblerBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != CrystalnexusModBlockEntities.PARTS_ASSEMBLER.get() ? null
            : (tickerLevel, pos, tickerState, blockEntity) -> PartsAssemblerBlockEntity.serverTick(tickerLevel, pos, tickerState, (PartsAssemblerBlockEntity) blockEntity);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof PartsAssemblerBlockEntity assembler) {
            Containers.dropContents(level, pos, assembler);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
