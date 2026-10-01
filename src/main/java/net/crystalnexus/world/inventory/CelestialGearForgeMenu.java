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
	public static final int[][] SLOT_POSITIONS = {{45, 11}, {70, 13}, {72, 37}, {70, 60},
		{45, 63}, {21, 60}, {18, 37}, {21, 13}, {45, 37}, {134, 37}};
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
		container = forge == null ? new SimpleContainer(CelestialGearForgeBlockEntity.OUTPUT_SLOT + 1) : forge;
		data = forge == null ? new SimpleContainerData(CelestialGearForgeBlockEntity.DATA_COUNT) : forge.data();
		access = ContainerLevelAccess.create(inventory.player.level(), forge == null ? BlockPos.ZERO : forge.getBlockPos());
		addDataSlots(data);
		for (int index = 0; index < CelestialGearForgeBlockEntity.INPUT_COUNT; index++)
			addSlot(input(index, SLOT_POSITIONS[index][0], SLOT_POSITIONS[index][1]));
		addSlot(new Slot(container, CelestialGearForgeBlockEntity.OUTPUT_SLOT, 134, 37) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
		for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
			addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 111 + row * 18));
		for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 169));
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
	public int selectedEnchantmentIndex() { return data.get(3); }
	public int energyStored() { return (data.get(4) & 0xffff) | (data.get(5) & 0xffff) << 16; }
	public int energyPerTick() { return (data.get(6) & 0xffff) | (data.get(7) & 0xffff) << 16; }
	@Override public boolean clickMenuButton(Player player, int id) {
		return player.containerMenu == this && stillValid(player) && (id == 0 || id == 1)
			&& forge.cycleEnchantment(id == 0 ? -1 : 1);
	}
	@Override public boolean stillValid(Player player) {
		return forge != null && stillValid(access, player, CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get());
	}

	@Override public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem(), original = stack.copy();
		int inventoryStart = CelestialGearForgeBlockEntity.OUTPUT_SLOT + 1;
		if (index < inventoryStart) {
			if (!moveItemStackTo(stack, inventoryStart, slots.size(), true)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, CelestialGearForgeBlockEntity.INPUT_COUNT, false)) return ItemStack.EMPTY;
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
		if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, stack);
		return original;
	}
}
