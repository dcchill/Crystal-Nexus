package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.HemolyzerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class HemolyzerMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    public HemolyzerMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data != null && data.readableBytes() >= Long.BYTES ? at(inventory, data) : new SimpleContainer(1));
    }
    public HemolyzerMenu(int id, Inventory inventory, Container container) {
        super(CrystalnexusModMenus.HEMOLYZER.get(), id);
        this.container = container;
        this.data = container instanceof HemolyzerBlockEntity hemolyzer ? hemolyzer.data() : new SimpleContainerData(2);
        checkContainerSize(container, 1);
        addDataSlots(this.data);
        access = container instanceof BlockEntity blockEntity ? ContainerLevelAccess.create(inventory.player.level(), blockEntity.getBlockPos()) : ContainerLevelAccess.NULL;
        container.startOpen(inventory.player);
        addSlot(new Slot(container, 0, 80, 35) { @Override public boolean mayPlace(ItemStack stack) { return container instanceof HemolyzerBlockEntity hemolyzer && HemolyzerBlockEntity.accepts(inventory.player.level(), stack); } });
        addPlayerSlots(inventory);
    }
    private static Container at(Inventory inventory, FriendlyByteBuf data) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(data.readBlockPos());
        return blockEntity instanceof HemolyzerBlockEntity hemolyzer ? hemolyzer : new SimpleContainer(1);
    }
    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
    }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, CrystalnexusModBlocks.HEMOLYZER.get()); }
    public int progress() { return data.get(0); }
    public int maxProgress() { return Math.max(1, data.get(1)); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index == 0 ? !moveItemStackTo(stack, 1, slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
}
