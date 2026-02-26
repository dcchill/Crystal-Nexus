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

public class MediumSulfurCrystalBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MediumSulfurCrystalBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.MEDIUM_AMETHYST_BUD)
                .strength(1.45f, 5f)
                .lightLevel(s -> 5)
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
            case UP -> box(3, 0, 3, 13, 8, 13);
            case DOWN -> box(3, 8, 3, 13, 16, 13);
            case NORTH -> box(3, 3, 8, 13, 13, 16);
            case SOUTH -> box(3, 3, 0, 13, 13, 8);
            case EAST -> box(0, 3, 3, 8, 13, 13);
            case WEST -> box(8, 3, 3, 16, 13, 13);
        };
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        Direction supportDirection = state.getValue(FACING).getOpposite();
        BlockPos supportPos = pos.relative(supportDirection);

        if (!level.getBlockState(supportPos)
                .is(CrystalnexusModBlocks.BUDDING_SULFUR_CRYSTAL.get()))
            return;

        if (random.nextInt(35) == 0) {
            level.setBlock(pos,
                    CrystalnexusModBlocks.LARGE_SULFUR_CRYSTAL.get()
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