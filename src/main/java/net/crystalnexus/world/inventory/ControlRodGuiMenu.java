package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.ReactorControlRodBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ControlRodGuiMenu extends AbstractContainerMenu {
	public final Level world;
	public final BlockPos pos;
	private final ContainerLevelAccess access;
	private final ReactorControlRodBlockEntity controlRod;
	private int insertion;

	public ControlRodGuiMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
		super(CrystalnexusModMenus.CONTROL_ROD_GUI.get(), id);
		world = inventory.player.level();
		pos = extraData.readBlockPos();
		access = ContainerLevelAccess.create(world, pos);
		controlRod = world.getBlockEntity(pos) instanceof ReactorControlRodBlockEntity rod ? rod : null;
		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return controlRod == null ? insertion : controlRod.getInsertion();
			}

			@Override
			public void set(int value) {
				insertion = Mth.clamp(value, 0, 100);
			}
		});
	}

	public int getInsertion() {
		return insertion;
	}

	public void setClientInsertion(int insertionPercent) {
		insertion = Mth.clamp(insertionPercent, 0, 100);
	}

	@Override
	public boolean clickMenuButton(Player player, int insertionPercent) {
		if (controlRod == null || insertionPercent < 0 || insertionPercent > 100) {
			return false;
		}
		controlRod.setInsertion(insertionPercent);
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return AbstractContainerMenu.stillValid(access, player, CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}
}
