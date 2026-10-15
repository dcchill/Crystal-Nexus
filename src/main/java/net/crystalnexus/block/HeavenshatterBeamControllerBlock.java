package net.crystalnexus.block;

import net.crystalnexus.block.entity.HeavenshatterBeamControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public final class HeavenshatterBeamControllerBlock extends Block implements EntityBlock {
	public HeavenshatterBeamControllerBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops());
		registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
	}

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(HorizontalDirectionalBlock.FACING); }
	@Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite()); }
	@Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(HorizontalDirectionalBlock.FACING, rotation.rotate(state.getValue(HorizontalDirectionalBlock.FACING))); }
	@Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING))); }

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HeavenshatterBeamControllerBlockEntity(pos, state);
	}

	@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
			Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide || type != CrystalnexusModBlockEntities.HEAVENSHATTER_BEAM_CONTROLLER.get()) return null;
		return (tickLevel, pos, tickState, entity) -> ((HeavenshatterBeamControllerBlockEntity) entity).serverTick();
	}

	@Override protected InteractionResult useWithoutItem(BlockState state, Level level,
			BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server
				&& level.getBlockEntity(pos) instanceof HeavenshatterBeamControllerBlockEntity controller) {
			controller.validateStructureNow();
			server.openMenu(controller, pos);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
		if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof HeavenshatterBeamControllerBlockEntity controller) {
			if (!level.isClientSide) net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), controller.meteorFilter().getStackInSlot(0));
			controller.onControllerRemoved();
		}
		super.onRemove(state, level, pos, next, moving);
	}
}
