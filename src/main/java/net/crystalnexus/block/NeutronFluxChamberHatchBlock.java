package net.crystalnexus.block;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.crystalnexus.block.entity.NeutronFluxChamberHatchBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.world.inventory.NeutronFluxChamberHatchMenu;
import io.netty.buffer.Unpooled;

public final class NeutronFluxChamberHatchBlock extends Block implements EntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public NeutronFluxChamberHatchBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(4f).requiresCorrectToolForDrops());
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer
				&& level.getBlockEntity(pos) instanceof NeutronFluxChamberHatchBlockEntity chamber) {
			if (!chamber.validateStructureNow()) {
				serverPlayer.displayClientMessage(Component.literal(chamber.getStructureError()), false);
				return InteractionResult.SUCCESS;
			}
			serverPlayer.openMenu(new MenuProvider() {
				@Override public Component getDisplayName() { return Component.translatable("block.crystalnexus.neutron_flux_chamber_hatch"); }
				@Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
					return new NeutronFluxChamberHatchMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
				}
			}, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new NeutronFluxChamberHatchBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide || type != CrystalnexusModBlockEntities.NEUTRON_FLUX_CHAMBER_HATCH.get() ? null
			: (BlockEntityTicker<T>) (BlockEntityTicker<NeutronFluxChamberHatchBlockEntity>) NeutronFluxChamberHatchBlockEntity::tick;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof NeutronFluxChamberHatchBlockEntity hatch) {
			hatch.onControllerRemoved();
			Containers.dropContents(level, pos, hatch);
		}
		super.onRemove(state, level, pos, newState, moving);
	}

}