package net.crystalnexus.block;

import org.checkerframework.checker.units.qual.s;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;

import net.crystalnexus.procedures.ConveyerBeltOnTickUpdateProcedure;
import net.crystalnexus.procedures.ConveyerBeltNeighbourBlockChangesProcedure;
import net.crystalnexus.block.entity.ConveyerBeltBlockEntity;

public class ConveyerBeltBlock extends Block implements EntityBlock {
	public static final IntegerProperty BLOCKSTATE = IntegerProperty.create("blockstate", 0, 2);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public ConveyerBeltBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(0.8f, 8f).lightLevel(s -> (new Object() {
			public int getLightLevel() {
				if (s.getValue(BLOCKSTATE) == 1)
					return 0;
				if (s.getValue(BLOCKSTATE) == 2)
					return 0;
				return 0;
			}
		}.getLightLevel())).noOcclusion().isRedstoneConductor((bs, br, bp) -> false).dynamicShape());
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
		if (state.getValue(BLOCKSTATE) == 1) {
			return switch (state.getValue(FACING)) {
				default -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(4, 0, 6, 12, 3, 10));
				case NORTH -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(4, 0, 6, 12, 3, 10));
				case EAST -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(6, 0, 4, 10, 3, 12));
				case WEST -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(6, 0, 4, 10, 3, 12));
			};
		}
		if (state.getValue(BLOCKSTATE) == 2) {
			return switch (state.getValue(FACING)) {
				default -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(4, 0, 6, 12, 3, 10), box(2, 14, 14, 14, 16, 16), box(0, 14, 2, 2, 16, 14), box(14, 14, 2, 16, 16, 14), box(4.01, 14.01, 2.01, 5.99, 15.99, 13.99),
						box(10.01, 14.01, 2.01, 11.99, 15.99, 13.99), box(14, 8, 0, 16, 16, 2), box(2, 14, 0, 14, 16, 2), box(0, 8, 0, 2, 16, 2), box(14, 8, 14, 16, 16, 16), box(0, 8, 14, 2, 16, 16));
				case NORTH -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(4, 0, 6, 12, 3, 10), box(2, 14, 0, 14, 16, 2), box(14, 14, 2, 16, 16, 14), box(0, 14, 2, 2, 16, 14), box(10.01, 14.01, 2.01, 11.99, 15.99, 13.99),
						box(4.01, 14.01, 2.01, 5.99, 15.99, 13.99), box(0, 8, 14, 2, 16, 16), box(2, 14, 14, 14, 16, 16), box(14, 8, 14, 16, 16, 16), box(0, 8, 0, 2, 16, 2), box(14, 8, 0, 16, 16, 2));
				case EAST -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(6, 0, 4, 10, 3, 12), box(14, 14, 2, 16, 16, 14), box(2, 14, 14, 14, 16, 16), box(2, 14, 0, 14, 16, 2), box(2.01, 14.01, 10.01, 13.99, 15.99, 11.99),
						box(2.01, 14.01, 4.01, 13.99, 15.99, 5.99), box(0, 8, 0, 2, 16, 2), box(0, 14, 2, 2, 16, 14), box(0, 8, 14, 2, 16, 16), box(14, 8, 0, 16, 16, 2), box(14, 8, 14, 16, 16, 16));
				case WEST -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(6, 0, 4, 10, 3, 12), box(0, 14, 2, 2, 16, 14), box(2, 14, 0, 14, 16, 2), box(2, 14, 14, 14, 16, 16), box(2.01, 14.01, 4.01, 13.99, 15.99, 5.99),
						box(2.01, 14.01, 10.01, 13.99, 15.99, 11.99), box(14, 8, 14, 16, 16, 16), box(14, 14, 2, 16, 16, 14), box(14, 8, 0, 16, 16, 2), box(0, 8, 14, 2, 16, 16), box(0, 8, 0, 2, 16, 2));
			};
		}
		return switch (state.getValue(FACING)) {
			default -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(4, 0, 6, 12, 3, 10));
			case NORTH -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(4, 0, 6, 12, 3, 10));
			case EAST -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(6, 0, 4, 10, 3, 12));
			case WEST -> Shapes.or(box(0, 5, 0, 16, 8, 16), box(7, 2, 7, 9, 5, 9), box(6, 0, 4, 10, 3, 12));
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, BLOCKSTATE);
	}
public net.minecraft.world.InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
) {
	    if (!player.getMainHandItem().isEmpty())
        return InteractionResult.PASS;
    if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;

    if (!(level.getBlockEntity(pos) instanceof net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity be))
        return net.minecraft.world.InteractionResult.PASS;

    boolean takeAll = player.isShiftKeyDown();
    int amount = takeAll ? 64 : 1;

    for (int i = net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity.SEGMENTS - 1; i >= 0; i--) {
        ItemStack stack = be.getItem(i);
        if (stack.isEmpty()) continue;

        ItemStack taken = stack.split(Math.min(amount, stack.getCount()));
        be.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);

        if (!player.getInventory().add(taken)) {
            player.drop(taken, false);
        }

        return net.minecraft.world.InteractionResult.CONSUME;
    }

    return net.minecraft.world.InteractionResult.PASS;
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
	public void neighborChanged(BlockState blockstate, Level world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean moving) {
		super.neighborChanged(blockstate, world, pos, neighborBlock, fromPos, moving);
		ConveyerBeltNeighbourBlockChangesProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
		BlockEntity tileEntity = worldIn.getBlockEntity(pos);
		return tileEntity instanceof MenuProvider menuProvider ? menuProvider : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ConveyerBeltBlockEntity(pos, state);
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
			if (blockEntity instanceof ConveyerBeltBlockEntity be) {
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
		if (tileentity instanceof ConveyerBeltBlockEntity be)
			return AbstractContainerMenu.getRedstoneSignalFromContainer(be);
		else
			return 0;
	}
}