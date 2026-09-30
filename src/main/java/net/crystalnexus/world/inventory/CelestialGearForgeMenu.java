package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.CelestialGearForgeBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class CelestialGearForgeMenu extends AbstractContainerMenu {
	private final CelestialGearForgeBlockEntity forge;
	private final Container container;
	private final ContainerData data;
	private final ContainerLevelAccess access;

	public CelestialGearForgeMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
		this(id, inventory, at(inventory, buffer != null && buffer.readableBytes() >= Long.BYTES ? buffer.readBlockPos() : BlockPos.ZERO));
	}

	public CelestialGearForgeMenu(int id, Inventory inventory, CelestialGearForgeBlockEntity forge) {
		super(CrystalnexusModMenus.CELESTIAL_GEAR_FORGE.get(), id);
		this.forge = forge;
		container = forge == null ? new SimpleContainer(6) : forge;
		data = forge == null ? new SimpleContainerData(3) : forge.data();
		access = ContainerLevelAccess.create(inventory.player.level(), forge == null ? BlockPos.ZERO : forge.getBlockPos());
		addDataSlots(data);
		addSlot(input(0, 80, 28));
		addSlot(input(1, 53, 55));
		addSlot(input(2, 80, 55));
		addSlot(input(3, 107, 55));
		addSlot(input(4, 80, 82));
		addSlot(new Slot(container, 5, 151, 55) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
		for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
			addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 130 + row * 18));
		for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 188));
	}

	private Slot input(int index, int x, int y) {
		return new Slot(container, index, x, y) {
			@Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(index, stack); }
		};
	}

	private static CelestialGearForgeBlockEntity at(Inventory inventory, BlockPos pos) {
		return inventory.player.level().getBlockEntity(pos) instanceof CelestialGearForgeBlockEntity forge ? forge : null;
	}

	public CelestialGearForgeBlockEntity forge() { return forge; }
	public int progress() { return data.get(0); }
	public int duration() { return data.get(1); }
	public boolean formed() { return data.get(2) != 0; }
	@Override public boolean stillValid(Player player) {
		return forge != null && stillValid(access, player, CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get());
	}

	@Override public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem(), original = stack.copy();
		if (index < 6) {
			if (!moveItemStackTo(stack, 6, slots.size(), true)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, 5, false)) return ItemStack.EMPTY;
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
		if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, stack);
		return original;
	}
}
