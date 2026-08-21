package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.SolarSimulatorControllerBlockEntity;
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

public final class SolarSimulatorMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final SolarSimulatorControllerBlockEntity controller;

    public SolarSimulatorMenu(int id, Inventory inventory, FriendlyByteBuf data) { this(id, inventory, controllerAt(inventory, data.readBlockPos())); }
    public SolarSimulatorMenu(int id, Inventory inventory, SolarSimulatorControllerBlockEntity controller) {
        super(CrystalnexusModMenus.SOLAR_SIMULATOR.get(), id);
        this.controller = controller;
        access = ContainerLevelAccess.create(inventory.player.level(), controller == null ? BlockPos.ZERO : controller.getBlockPos());
        Container container = controller == null ? new SimpleContainer(5) : controller;
        addSlot(new Slot(container, 0, 53, 37));
        addSlot(new Slot(container, 1, 107, 37));
        addSlot(new Slot(container, 2, 80, 11));
        addSlot(new Slot(container, 3, 80, 63));
        addSlot(new Slot(container, 4, 80, 37));
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 99 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 157));
    }
    private static SolarSimulatorControllerBlockEntity controllerAt(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof SolarSimulatorControllerBlockEntity controller ? controller : null;
    }
    public SolarSimulatorControllerBlockEntity controller() { return controller; }
    @Override public boolean stillValid(Player player) { return controller != null && controller.isFormed() && stillValid(access, player, CrystalnexusModBlocks.SOLAR_SIMULATOR_CONTROLLER.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < 5 ? !moveItemStackTo(stack, 5, slots.size(), true) : !moveItemStackTo(stack, 0, 5, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
