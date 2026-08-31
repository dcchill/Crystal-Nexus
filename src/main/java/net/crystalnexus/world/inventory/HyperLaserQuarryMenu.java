package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.QuarryBlockEntity;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;

public final class HyperLaserQuarryMenu extends AbstractContainerMenu {
	public final Level world;
	public final Player entity;
	public final int x, y, z;
	private final Container container;
	private final ContainerData data;

	public HyperLaserQuarryMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
		this(id, inventory, findQuarry(inventory, buffer.readBlockPos()), null);
	}

	public HyperLaserQuarryMenu(int id, Inventory inventory, QuarryBlockEntity quarry, ContainerData data) {
		this(id, inventory, (Container) quarry, data);
	}

	private HyperLaserQuarryMenu(int id, Inventory inventory, Container container, ContainerData serverData) {
		super(CrystalnexusModMenus.HYPER_LASER_QUARRY.get(), id);
		this.container = container;
		this.entity = inventory.player;
		this.world = inventory.player.level();
		BlockPos pos = container instanceof BlockEntity blockEntity ? blockEntity.getBlockPos() : BlockPos.ZERO;
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
		this.data = serverData == null ? new SimpleContainerData(5) : serverData;
		checkContainerSize(container, 10);
		checkContainerDataCount(this.data, 5);
		container.startOpen(inventory.player);

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				addSlot(new Slot(container, column + row * 3, 142 + column * 18, 25 + row * 18) {
					@Override public boolean mayPlace(ItemStack stack) { return false; }
				});
			}
		}
		addSlot(new Slot(container, 9, 215, 25) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(ItemTags.create(ResourceLocation.parse("crystalnexus:machine_upgrades")));
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlot(new Slot(inventory, column + row * 9 + 9, 43 + column * 18, 108 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 43 + column * 18, 166));
		addDataSlots(this.data);
	}

	private static Container findQuarry(Inventory inventory, BlockPos pos) {
		BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
		return blockEntity instanceof QuarryBlockEntity quarry && quarry.isHyper() ? quarry : new SimpleContainer(10);
	}

	public int selectionWidth() { return data.get(0); }
	public int selectionDepth() { return data.get(1); }
	public int currentY() { return data.get(2); }
	public int bufferedSlots() { return data.get(3); }
	public int status() { return data.get(4); }

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(container instanceof QuarryBlockEntity quarry)) return false;
		switch (id) {
			case 0 -> quarry.resizeSelection(-1, 0);
			case 1 -> quarry.resizeSelection(1, 0);
			case 2 -> quarry.resizeSelection(0, -1);
			case 3 -> quarry.resizeSelection(0, 1);
			default -> { return false; }
		}
		return true;
	}

	@Override public boolean stillValid(Player player) { return container.stillValid(player); }

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack original = slot.getItem();
		ItemStack copy = original.copy();
		if (index < 10) {
			if (!moveItemStackTo(original, 10, slots.size(), true)) return ItemStack.EMPTY;
		} else if (original.is(ItemTags.create(ResourceLocation.parse("crystalnexus:machine_upgrades")))) {
			if (!moveItemStackTo(original, 9, 10, false)) return ItemStack.EMPTY;
		} else return ItemStack.EMPTY;
		if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
		if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, original);
		return copy;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		container.stopOpen(player);
	}
}
