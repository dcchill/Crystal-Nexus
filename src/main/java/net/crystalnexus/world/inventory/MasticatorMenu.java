package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.MasticatorBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MasticatorMenu extends AbstractContainerMenu {
	private final Container container;
    private final int machineSlots;
	private final ContainerLevelAccess access;
	private final ContainerData data;

	public MasticatorMenu(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, containerAt(inventory, data), new SimpleContainerData(2));
	}

	public MasticatorMenu(int id, Inventory inventory, Container container) {
		this(id, inventory, container, container instanceof MasticatorBlockEntity masticator
				? masticator.data() : new SimpleContainerData(2));
	}

	private MasticatorMenu(int id, Inventory inventory, Container container, ContainerData data) {
		super(CrystalnexusModMenus.MASTICATOR.get(), id);
		this.container = container;
		this.data = data;
		checkContainerSize(container, 4);
		this.access = container instanceof BlockEntity blockEntity
				? ContainerLevelAccess.create(inventory.player.level(), blockEntity.getBlockPos()) : ContainerLevelAccess.NULL;
		container.startOpen(inventory.player);

		addSlot(new Slot(container, 1, 80, 35) {
			@Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(1, stack); }
		});
		addSlot(new Slot(container, 2, 134, 26) {
			@Override public boolean mayPlace(ItemStack stack) { return false; }
		});

		addSlot(new Slot(container, 3, 134, 44) {
			@Override public boolean mayPlace(ItemStack stack) { return false; }
		});

        var upgradeState = container instanceof BlockEntity machine ? machine.getBlockState() : CrystalnexusModBlocks.MASTICATOR.get().defaultBlockState();
        var upgradeItems = new net.neoforged.neoforge.items.wrapper.InvWrapper(container);
        for (int i = 0; i < net.crystalnexus.processing.MachineTier.from(upgradeState).upgradeSlots(); i++)
            addSlot(new MachineUpgradeSlot(upgradeItems, 4 + i, upgradeState, i));
        machineSlots = slots.size();

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			addSlot(new Slot(inventory, column, 8 + column * 18, 142));
		}
		addDataSlots(data);
	}

	public int progress() { return data.get(0); }

	public int maxProgress() { return Math.max(1, data.get(1)); }

	private static Container containerAt(Inventory inventory, FriendlyByteBuf data) {
		BlockEntity blockEntity = inventory.player.level().getBlockEntity(data.readBlockPos());
		return blockEntity instanceof MasticatorBlockEntity masticator ? masticator : new SimpleContainer(10);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, CrystalnexusModBlocks.MASTICATOR.get());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < machineSlots) {
			if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
		} else if (net.crystalnexus.util.MachineUpgradeHelper.isMachineUpgrade(stack)) {
            boolean moved = false;
            for (int i = 3; i < machineSlots && !stack.isEmpty(); i++) moved |= moveItemStackTo(stack, i, i + 1, false);
            if (!moved) return ItemStack.EMPTY;
		} else {
			if (!stack.is(CrystalnexusModItems.PRISON_CUBE.get()) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
		return copy;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		container.stopOpen(player);
	}
}
