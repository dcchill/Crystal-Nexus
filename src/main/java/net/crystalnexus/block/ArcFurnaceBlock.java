package net.crystalnexus.block;

import io.netty.buffer.Unpooled;
import net.crystalnexus.block.entity.ArcFurnaceBlockEntity;
import net.crystalnexus.procedures.ArcFurnaceOnTickUpdateProcedure;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.processing.TieredMachineBlock;
import net.crystalnexus.world.inventory.ArcFurnaceMenu;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ArcFurnaceBlock extends ChemicalReactionChamberBlock implements TieredMachineBlock {
	@Override public MachineTier machineTier() { return MachineTier.TUNGSTEN; }

	@Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		ArcFurnaceOnTickUpdateProcedure.execute(level, pos);
		level.scheduleTick(pos, this, 1);
	}

	@Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(new MenuProvider() {
			@Override public Component getDisplayName() { return Component.translatable("block.crystalnexus.arc_furnace"); }
			@Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
				return new ArcFurnaceMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
			}
		}, pos);
		return InteractionResult.SUCCESS;
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArcFurnaceBlockEntity(pos, state); }

	@Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
		if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof ArcFurnaceBlockEntity furnace) {
			furnace.onControllerRemoved();
			Containers.dropContents(level, pos, furnace);
		}
		super.onRemove(state, level, pos, next, moving);
	}
}
