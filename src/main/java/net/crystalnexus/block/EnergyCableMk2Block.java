package net.crystalnexus.block;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.energy.IEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

import net.crystalnexus.block.entity.EnergyCableMk2BlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;

public class EnergyCableMk2Block extends Block implements EntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");

    /**
     * REQUIRED by MCreator:
     * CrystalnexusModBlocks uses EnergyCableMk2Block::new
     * and resolves it as Function<ResourceLocation, Block>
     */
    public EnergyCableMk2Block(ResourceLocation id) {
        this(BlockBehaviour.Properties.of().noOcclusion());
    }

    /**
     * Internal constructor
     */
    public EnergyCableMk2Block(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, false)
            .setValue(EAST, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false)
        );
    }
// Core matches model: 6..10 in all axes
private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);

// Arms are 4x4 extending from the core
private static final VoxelShape ARM_NORTH = Block.box(6, 6, 0, 10, 10, 6);
private static final VoxelShape ARM_SOUTH = Block.box(6, 6, 10, 10, 10, 16);
private static final VoxelShape ARM_WEST  = Block.box(0, 6, 6, 6, 10, 10);
private static final VoxelShape ARM_EAST  = Block.box(10, 6, 6, 16, 10, 10);
private static final VoxelShape ARM_DOWN  = Block.box(6, 0, 6, 10, 6, 10);
private static final VoxelShape ARM_UP    = Block.box(6, 10, 6, 10, 16, 10);

	@Override
public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext ctx) {
    VoxelShape shape = CORE;

    if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_NORTH);
    if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_SOUTH);
    if (state.getValue(WEST))  shape = Shapes.or(shape, ARM_WEST);
    if (state.getValue(EAST))  shape = Shapes.or(shape, ARM_EAST);
    if (state.getValue(DOWN))  shape = Shapes.or(shape, ARM_DOWN);
    if (state.getValue(UP))    shape = Shapes.or(shape, ARM_UP);

    return shape;
}

@Override
public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext ctx) {
    // Usually same as outline shape for cables
    return getShape(state, level, pos, ctx);
}

@Override
public boolean isCollisionShapeFullBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
    return false;
}

@Override
public boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
    return true;
}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return updateConnections(ctx.getLevel(), ctx.getClickedPos(), this.defaultBlockState());
    }

    @Override
    public BlockState updateShape(
        BlockState state,
        Direction dir,
        BlockState neighborState,
        LevelAccessor level,
        BlockPos pos,
        BlockPos neighborPos
    ) {
        return updateConnections(level, pos, state);
    }

    private BlockState updateConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        return state
            .setValue(NORTH, canConnect(level, pos, Direction.NORTH))
            .setValue(EAST,  canConnect(level, pos, Direction.EAST))
            .setValue(SOUTH, canConnect(level, pos, Direction.SOUTH))
            .setValue(WEST,  canConnect(level, pos, Direction.WEST))
            .setValue(UP,    canConnect(level, pos, Direction.UP))
            .setValue(DOWN,  canConnect(level, pos, Direction.DOWN));
    }

    private boolean canConnect(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos otherPos = pos.relative(dir);
        BlockState otherState = level.getBlockState(otherPos);

        // Connect to other cables
        if (otherState.getBlock() == this) return true;

        // Connect to blocks with energy capability
        if (level instanceof Level l && l instanceof ILevelExtension ext) {
            IEnergyStorage storage = ext.getCapability(
                Capabilities.EnergyStorage.BLOCK,
                otherPos,
                dir.getOpposite()
            );
            return storage != null;
        }

        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableMk2BlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type
    ) {
        if (level.isClientSide) return null;

        return type == CrystalnexusModBlockEntities.ENERGY_CABLE_MK_2.get()
            ? (lvl, p, s, be) -> ((EnergyCableMk2BlockEntity) be).serverTick()
            : null;
    }
}
