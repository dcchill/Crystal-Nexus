package net.crystalnexus.block;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.world.inventory.NodeExtractorGUIMenu;
import net.crystalnexus.procedures.NodeExtractorOnTickUpdateProcedure;
import net.crystalnexus.procedures.CrystalPurifierBlockAddedProcedure;
import net.crystalnexus.block.entity.NodeExtractorBlockEntity;

import java.util.List;

import io.netty.buffer.Unpooled;

public class NodeExtractorBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	public NodeExtractorBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5f, 8f).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("block.crystalnexus.node_extractor.description_0"));
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return state.getFluidState().isEmpty();
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
			default -> Shapes.or(box(2, 14, 0, 14, 15.99, 2), box(2, 14, 14, 14, 15.99, 16), box(2, 4.01, 14, 14, 6, 16), box(2, 9, 7, 14, 10.99, 9), box(0, 0, 0, 2, 16, 2), box(0, 0, 14, 2, 16, 16), box(14, 0, 14, 16, 16, 16),
					box(14, 0, 0, 16, 16, 2), box(0, 14, 2, 2, 16, 14), box(14, 14, 2, 16, 16, 14), box(14, 9, 2, 16, 11, 14), box(0, 9, 2, 2, 11, 14), box(4, 6, 4, 12, 14, 12), box(4, 6, 14, 12, 14, 16), box(4, 0, 4, 12, 2, 12),
					box(5, 2, 5, 11, 12, 11), box(5, 7, 4, 11, 13, 14));
			case NORTH -> Shapes.or(box(2, 14, 14, 14, 15.99, 16), box(2, 14, 0, 14, 15.99, 2), box(2, 4.01, 0, 14, 6, 2), box(2, 9, 7, 14, 10.99, 9), box(14, 0, 14, 16, 16, 16), box(14, 0, 0, 16, 16, 2), box(0, 0, 0, 2, 16, 2),
					box(0, 0, 14, 2, 16, 16), box(14, 14, 2, 16, 16, 14), box(0, 14, 2, 2, 16, 14), box(0, 9, 2, 2, 11, 14), box(14, 9, 2, 16, 11, 14), box(4, 6, 4, 12, 14, 12), box(4, 6, 0, 12, 14, 2), box(4, 0, 4, 12, 2, 12),
					box(5, 2, 5, 11, 12, 11), box(5, 7, 2, 11, 13, 12));
			case EAST -> Shapes.or(box(0, 14, 2, 2, 15.99, 14), box(14, 14, 2, 16, 15.99, 14), box(14, 4.01, 2, 16, 6, 14), box(7, 9, 2, 9, 10.99, 14), box(0, 0, 14, 2, 16, 16), box(14, 0, 14, 16, 16, 16), box(14, 0, 0, 16, 16, 2),
					box(0, 0, 0, 2, 16, 2), box(2, 14, 14, 14, 16, 16), box(2, 14, 0, 14, 16, 2), box(2, 9, 0, 14, 11, 2), box(2, 9, 14, 14, 11, 16), box(4, 6, 4, 12, 14, 12), box(14, 6, 4, 16, 14, 12), box(4, 0, 4, 12, 2, 12),
					box(5, 2, 5, 11, 12, 11), box(4, 7, 5, 14, 13, 11));
			case WEST -> Shapes.or(box(14, 14, 2, 16, 15.99, 14), box(0, 14, 2, 2, 15.99, 14), box(0, 4.01, 2, 2, 6, 14), box(7, 9, 2, 9, 10.99, 14), box(14, 0, 0, 16, 16, 2), box(0, 0, 0, 2, 16, 2), box(0, 0, 14, 2, 16, 16),
					box(14, 0, 14, 16, 16, 16), box(2, 14, 0, 14, 16, 2), box(2, 14, 14, 14, 16, 16), box(2, 9, 14, 14, 11, 16), box(2, 9, 0, 14, 11, 2), box(4, 6, 4, 12, 14, 12), box(0, 6, 4, 2, 14, 12), box(4, 0, 4, 12, 2, 12),
					box(5, 2, 5, 11, 12, 11), box(2, 7, 5, 12, 13, 11));
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean flag = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
		return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, flag);
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
		if (state.getValue(WATERLOGGED)) {
			world.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		}
		return super.updateShape(state, facing, facingState, world, currentPos, facingPos);
	}

	@Override
	public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(blockstate, world, pos, oldState, moving);
		world.scheduleTick(pos, this, 1);
		CrystalPurifierBlockAddedProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
		super.tick(blockstate, world, pos, random);
		NodeExtractorOnTickUpdateProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
		world.scheduleTick(pos, this, 1);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
		super.useWithoutItem(blockstate, world, pos, entity, hit);
		if (entity instanceof ServerPlayer player) {
			player.openMenu(new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("Node Extractor");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
					return new NodeExtractorGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
				}
			}, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
		BlockEntity tileEntity = worldIn.getBlockEntity(pos);
		return tileEntity instanceof MenuProvider menuProvider ? menuProvider : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new NodeExtractorBlockEntity(pos, state);
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
			if (blockEntity instanceof NodeExtractorBlockEntity be) {
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
		if (tileentity instanceof NodeExtractorBlockEntity be)
			return AbstractContainerMenu.getRedstoneSignalFromContainer(be);
		else
			return 0;
	}
}