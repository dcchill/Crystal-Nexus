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
import net.minecraft.world.Containers;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.world.inventory.ComputationClusterGUIMenu;
import net.crystalnexus.procedures.CrystalPurifierBlockAddedProcedure;
import net.crystalnexus.procedures.ComputationClusterOnTickUpdateProcedure;
import net.crystalnexus.block.entity.ComputationClusterBlockEntity;

import io.netty.buffer.Unpooled;

public class ComputationClusterBlock extends Block implements EntityBlock {
	public static final IntegerProperty BLOCKSTATE = IntegerProperty.create("blockstate", 0, 2);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public ComputationClusterBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(4f, 14f).lightLevel(s -> (new Object() {
			public int getLightLevel() {
				if (s.getValue(BLOCKSTATE) == 1)
					return 0;
				if (s.getValue(BLOCKSTATE) == 2)
					return 15;
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
				default -> Shapes.or(box(1, 0, 0, 15, 4, 13), box(1, 0, 15, 2, 4, 16), box(1, 0, 13, 2, 1, 15), box(1, 3, 13, 2, 4, 15), box(14, 3, 13, 15, 4, 15), box(14, 0, 15, 15, 4, 16), box(14, 0, 13, 15, 1, 15), box(1, 6, 0, 15, 10, 13),
						box(1, 6, 15, 2, 10, 16), box(1, 6, 13, 2, 7, 15), box(1, 9, 13, 2, 10, 15), box(14, 9, 13, 15, 10, 15), box(14, 6, 15, 15, 10, 16), box(14, 6, 13, 15, 7, 15), box(1, 12, 0, 15, 16, 13), box(1, 12, 15, 2, 16, 16),
						box(1, 12, 13, 2, 13, 15), box(1, 15, 13, 2, 16, 15), box(14, 15, 13, 15, 16, 15), box(14, 12, 15, 15, 16, 16), box(14, 12, 13, 15, 13, 15), box(12, 7, 13, 13, 14, 14), box(10, 11, 13, 11, 14, 14), box(10, 1, 13, 11, 8, 14),
						box(15, 0, 11, 16, 16, 13), box(0, 0, 11, 1, 16, 13), box(0, 0, 0, 1, 16, 2), box(15, 0, 0, 16, 16, 2));
				case NORTH -> Shapes.or(box(1, 0, 3, 15, 4, 16), box(14, 0, 0, 15, 4, 1), box(14, 0, 1, 15, 1, 3), box(14, 3, 1, 15, 4, 3), box(1, 3, 1, 2, 4, 3), box(1, 0, 0, 2, 4, 1), box(1, 0, 1, 2, 1, 3), box(1, 6, 3, 15, 10, 16),
						box(14, 6, 0, 15, 10, 1), box(14, 6, 1, 15, 7, 3), box(14, 9, 1, 15, 10, 3), box(1, 9, 1, 2, 10, 3), box(1, 6, 0, 2, 10, 1), box(1, 6, 1, 2, 7, 3), box(1, 12, 3, 15, 16, 16), box(14, 12, 0, 15, 16, 1),
						box(14, 12, 1, 15, 13, 3), box(14, 15, 1, 15, 16, 3), box(1, 15, 1, 2, 16, 3), box(1, 12, 0, 2, 16, 1), box(1, 12, 1, 2, 13, 3), box(3, 7, 2, 4, 14, 3), box(5, 11, 2, 6, 14, 3), box(5, 1, 2, 6, 8, 3), box(0, 0, 3, 1, 16, 5),
						box(15, 0, 3, 16, 16, 5), box(15, 0, 14, 16, 16, 16), box(0, 0, 14, 1, 16, 16));
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
		if (state.getValue(BLOCKSTATE) == 2) {
			return switch (state.getValue(FACING)) {
				default -> Shapes.or(box(1, 0, 0, 15, 4, 13), box(1, 0, 15, 2, 4, 16), box(1, 0, 13, 2, 1, 15), box(1, 3, 13, 2, 4, 15), box(14, 3, 13, 15, 4, 15), box(14, 0, 15, 15, 4, 16), box(14, 0, 13, 15, 1, 15), box(1, 6, 0, 15, 10, 13),
						box(1, 6, 15, 2, 10, 16), box(1, 6, 13, 2, 7, 15), box(1, 9, 13, 2, 10, 15), box(14, 9, 13, 15, 10, 15), box(14, 6, 15, 15, 10, 16), box(14, 6, 13, 15, 7, 15), box(1, 12, 0, 15, 16, 13), box(1, 12, 15, 2, 16, 16),
						box(1, 12, 13, 2, 13, 15), box(1, 15, 13, 2, 16, 15), box(14, 15, 13, 15, 16, 15), box(14, 12, 15, 15, 16, 16), box(14, 12, 13, 15, 13, 15), box(12, 7, 13, 13, 14, 14), box(10, 11, 13, 11, 14, 14), box(10, 1, 13, 11, 8, 14),
						box(15, 0, 11, 16, 16, 13), box(0, 0, 11, 1, 16, 13), box(0, 0, 0, 1, 16, 2), box(15, 0, 0, 16, 16, 2));
				case NORTH -> Shapes.or(box(1, 0, 3, 15, 4, 16), box(14, 0, 0, 15, 4, 1), box(14, 0, 1, 15, 1, 3), box(14, 3, 1, 15, 4, 3), box(1, 3, 1, 2, 4, 3), box(1, 0, 0, 2, 4, 1), box(1, 0, 1, 2, 1, 3), box(1, 6, 3, 15, 10, 16),
						box(14, 6, 0, 15, 10, 1), box(14, 6, 1, 15, 7, 3), box(14, 9, 1, 15, 10, 3), box(1, 9, 1, 2, 10, 3), box(1, 6, 0, 2, 10, 1), box(1, 6, 1, 2, 7, 3), box(1, 12, 3, 15, 16, 16), box(14, 12, 0, 15, 16, 1),
						box(14, 12, 1, 15, 13, 3), box(14, 15, 1, 15, 16, 3), box(1, 15, 1, 2, 16, 3), box(1, 12, 0, 2, 16, 1), box(1, 12, 1, 2, 13, 3), box(3, 7, 2, 4, 14, 3), box(5, 11, 2, 6, 14, 3), box(5, 1, 2, 6, 8, 3), box(0, 0, 3, 1, 16, 5),
						box(15, 0, 3, 16, 16, 5), box(15, 0, 14, 16, 16, 16), box(0, 0, 14, 1, 16, 16));
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
		CrystalPurifierBlockAddedProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
		super.tick(blockstate, world, pos, random);
		ComputationClusterOnTickUpdateProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
		world.scheduleTick(pos, this, 1);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
		super.useWithoutItem(blockstate, world, pos, entity, hit);
		if (entity instanceof ServerPlayer player) {
			player.openMenu(new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("Computation Cluster");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
					return new ComputationClusterGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
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
		return new ComputationClusterBlockEntity(pos, state);
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
			if (blockEntity instanceof ComputationClusterBlockEntity be) {
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
		if (tileentity instanceof ComputationClusterBlockEntity be)
			return AbstractContainerMenu.getRedstoneSignalFromContainer(be);
		else
			return 0;
	}
}