package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.ArcFurnaceBlockEntity;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ArcFurnaceMenu extends TieredMachineMenu {
    private final int machineSlots;
	public final Player entity;
	public final int x, y, z;
	private final ContainerLevelAccess access;
	private final ArcFurnaceBlockEntity furnace;

	public ArcFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf data) {
		super(CrystalnexusModMenus.ARC_FURNACE.get(), id);
		entity = inventory.player;
		BlockPos pos = data.readBlockPos();
		x = pos.getX(); y = pos.getY(); z = pos.getZ();
		access = ContainerLevelAccess.create(entity.level(), pos);
		furnace = entity.level().getBlockEntity(pos) instanceof ArcFurnaceBlockEntity be ? be : null;
		MachineItemHandler items = new MachineItemHandler(furnace == null ? new SimpleContainer(9) : furnace);
		addSlot(new MachineItemSlot(items, 0, 44, 35));
		addSlot(new MachineItemSlot(items, 1, 26, 35));
		addSlot(new MachineItemSlot(items, 2, 115, 35) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
		var upgradeState = entity.level().getBlockState(pos);
        addSlot(new MachineUpgradeSlot(items, 3, upgradeState, 0));
        for (int i = 1; i < net.crystalnexus.processing.MachineTier.from(upgradeState).upgradeSlots(); i++)
            addSlot(new MachineUpgradeSlot(items, 4 + i - 1, upgradeState, i));
        machineSlots = slots.size();
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 84 + row * 18));
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
	}

	@Override public boolean stillValid(Player player) {
		return furnace != null && AbstractContainerMenu.stillValid(access, player, furnace.getBlockState().getBlock());
	}

	@Override public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack original = slot.getItem(), copy = original.copy();
		if (index < machineSlots) {
			if (!moveItemStackTo(original, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
		} else if (net.crystalnexus.util.MachineUpgradeHelper.isMachineUpgrade(original)) {
                boolean moved = false;
                for (int i = 0; i < machineSlots && !original.isEmpty(); i++)
                    if (slots.get(i) instanceof MachineUpgradeSlot) moved |= moveItemStackTo(original, i, i + 1, false);
                if (!moved) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(original, 0, 3, false)) return ItemStack.EMPTY;
		if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
		if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, original);
		return copy;
	}
}
