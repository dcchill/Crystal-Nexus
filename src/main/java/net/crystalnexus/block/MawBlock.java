package net.crystalnexus.block;

import net.crystalnexus.block.entity.MawBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MawBlock extends Block implements EntityBlock {
	public MawBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.MUD).strength(1.5f, 10f).requiresCorrectToolForDrops());
    }

	@Override
	public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return 15;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MawBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide || type != CrystalnexusModBlockEntities.MAW.get() ? null
				: (BlockEntityTicker<T>) (BlockEntityTicker<MawBlockEntity>) MawBlockEntity::tick;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MawBlockEntity blockEntity) {
			serverPlayer.openMenu(blockEntity, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof MawBlockEntity blockEntity) {
			Containers.dropContents(level, pos, blockEntity);
			level.updateNeighbourForOutputSignal(pos, this);
		}
		super.onRemove(state, level, pos, newState, moving);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof MawBlockEntity blockEntity
				? AbstractContainerMenu.getRedstoneSignalFromContainer(blockEntity) : 0;
	}
}
