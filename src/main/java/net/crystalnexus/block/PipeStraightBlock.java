package net.crystalnexus.block;

import net.crystalnexus.block.entity.PipeStraightBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

public class PipeStraightBlock extends Block implements EntityBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final VoxelShape CORE = box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape ARM_NORTH = box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape ARM_SOUTH = box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape ARM_WEST = box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape ARM_EAST = box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape ARM_DOWN = box(5, 0, 5, 11, 5, 11);
    private static final VoxelShape ARM_UP = box(5, 11, 5, 11, 16, 11);

    public PipeStraightBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5f, 11f).noOcclusion()
            .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
            .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
            .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return updateConnections(level, pos, state);
    }

    private BlockState updateConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(property(direction), canConnect(level, pos.relative(direction), direction));
        }
        return state;
    }

    private boolean canConnect(LevelAccessor level, BlockPos neighborPos, Direction direction) {
        if (level.getBlockState(neighborPos).getBlock() instanceof PipeStraightBlock) return true;
        if (level instanceof Level actualLevel) {
            if (level.getBlockState(neighborPos).getBlock() instanceof TemporalExploiterBlock) {
                return actualLevel.getCapability(Capabilities.FluidHandler.BLOCK,
                    neighborPos, direction.getOpposite()) != null;
            }
            if (actualLevel.getCapability(Capabilities.FluidHandler.BLOCK,
                neighborPos, direction.getOpposite()) != null
                || actualLevel.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, null) != null) return true;
            for (Direction side : Direction.values()) {
                if (actualLevel.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, side) != null) return true;
            }
        }
        return false;
    }

    public static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    public static Direction connectionAt(BlockState state, BlockPos pos, Vec3 hit, Direction fallback) {
        double x = hit.x - pos.getX() - 0.5;
        double y = hit.y - pos.getY() - 0.5;
        double z = hit.z - pos.getZ() - 0.5;
        double best = 3.0 / 16.0;
        Direction selected = null;
        for (Direction direction : Direction.values()) {
            if (!state.getValue(property(direction))) continue;
            double distance = x * direction.getStepX() + y * direction.getStepY() + z * direction.getStepZ();
            if (distance > best) {
                best = distance;
                selected = direction;
            }
        }
        return selected == null ? fallback : selected;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_SOUTH);
        if (state.getValue(WEST)) shape = Shapes.or(shape, ARM_WEST);
        if (state.getValue(EAST)) shape = Shapes.or(shape, ARM_EAST);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, ARM_DOWN);
        if (state.getValue(UP)) shape = Shapes.or(shape, ARM_UP);
        return shape;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeStraightBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != CrystalnexusModBlockEntities.PIPE_STRAIGHT.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) ->
            ((PipeStraightBlockEntity) blockEntity).serverTick();
    }
}
