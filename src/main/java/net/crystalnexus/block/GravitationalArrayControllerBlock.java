package net.crystalnexus.block;

import net.crystalnexus.block.entity.GravitationalArrayControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class GravitationalArrayControllerBlock extends Block implements EntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public GravitationalArrayControllerBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(2f, 25f).requiresCorrectToolForDrops());
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
	@Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
	@Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
	@Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof GravitationalArrayControllerBlockEntity controller)
				|| !controller.validateStructureNow()) {
			player.displayClientMessage(Component.translatable("message.crystalnexus.gravitational_array_incomplete"), true);
			return InteractionResult.FAIL;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
		serverPlayer.openMenu(controller, pos);
		return InteractionResult.CONSUME;
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GravitationalArrayControllerBlockEntity(pos, state); }

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide || type != net.crystalnexus.init.CrystalnexusModBlockEntities.GRAVITATIONAL_ARRAY_CONTROLLER.get()) return null;
		return (tickLevel, pos, tickState, blockEntity) ->
			((GravitationalArrayControllerBlockEntity) blockEntity).serverTick();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof GravitationalArrayControllerBlockEntity controller) {
			controller.onControllerRemoved();
			Containers.dropContents(level, pos, controller);
			level.updateNeighbourForOutputSignal(pos, this);
		}
		super.onRemove(state, level, pos, newState, moving);
	}
}
