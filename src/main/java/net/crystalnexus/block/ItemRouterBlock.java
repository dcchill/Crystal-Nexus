package net.crystalnexus.block;

import net.crystalnexus.block.entity.ItemRouterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class ItemRouterBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final boolean merger;

    public ItemRouterBlock(boolean merger) {
        super(Properties.of().sound(SoundType.METAL).strength(1f, 10f));
        this.merger = merger;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public boolean isMerger() { return merger; }

    public static Direction redPort(Direction facing, int port) {
        return switch (port) {
            case 0 -> facing;
            case 1 -> facing.getCounterClockWise();
            case 2 -> facing.getClockWise();
            default -> throw new IllegalArgumentException("Invalid router port: " + port);
        };
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, merger ? context.getHorizontalDirection().getOpposite() : context.getHorizontalDirection());
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ItemRouterBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (world, pos, current, entity) -> {
            if (entity instanceof ItemRouterBlockEntity router) router.tick();
        };
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!level.isClientSide && data.contains("BlockEntityTag") && level.getBlockEntity(pos) instanceof ItemRouterBlockEntity router) {
            router.loadWithComponents(data.getCompound("BlockEntityTag"), level.registryAccess());
            router.setChanged();
        }
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock()) {
            if (level.getBlockEntity(pos) instanceof ItemRouterBlockEntity router) Containers.dropContents(level, pos, router);
            super.onRemove(state, level, pos, next, moving);
        }
    }
}
