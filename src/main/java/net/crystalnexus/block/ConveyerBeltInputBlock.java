package net.crystalnexus.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import javax.annotation.Nullable;

import net.crystalnexus.procedures.ConveyerBeltOnTickUpdateProcedure;
import net.crystalnexus.block.entity.ConveyerBeltInputBlockEntity;

public class ConveyerBeltInputBlock extends Block implements EntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public ConveyerBeltInputBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(0.9f, 8f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
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
public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    return (lvl, pos, st, be) -> {
        if (!lvl.isClientSide && be instanceof net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity belt) {
            belt.serverTick();
        }
    };
}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			default -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 13, 9, 5, 15), box(4, 0, 12, 12, 3, 16), box(7, 2, 1, 9, 5, 3), box(4, 0, 0, 12, 3, 4), box(0, 8, 14, 2, 18, 16), box(14, 8, 14, 16, 18, 16), box(2, 16, 14, 14, 18, 16),
					box(2, 8, 15, 14, 16, 17));
			case NORTH -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 1, 9, 5, 3), box(4, 0, 0, 12, 3, 4), box(7, 2, 13, 9, 5, 15), box(4, 0, 12, 12, 3, 16), box(14, 8, 0, 16, 18, 2), box(0, 8, 0, 2, 18, 2), box(2, 16, 0, 14, 18, 2),
					box(2, 8, -1, 14, 16, 1));
			case EAST -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(13, 2, 7, 15, 5, 9), box(12, 0, 4, 16, 3, 12), box(1, 2, 7, 3, 5, 9), box(0, 0, 4, 4, 3, 12), box(14, 8, 14, 16, 18, 16), box(14, 8, 0, 16, 18, 2), box(14, 16, 2, 16, 18, 14),
					box(15, 8, 2, 17, 16, 14));
			case WEST -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(1, 2, 7, 3, 5, 9), box(0, 0, 4, 4, 3, 12), box(13, 2, 7, 15, 5, 9), box(12, 0, 4, 16, 3, 12), box(0, 8, 0, 2, 18, 2), box(0, 8, 14, 2, 18, 16), box(0, 16, 2, 2, 18, 14),
					box(-1, 8, 2, 1, 16, 14));
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

	@Override
	public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(blockstate, world, pos, oldState, moving);
		world.scheduleTick(pos, this, 1);
	}


	@Override
	public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
		BlockEntity tileEntity = worldIn.getBlockEntity(pos);
		return tileEntity instanceof MenuProvider menuProvider ? menuProvider : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ConveyerBeltInputBlockEntity(pos, state);
	}

	@Override
	public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
		super.triggerEvent(state, world, pos, eventID, eventParam);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity != null && blockEntity.triggerEvent(eventID, eventParam);
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof ConveyerBeltInputBlockEntity be) {
				Containers.dropContents(world, pos, be);
				world.updateNeighbourForOutputSignal(pos, this);
			}
			super.onRemove(state, world, pos, newState, isMoving);
		}
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
		BlockEntity tileentity = world.getBlockEntity(pos);
		if (tileentity instanceof ConveyerBeltInputBlockEntity be)
			return AbstractContainerMenu.getRedstoneSignalFromContainer(be);
		else
			return 0;
	}
}