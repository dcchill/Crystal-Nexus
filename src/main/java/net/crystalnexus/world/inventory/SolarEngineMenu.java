package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.SolarEngineControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class SolarEngineMenu extends AbstractContainerMenu {
	private final ContainerLevelAccess access;
	private final SolarEngineControllerBlockEntity controller;

	public SolarEngineMenu(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, controllerAt(inventory, data.readBlockPos()));
	}

	public SolarEngineMenu(int id, Inventory inventory, SolarEngineControllerBlockEntity controller) {
		super(CrystalnexusModMenus.SOLAR_ENGINE.get(), id);
		this.controller = controller;
		access = ContainerLevelAccess.create(inventory.player.level(), controller == null ? BlockPos.ZERO : controller.getBlockPos());
		Container container = controller == null ? new SimpleContainer(1) : controller;
		addSlot(new Slot(container, 0, 80, 37));
		for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
			addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 99 + row * 18));
		for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 157));
	}

	private static SolarEngineControllerBlockEntity controllerAt(Inventory inventory, BlockPos pos) {
		return inventory.player.level().getBlockEntity(pos) instanceof SolarEngineControllerBlockEntity controller ? controller : null;
	}

	public SolarEngineControllerBlockEntity controller() { return controller; }
	@Override public boolean clickMenuButton(Player player, int extractionPercent) {
		if (controller == null || extractionPercent < 0 || extractionPercent > 100) return false;
		controller.setExtractionPercent(extractionPercent);
		return true;
	}
	@Override public boolean stillValid(Player player) {
		return controller != null && controller.isFormed()
			&& stillValid(access, player, CrystalnexusModBlocks.SOLAR_ENGINE_CONTROLLER.get());
	}
	@Override public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem(), copy = stack.copy();
		if (index == 0 ? !moveItemStackTo(stack, 1, slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
		return copy;
	}
}
