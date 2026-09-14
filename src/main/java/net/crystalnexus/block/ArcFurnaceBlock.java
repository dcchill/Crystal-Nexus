package net.crystalnexus.block;

import io.netty.buffer.Unpooled;
import net.crystalnexus.block.entity.ArcFurnaceBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.processing.TieredMachineBlock;
import net.crystalnexus.world.inventory.ArcFurnaceMenu;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ArcFurnaceBlock extends ChemicalReactionChamberBlock implements TieredMachineBlock {
	private final MachineTier machineTier;
	private final int recipeTier;

	public ArcFurnaceBlock() {
		this(MachineTier.TUNGSTEN, 2);
	}

	public ArcFurnaceBlock(MachineTier machineTier, int recipeTier) {
		this.machineTier = machineTier;
		this.recipeTier = recipeTier;
	}

	@Override public MachineTier machineTier() { return machineTier; }
	public int recipeTier() { return recipeTier; }

	@Override @SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide || type != CrystalnexusModBlockEntities.ARC_FURNACE.get() ? null
				: (BlockEntityTicker<T>) (BlockEntityTicker<ArcFurnaceBlockEntity>) ArcFurnaceBlockEntity::tick;
	}

	@Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(new MenuProvider() {
			@Override public Component getDisplayName() {
				return Component.translatable(recipeTier == 1 ? "block.crystalnexus.azurine_blast_furnace" : "block.crystalnexus.arc_furnace");
			}
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
