package net.crystalnexus.block;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
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

import net.crystalnexus.world.inventory.NodeMinerGUIMenu;
import net.crystalnexus.procedures.NodeMinerOnTickUpdateProcedure;
import net.crystalnexus.procedures.CrystalPurifierBlockAddedProcedure;
import net.crystalnexus.block.entity.NodeMinerBlockEntity;

import java.util.List;

import io.netty.buffer.Unpooled;

public class NodeMinerBlock extends Block implements EntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public NodeMinerBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1f, 10f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("block.crystalnexus.node_miner.description_0"));
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
			default -> Shapes.or(box(2, 14, 0, 14, 15.99, 2), box(2, 7, 7, 14, 8.99, 9), box(0, 0, 0, 2, 16, 2), box(0, 0, 14, 2, 16, 16), box(14, 0, 14, 16, 16, 16), box(14, 0, 0, 16, 16, 2), box(0, 14, 2, 2, 16, 14), box(14, 14, 2, 16, 16, 14),
					box(14, 7, 2, 16, 9, 14), box(0, 7, 2, 2, 9, 14), box(6, -1, 6, 10, 4, 10), box(4, 4, 4, 12, 12, 12), box(2, 14, 14, 14, 15.99, 16), box(2, 4, 14, 14, 5.99, 16), box(2.01, 6.01, 12.01, 13.99, 13.99, 14.99));
			case NORTH -> Shapes.or(box(2, 14, 14, 14, 15.99, 16), box(2, 7, 7, 14, 8.99, 9), box(14, 0, 14, 16, 16, 16), box(14, 0, 0, 16, 16, 2), box(0, 0, 0, 2, 16, 2), box(0, 0, 14, 2, 16, 16), box(14, 14, 2, 16, 16, 14),
					box(0, 14, 2, 2, 16, 14), box(0, 7, 2, 2, 9, 14), box(14, 7, 2, 16, 9, 14), box(6, -1, 6, 10, 4, 10), box(4, 4, 4, 12, 12, 12), box(2, 14, 0, 14, 15.99, 2), box(2, 4, 0, 14, 5.99, 2), box(2.01, 6.01, 1.01, 13.99, 13.99, 3.99));
			case EAST -> Shapes.or(box(0, 14, 2, 2, 15.99, 14), box(7, 7, 2, 9, 8.99, 14), box(0, 0, 14, 2, 16, 16), box(14, 0, 14, 16, 16, 16), box(14, 0, 0, 16, 16, 2), box(0, 0, 0, 2, 16, 2), box(2, 14, 14, 14, 16, 16), box(2, 14, 0, 14, 16, 2),
					box(2, 7, 0, 14, 9, 2), box(2, 7, 14, 14, 9, 16), box(6, -1, 6, 10, 4, 10), box(4, 4, 4, 12, 12, 12), box(14, 14, 2, 16, 15.99, 14), box(14, 4, 2, 16, 5.99, 14), box(12.01, 6.01, 2.01, 14.99, 13.99, 13.99));
			case WEST -> Shapes.or(box(14, 14, 2, 16, 15.99, 14), box(7, 7, 2, 9, 8.99, 14), box(14, 0, 0, 16, 16, 2), box(0, 0, 0, 2, 16, 2), box(0, 0, 14, 2, 16, 16), box(14, 0, 14, 16, 16, 16), box(2, 14, 0, 14, 16, 2), box(2, 14, 14, 14, 16, 16),
					box(2, 7, 14, 14, 9, 16), box(2, 7, 0, 14, 9, 2), box(6, -1, 6, 10, 4, 10), box(4, 4, 4, 12, 12, 12), box(0, 14, 2, 2, 15.99, 14), box(0, 4, 2, 2, 5.99, 14), box(1.01, 6.01, 2.01, 3.99, 13.99, 13.99));
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
		CrystalPurifierBlockAddedProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
		super.tick(blockstate, world, pos, random);
		NodeMinerOnTickUpdateProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
		world.scheduleTick(pos, this, 1);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
		super.useWithoutItem(blockstate, world, pos, entity, hit);
		if (entity instanceof ServerPlayer player) {
			player.openMenu(new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("Node Miner");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
					return new NodeMinerGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
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
		return new NodeMinerBlockEntity(pos, state);
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
			if (blockEntity instanceof NodeMinerBlockEntity be) {
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
		if (tileentity instanceof NodeMinerBlockEntity be)
			return AbstractContainerMenu.getRedstoneSignalFromContainer(be);
		else
			return 0;
	}
}