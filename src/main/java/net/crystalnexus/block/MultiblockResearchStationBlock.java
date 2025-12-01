package net.crystalnexus.block;

import org.checkerframework.checker.units.qual.s;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.world.inventory.MultiblockGuiPage1Menu;
import net.crystalnexus.procedures.MultiblockResearchStationOnTickUpdateProcedure;
import net.crystalnexus.procedures.MultiblockResearchStationOnBlockRightClickedProcedure;
import net.crystalnexus.block.entity.MultiblockResearchStationBlockEntity;

import io.netty.buffer.Unpooled;

public class MultiblockResearchStationBlock extends Block implements EntityBlock {
	public static final IntegerProperty BLOCKSTATE = IntegerProperty.create("blockstate", 0, 2);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public MultiblockResearchStationBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.1f, 13.5f).lightLevel(s -> (new Object() {
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
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		if (state.getValue(BLOCKSTATE) == 1) {
			return switch (state.getValue(FACING)) {
				default -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(8, 15, 0, 16, 16, 16), box(0, 15, 0, 8, 16, 16), box(2, 14.5, 2, 14, 15.5, 8), box(2, 14.5, 10, 14, 15.5, 14));
				case NORTH -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(0, 15, 0, 8, 16, 16), box(8, 15, 0, 16, 16, 16), box(2, 14.5, 8, 14, 15.5, 14), box(2, 14.5, 2, 14, 15.5, 6));
				case EAST -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(0, 15, 0, 16, 16, 8), box(0, 15, 8, 16, 16, 16), box(2, 14.5, 2, 8, 15.5, 14), box(10, 14.5, 2, 14, 15.5, 14));
				case WEST -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(0, 15, 8, 16, 16, 16), box(0, 15, 0, 16, 16, 8), box(8, 14.5, 2, 14, 15.5, 14), box(2, 14.5, 2, 6, 15.5, 14));
			};
		}
		if (state.getValue(BLOCKSTATE) == 2) {
			return switch (state.getValue(FACING)) {
				default -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(16, 15, 0, 17, 23, 16), box(-1, 15, 0, 0, 23, 16));
				case NORTH -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(-1, 15, 0, 0, 23, 16), box(16, 15, 0, 17, 23, 16));
				case EAST -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(0, 15, -1, 16, 23, 0), box(0, 15, 16, 16, 23, 17));
				case WEST -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(0, 15, 16, 16, 23, 17), box(0, 15, -1, 16, 23, 0));
			};
		}
		return switch (state.getValue(FACING)) {
			default -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(8, 15, 0, 16, 16, 16), box(0, 15, 0, 8, 16, 16), box(2, 14.5, 2, 14, 15.5, 8), box(2, 14.5, 10, 14, 15.5, 14));
			case NORTH -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(0, 15, 0, 8, 16, 16), box(8, 15, 0, 16, 16, 16), box(2, 14.5, 8, 14, 15.5, 14), box(2, 14.5, 2, 14, 15.5, 6));
			case EAST -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(0, 15, 0, 16, 16, 8), box(0, 15, 8, 16, 16, 16), box(2, 14.5, 2, 8, 15.5, 14), box(10, 14.5, 2, 14, 15.5, 14));
			case WEST -> Shapes.or(box(0, 0, 0, 16, 15, 16), box(0, 15, 8, 16, 16, 16), box(0, 15, 0, 16, 16, 8), box(8, 14.5, 2, 14, 15.5, 14), box(2, 14.5, 2, 6, 15.5, 14));
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, BLOCKSTATE);
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
	public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
		super.tick(blockstate, world, pos, random);
		MultiblockResearchStationOnTickUpdateProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
		world.scheduleTick(pos, this, 1);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
		super.useWithoutItem(blockstate, world, pos, entity, hit);
		if (entity instanceof ServerPlayer player) {
			player.openMenu(new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("Multiblock Research Station");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
					return new MultiblockGuiPage1Menu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
				}
			}, pos);
		}
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		double hitX = hit.getLocation().x;
		double hitY = hit.getLocation().y;
		double hitZ = hit.getLocation().z;
		Direction direction = hit.getDirection();
		MultiblockResearchStationOnBlockRightClickedProcedure.execute(world, x, y, z);
		return InteractionResult.SUCCESS;
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
		BlockEntity tileEntity = worldIn.getBlockEntity(pos);
		return tileEntity instanceof MenuProvider menuProvider ? menuProvider : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MultiblockResearchStationBlockEntity(pos, state);
	}

	@Override
	public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
		super.triggerEvent(state, world, pos, eventID, eventParam);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity != null && blockEntity.triggerEvent(eventID, eventParam);
	}
}