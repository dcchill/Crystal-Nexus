package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.CryogenicFlashFreezerBlockEntity;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public final class CryogenicFlashFreezerMenu extends AbstractContainerMenu {
	public final Player entity;
	public final int x, y, z;
	private final ContainerLevelAccess access;
	private final CryogenicFlashFreezerBlockEntity freezer;

	public CryogenicFlashFreezerMenu(int id, Inventory inventory, FriendlyByteBuf data) {
		super(CrystalnexusModMenus.CRYOGENIC_FLASH_FREEZER.get(), id);
		entity = inventory.player;
		BlockPos pos = data.readBlockPos();
		x = pos.getX(); y = pos.getY(); z = pos.getZ();
		access = ContainerLevelAccess.create(entity.level(), pos);
		freezer = entity.level().getBlockEntity(pos) instanceof CryogenicFlashFreezerBlockEntity be ? be : null;
		InvWrapper items = new InvWrapper(freezer == null ? new SimpleContainer(2) : freezer);
		addSlot(new SlotItemHandler(items, 0, 50, 54));
		addSlot(new SlotItemHandler(items, 1, 113, 54) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
		for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
			addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 84 + row * 18));
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
	}

	public CryogenicFlashFreezerBlockEntity freezer() { return freezer; }

	@Override public boolean stillValid(Player player) {
		return freezer != null && AbstractContainerMenu.stillValid(access, player, freezer.getBlockState().getBlock());
	}

	@Override public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack original = slot.getItem(), copy = original.copy();
		if (index < 2) {
			if (!moveItemStackTo(original, 2, slots.size(), true)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(original, 0, 1, false)) return ItemStack.EMPTY;
		if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
		if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, original);
		return copy;
	}
}
