package net.crystalnexus.block;

import net.crystalnexus.CrystalnexusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DepotCableBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    private static final TagKey<Block> COMPONENTS = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "depot_components"));
    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape ARM_NORTH = Block.box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape ARM_SOUTH = Block.box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape ARM_WEST = Block.box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape ARM_EAST = Block.box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape ARM_DOWN = Block.box(6, 0, 6, 10, 6, 10);
    private static final VoxelShape ARM_UP = Block.box(6, 10, 6, 10, 16, 10);

    public DepotCableBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5f).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
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
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return updateConnections(level, pos, state);
    }

    private BlockState updateConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        return state.setValue(NORTH, connects(level, pos, Direction.NORTH))
                .setValue(EAST, connects(level, pos, Direction.EAST))
                .setValue(SOUTH, connects(level, pos, Direction.SOUTH))
                .setValue(WEST, connects(level, pos, Direction.WEST))
                .setValue(UP, connects(level, pos, Direction.UP))
                .setValue(DOWN, connects(level, pos, Direction.DOWN));
    }

    private boolean connects(LevelAccessor level, BlockPos pos, Direction direction) {
        BlockState neighbor = level.getBlockState(pos.relative(direction));
        return neighbor.is(this) || neighbor.is(COMPONENTS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
