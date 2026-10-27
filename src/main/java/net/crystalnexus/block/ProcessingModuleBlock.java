package net.crystalnexus.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.crystalnexus.block.entity.ProcessingModuleBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;

import javax.annotation.Nullable;

public final class ProcessingModuleBlock extends Block implements EntityBlock, net.crystalnexus.processing.TieredMachineBlock {
    public enum Operation { PRESS, ENGRAVE, DRILL, NANOFABRICATE, WELD }
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
        net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

    private final Operation operation;

    public ProcessingModuleBlock(Operation operation) {
        super(BlockBehaviour.Properties.of().strength(2.0F).requiresCorrectToolForDrops());
        this.operation = operation;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public Operation operation() { return operation; }

    @Override public net.crystalnexus.processing.MachineTier machineTier() {
        return operation == Operation.NANOFABRICATE ? net.crystalnexus.processing.MachineTier.HYPER : net.crystalnexus.processing.MachineTier.IRON;
    }

    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ProcessingModuleBlockEntity(pos, state); }

    @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ProcessingModuleBlockEntity module) serverPlayer.openMenu(module, pos);
        return InteractionResult.SUCCESS;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof ProcessingModuleBlockEntity module)
            Containers.dropContents(level, pos, module);
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockEntity(pos.below()) instanceof net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
    }

    @Override public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction,
            BlockState neighborState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == net.minecraft.core.Direction.DOWN && !canSurvive(state, level, pos)
            ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
