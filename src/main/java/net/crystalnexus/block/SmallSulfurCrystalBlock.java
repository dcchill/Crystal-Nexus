package net.crystalnexus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class SmallSulfurCrystalBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public SmallSulfurCrystalBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.SMALL_AMETHYST_BUD)
                .strength(1.2f, 5f)
                .lightLevel(s -> 3)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .randomTicks()
                .isRedstoneConductor((bs, br, bp) -> false));

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getClickedFace());
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> box(4, 0, 4, 12, 6, 12);
            case DOWN -> box(4, 10, 4, 12, 16, 12);
            case NORTH -> box(4, 4, 10, 12, 12, 16);
            case SOUTH -> box(4, 4, 0, 12, 12, 6);
            case EAST -> box(0, 4, 4, 6, 12, 12);
            case WEST -> box(10, 4, 4, 16, 12, 12);
        };
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        Direction supportDirection = state.getValue(FACING).getOpposite();
        BlockPos supportPos = pos.relative(supportDirection);

        BlockState supportState = level.getBlockState(supportPos);

        if (!supportState.is(CrystalnexusModBlocks.BUDDING_SULFUR_CRYSTAL.get()))
            return;

        // 20% growth chance
        if (random.nextInt(35) == 0) {
            level.setBlock(pos,
                    CrystalnexusModBlocks.MEDIUM_SULFUR_CRYSTAL.get()
                            .defaultBlockState()
                            .setValue(FACING, state.getValue(FACING)),
                    2);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
                                  BlockState neighborState,
                                  LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {

        Direction supportDirection = state.getValue(FACING).getOpposite();

        if (direction == supportDirection &&
                !neighborState.is(CrystalnexusModBlocks.BUDDING_SULFUR_CRYSTAL.get())) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}