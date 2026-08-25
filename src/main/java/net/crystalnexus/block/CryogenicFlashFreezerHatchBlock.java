package net.crystalnexus.block;

import io.netty.buffer.Unpooled;
import net.crystalnexus.block.entity.CryogenicFlashFreezerBlockEntity;
import net.crystalnexus.world.inventory.CryogenicFlashFreezerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public final class CryogenicFlashFreezerHatchBlock extends Block implements EntityBlock {
	public CryogenicFlashFreezerHatchBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(4f).requiresCorrectToolForDrops());
		registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HorizontalDirectionalBlock.FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(state, level, pos, oldState, moving);
		level.scheduleTick(pos, this, 1);
	}

	@Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof CryogenicFlashFreezerBlockEntity freezer) freezer.serverTick();
		level.scheduleTick(pos, this, 1);
	}

	@Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(new MenuProvider() {
			@Override public Component getDisplayName() { return Component.translatable("block.crystalnexus.cryogenic_flash_freezer_hatch"); }
			@Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
				return new CryogenicFlashFreezerMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
			}
		}, pos);
		return InteractionResult.SUCCESS;
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CryogenicFlashFreezerBlockEntity(pos, state);
	}

	@Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
		if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof CryogenicFlashFreezerBlockEntity freezer) {
			freezer.onControllerRemoved();
			Containers.dropContents(level, pos, freezer);
		}
		super.onRemove(state, level, pos, next, moving);
	}
}
