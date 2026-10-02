package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.NeutronFluxChamberHatchBlockEntity;
import net.crystalnexus.item.DemonCoreItem;
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
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public final class NeutronFluxChamberHatchMenu extends AbstractContainerMenu {
	public final Player entity;
	public final int x, y, z;
	private final ContainerLevelAccess access;
	private final NeutronFluxChamberHatchBlockEntity hatch;
	public NeutronFluxChamberHatchBlockEntity hatch() { return hatch; }

	public NeutronFluxChamberHatchMenu(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, findHatch(inventory, data.readBlockPos()));
	}

	public NeutronFluxChamberHatchMenu(int id, Inventory inventory, NeutronFluxChamberHatchBlockEntity hatch) {
		super(CrystalnexusModMenus.NEUTRON_FLUX_CHAMBER_HATCH.get(), id);
		this.entity = inventory.player;
		this.hatch = hatch;
		BlockPos pos = hatch == null ? BlockPos.ZERO : hatch.getBlockPos();
		this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ();
		this.access = ContainerLevelAccess.create(entity.level(), pos);
		InvWrapper items = new InvWrapper(hatch == null ? new SimpleContainer(1) : hatch);
		addSlot(new net.neoforged.neoforge.items.SlotItemHandler(items, 0, 43, 35) {
			@Override public boolean mayPlace(ItemStack stack) { return DemonCoreItem.isClosed(stack); }
		});
		for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
			addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
		for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
	}

	private static NeutronFluxChamberHatchBlockEntity findHatch(Inventory inventory, BlockPos pos) {
		return inventory.player.level().getBlockEntity(pos) instanceof NeutronFluxChamberHatchBlockEntity hatch ? hatch : null;
	}

	@Override public boolean stillValid(Player player) {
		return hatch != null && hatch.isFormed() && AbstractContainerMenu.stillValid(access, player, hatch.getBlockState().getBlock());
	}

	@Override public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack original = slot.getItem(), copy = original.copy();
		if (index == 0) {
			if (!moveItemStackTo(original, 1, slots.size(), true)) return ItemStack.EMPTY;
		} else if (DemonCoreItem.isClosed(original)) {
			if (!moveItemStackTo(original, 0, 1, false)) return ItemStack.EMPTY;
		} else {
			return ItemStack.EMPTY;
		}
		if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
		if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, original);
		return copy;
	}
}