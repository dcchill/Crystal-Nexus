package net.crystalnexus.block.entity;

import io.netty.buffer.Unpooled;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.world.inventory.ControlRodGuiMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ReactorControlRodBlockEntity extends BlockEntity implements MenuProvider {
	private int insertion;

	public ReactorControlRodBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.REACTOR_CONTROL_ROD.get(), pos, state);
	}

	public int getInsertion() {
		return insertion;
	}

	public double getReactivity() {
		return reactivity(insertion);
	}

	public static double reactivity(int insertionPercent) {
		return 1.0 - Mth.clamp(insertionPercent, 0, 100) / 100.0;
	}

	public void setInsertion(int insertionPercent) {
		int next = Mth.clamp(insertionPercent, 0, 100);
		if (next == insertion) {
			return;
		}
		insertion = next;
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(tag, lookupProvider);
		insertion = Mth.clamp(tag.getInt("controlRodInsertion"), 0, 100);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(tag, lookupProvider);
		tag.putInt("controlRodInsertion", insertion);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return saveWithFullMetadata(lookupProvider);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("gui.crystalnexus.control_rod_gui.title");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new ControlRodGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(worldPosition));
	}
}
