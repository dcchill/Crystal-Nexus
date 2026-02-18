package net.crystalnexus.block;

import org.checkerframework.checker.units.qual.s;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.procedures.FactoryLightRedstoneOnProcedure;
import net.crystalnexus.procedures.FactoryLightRedstoneOffProcedure;
import net.crystalnexus.procedures.FactoryLightOnBlockRightClickedProcedure;

import java.util.List;

public class FactoryLightBlock extends Block {
	public static final IntegerProperty BLOCKSTATE = IntegerProperty.create("blockstate", 0, 2);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public FactoryLightBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.GLASS).strength(2.5f, 10f).lightLevel(s -> (new Object() {
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
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("block.crystalnexus.factory_light.description_0"));
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
				default -> Shapes.or(box(9, 13, 2, 11, 15, 14), box(5, 13, 2, 7, 15, 14), box(4, 12, 0, 12, 16, 2), box(4, 12, 14, 12, 16, 16), box(4, 15, 2, 12, 16, 14), box(8.5, 12.5, 2, 11.5, 15, 14), box(4.5, 12.5, 2, 7.5, 15, 14));
				case NORTH -> Shapes.or(box(5, 13, 2, 7, 15, 14), box(9, 13, 2, 11, 15, 14), box(4, 12, 14, 12, 16, 16), box(4, 12, 0, 12, 16, 2), box(4, 15, 2, 12, 16, 14), box(4.5, 12.5, 2, 7.5, 15, 14), box(8.5, 12.5, 2, 11.5, 15, 14));
				case EAST -> Shapes.or(box(2, 13, 5, 14, 15, 7), box(2, 13, 9, 14, 15, 11), box(0, 12, 4, 2, 16, 12), box(14, 12, 4, 16, 16, 12), box(2, 15, 4, 14, 16, 12), box(2, 12.5, 4.5, 14, 15, 7.5), box(2, 12.5, 8.5, 14, 15, 11.5));
				case WEST -> Shapes.or(box(2, 13, 9, 14, 15, 11), box(2, 13, 5, 14, 15, 7), box(14, 12, 4, 16, 16, 12), box(0, 12, 4, 2, 16, 12), box(2, 15, 4, 14, 16, 12), box(2, 12.5, 8.5, 14, 15, 11.5), box(2, 12.5, 4.5, 14, 15, 7.5));
			};
		}
		if (state.getValue(BLOCKSTATE) == 2) {
			return switch (state.getValue(FACING)) {
				default -> Shapes.or(box(9, 13, 2, 11, 15, 14), box(5, 13, 2, 7, 15, 14), box(4, 12, 0, 12, 16, 2), box(4, 12, 14, 12, 16, 16), box(4, 15, 2, 12, 16, 14), box(8.5, 12.5, 2, 11.5, 15, 14), box(4.5, 12.5, 2, 7.5, 15, 14));
				case NORTH -> Shapes.or(box(5, 13, 2, 7, 15, 14), box(9, 13, 2, 11, 15, 14), box(4, 12, 14, 12, 16, 16), box(4, 12, 0, 12, 16, 2), box(4, 15, 2, 12, 16, 14), box(4.5, 12.5, 2, 7.5, 15, 14), box(8.5, 12.5, 2, 11.5, 15, 14));
				case EAST -> Shapes.or(box(2, 13, 5, 14, 15, 7), box(2, 13, 9, 14, 15, 11), box(0, 12, 4, 2, 16, 12), box(14, 12, 4, 16, 16, 12), box(2, 15, 4, 14, 16, 12), box(2, 12.5, 4.5, 14, 15, 7.5), box(2, 12.5, 8.5, 14, 15, 11.5));
				case WEST -> Shapes.or(box(2, 13, 9, 14, 15, 11), box(2, 13, 5, 14, 15, 7), box(14, 12, 4, 16, 16, 12), box(0, 12, 4, 2, 16, 12), box(2, 15, 4, 14, 16, 12), box(2, 12.5, 8.5, 14, 15, 11.5), box(2, 12.5, 4.5, 14, 15, 7.5));
			};
		}
		return switch (state.getValue(FACING)) {
			default -> Shapes.or(box(9, 13, 2, 11, 15, 14), box(5, 13, 2, 7, 15, 14), box(4, 12, 0, 12, 16, 2), box(4, 12, 14, 12, 16, 16), box(4, 15, 2, 12, 16, 14), box(8.5, 12.5, 2, 11.5, 15, 14), box(4.5, 12.5, 2, 7.5, 15, 14));
			case NORTH -> Shapes.or(box(5, 13, 2, 7, 15, 14), box(9, 13, 2, 11, 15, 14), box(4, 12, 14, 12, 16, 16), box(4, 12, 0, 12, 16, 2), box(4, 15, 2, 12, 16, 14), box(4.5, 12.5, 2, 7.5, 15, 14), box(8.5, 12.5, 2, 11.5, 15, 14));
			case EAST -> Shapes.or(box(2, 13, 5, 14, 15, 7), box(2, 13, 9, 14, 15, 11), box(0, 12, 4, 2, 16, 12), box(14, 12, 4, 16, 16, 12), box(2, 15, 4, 14, 16, 12), box(2, 12.5, 4.5, 14, 15, 7.5), box(2, 12.5, 8.5, 14, 15, 11.5));
			case WEST -> Shapes.or(box(2, 13, 9, 14, 15, 11), box(2, 13, 5, 14, 15, 7), box(14, 12, 4, 16, 16, 12), box(0, 12, 4, 2, 16, 12), box(2, 15, 4, 14, 16, 12), box(2, 12.5, 8.5, 14, 15, 11.5), box(2, 12.5, 4.5, 14, 15, 7.5));
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
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		return true;
	}

	@Override
	public void neighborChanged(BlockState blockstate, Level world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean moving) {
		super.neighborChanged(blockstate, world, pos, neighborBlock, fromPos, moving);
		if (world.getBestNeighborSignal(pos) > 0) {
			FactoryLightRedstoneOnProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
		} else {
			FactoryLightRedstoneOffProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
		}
	}

	@Override
	public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
		super.useWithoutItem(blockstate, world, pos, entity, hit);
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		double hitX = hit.getLocation().x;
		double hitY = hit.getLocation().y;
		double hitZ = hit.getLocation().z;
		Direction direction = hit.getDirection();
		FactoryLightOnBlockRightClickedProcedure.execute(world, x, y, z, blockstate);
		return InteractionResult.SUCCESS;
	}
}