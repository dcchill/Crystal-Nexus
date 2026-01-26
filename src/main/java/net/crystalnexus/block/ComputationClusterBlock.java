package net.crystalnexus.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class ComputationClusterBlock extends Block {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public ComputationClusterBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(4f, 14f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return true;
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			default -> Shapes.or(box(1, 0, 0, 15, 4, 13), box(1, 0, 15, 2, 4, 16), box(1, 0, 13, 2, 1, 15), box(1, 3, 13, 2, 4, 15), box(14, 3, 13, 15, 4, 15), box(14, 0, 15, 15, 4, 16), box(14, 0, 13, 15, 1, 15), box(1, 6, 0, 15, 10, 13),
					box(1, 6, 15, 2, 10, 16), box(1, 6, 13, 2, 7, 15), box(1, 9, 13, 2, 10, 15), box(14, 9, 13, 15, 10, 15), box(14, 6, 15, 15, 10, 16), box(14, 6, 13, 15, 7, 15), box(1, 12, 0, 15, 16, 13), box(1, 12, 15, 2, 16, 16),
					box(1, 12, 13, 2, 13, 15), box(1, 15, 13, 2, 16, 15), box(14, 15, 13, 15, 16, 15), box(14, 12, 15, 15, 16, 16), box(14, 12, 13, 15, 13, 15), box(12, 7, 13, 13, 14, 14), box(10, 11, 13, 11, 14, 14), box(10, 1, 13, 11, 8, 14),
					box(15, 0, 11, 16, 16, 13), box(0, 0, 11, 1, 16, 13), box(0, 0, 0, 1, 16, 2), box(15, 0, 0, 16, 16, 2));
			case NORTH -> Shapes.or(box(1, 0, 3, 15, 4, 16), box(14, 0, 0, 15, 4, 1), box(14, 0, 1, 15, 1, 3), box(14, 3, 1, 15, 4, 3), box(1, 3, 1, 2, 4, 3), box(1, 0, 0, 2, 4, 1), box(1, 0, 1, 2, 1, 3), box(1, 6, 3, 15, 10, 16),
					box(14, 6, 0, 15, 10, 1), box(14, 6, 1, 15, 7, 3), box(14, 9, 1, 15, 10, 3), box(1, 9, 1, 2, 10, 3), box(1, 6, 0, 2, 10, 1), box(1, 6, 1, 2, 7, 3), box(1, 12, 3, 15, 16, 16), box(14, 12, 0, 15, 16, 1), box(14, 12, 1, 15, 13, 3),
					box(14, 15, 1, 15, 16, 3), box(1, 15, 1, 2, 16, 3), box(1, 12, 0, 2, 16, 1), box(1, 12, 1, 2, 13, 3), box(3, 7, 2, 4, 14, 3), box(5, 11, 2, 6, 14, 3), box(5, 1, 2, 6, 8, 3), box(0, 0, 3, 1, 16, 5), box(15, 0, 3, 16, 16, 5),
					box(15, 0, 14, 16, 16, 16), box(0, 0, 14, 1, 16, 16));
			case EAST -> Shapes.or(box(0, 0, 1, 13, 4, 15), box(15, 0, 14, 16, 4, 15), box(13, 0, 14, 15, 1, 15), box(13, 3, 14, 15, 4, 15), box(13, 3, 1, 15, 4, 2), box(15, 0, 1, 16, 4, 2), box(13, 0, 1, 15, 1, 2), box(0, 6, 1, 13, 10, 15),
					box(15, 6, 14, 16, 10, 15), box(13, 6, 14, 15, 7, 15), box(13, 9, 14, 15, 10, 15), box(13, 9, 1, 15, 10, 2), box(15, 6, 1, 16, 10, 2), box(13, 6, 1, 15, 7, 2), box(0, 12, 1, 13, 16, 15), box(15, 12, 14, 16, 16, 15),
					box(13, 12, 14, 15, 13, 15), box(13, 15, 14, 15, 16, 15), box(13, 15, 1, 15, 16, 2), box(15, 12, 1, 16, 16, 2), box(13, 12, 1, 15, 13, 2), box(13, 7, 3, 14, 14, 4), box(13, 11, 5, 14, 14, 6), box(13, 1, 5, 14, 8, 6),
					box(11, 0, 0, 13, 16, 1), box(11, 0, 15, 13, 16, 16), box(0, 0, 15, 2, 16, 16), box(0, 0, 0, 2, 16, 1));
			case WEST -> Shapes.or(box(3, 0, 1, 16, 4, 15), box(0, 0, 1, 1, 4, 2), box(1, 0, 1, 3, 1, 2), box(1, 3, 1, 3, 4, 2), box(1, 3, 14, 3, 4, 15), box(0, 0, 14, 1, 4, 15), box(1, 0, 14, 3, 1, 15), box(3, 6, 1, 16, 10, 15),
					box(0, 6, 1, 1, 10, 2), box(1, 6, 1, 3, 7, 2), box(1, 9, 1, 3, 10, 2), box(1, 9, 14, 3, 10, 15), box(0, 6, 14, 1, 10, 15), box(1, 6, 14, 3, 7, 15), box(3, 12, 1, 16, 16, 15), box(0, 12, 1, 1, 16, 2), box(1, 12, 1, 3, 13, 2),
					box(1, 15, 1, 3, 16, 2), box(1, 15, 14, 3, 16, 15), box(0, 12, 14, 1, 16, 15), box(1, 12, 14, 3, 13, 15), box(2, 7, 12, 3, 14, 13), box(2, 11, 10, 3, 14, 11), box(2, 1, 10, 3, 8, 11), box(3, 0, 15, 5, 16, 16),
					box(3, 0, 0, 5, 16, 1), box(14, 0, 0, 16, 16, 1), box(14, 0, 15, 16, 16, 16));
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}
}