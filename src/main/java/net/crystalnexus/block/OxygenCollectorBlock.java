package net.crystalnexus.block;

import io.netty.buffer.Unpooled;
import net.crystalnexus.block.entity.OxygenCollectorBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.world.inventory.NodeExtractorGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

public class OxygenCollectorBlock extends Block implements EntityBlock {
	public OxygenCollectorBlock() { super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5f, 8f).requiresCorrectToolForDrops()); }
	@Override @SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide || type != CrystalnexusModBlockEntities.OXYGEN_COLLECTOR.get() ? null
				: (BlockEntityTicker<T>) (BlockEntityTicker<OxygenCollectorBlockEntity>) OxygenCollectorBlockEntity::tick;
	}
	@Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(new MenuProvider() {
			@Override public Component getDisplayName() { return Component.translatable("block.crystalnexus.atmosphere_collector"); }
			@Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) { return new NodeExtractorGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos)); }
		}, pos);
		return InteractionResult.SUCCESS;
	}
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new OxygenCollectorBlockEntity(pos, state); }
	@Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
		if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof OxygenCollectorBlockEntity collector) Containers.dropContents(level, pos, collector);
		super.onRemove(state, level, pos, replacement, moving);
	}
}
