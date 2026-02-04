package net.crystalnexus.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

import net.crystalnexus.block.entity.QuarryBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;

import javax.annotation.Nullable;

public class QuarryBlock extends Block implements EntityBlock {
	public QuarryBlock() {
		super(BlockBehaviour.Properties.of()
			.sound(SoundType.METAL)
			.strength(1f, 10f)
			.noOcclusion()
			.isRedstoneConductor((bs, br, bp) -> false));
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
		return Shapes.or(
			box(0, 0, 6.4087, 1, 5, 9.5913),
			box(15, 0, 6.4087, 16, 5, 9.5913),
			box(0, 5, 0, 16, 16, 16)
		);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (world.isClientSide) return InteractionResult.SUCCESS;

		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof MenuProvider provider && player instanceof ServerPlayer sp) {
			sp.openMenu(provider, pos);
			return InteractionResult.CONSUME;
		}

		return InteractionResult.PASS;
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
		BlockEntity be = worldIn.getBlockEntity(pos);
		return be instanceof MenuProvider mp ? mp : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new QuarryBlockEntity(pos, state);
	}

	// ✅ This is the ONLY ticker you want (server-side only)
	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide) return null;

		return type == CrystalnexusModBlockEntities.QUARRY.get()
			? (lvl, p, st, be) -> QuarryBlockEntity.tick(lvl, p, st, (QuarryBlockEntity) be)
			: null;
	}

	@Override
	public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
		super.triggerEvent(state, world, pos, eventID, eventParam);
		BlockEntity be = world.getBlockEntity(pos);
		return be != null && be.triggerEvent(eventID, eventParam);
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof QuarryBlockEntity qbe) {
				Containers.dropContents(world, pos, qbe);
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
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof QuarryBlockEntity qbe)
			return AbstractContainerMenu.getRedstoneSignalFromContainer(qbe);
		return 0;
	}
}
