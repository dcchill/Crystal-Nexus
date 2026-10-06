package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.PlasmaGeneratorControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.crystalnexus.energy.PlasmaGrid;

public final class PlasmaGeneratorMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final PlasmaGeneratorControllerBlockEntity controller;

    public PlasmaGeneratorMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, controllerAt(inventory, data.readBlockPos()));
    }

    public PlasmaGeneratorMenu(int id, Inventory inventory, PlasmaGeneratorControllerBlockEntity controller) {
        super(CrystalnexusModMenus.PLASMA_GENERATOR.get(), id);
        this.controller = controller;
        access = ContainerLevelAccess.create(inventory.player.level(),
            controller == null ? BlockPos.ZERO : controller.getBlockPos());
        Container container = controller == null ? new SimpleContainer(PlasmaGrid.SIZE) : controller;
        for (int row = 0; row < PlasmaGrid.ROWS; row++) for (int column = 0; column < PlasmaGrid.COLUMNS; column++)
            addSlot(new Slot(container, row * PlasmaGrid.COLUMNS + column, 8 + column * 18, 29 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return PlasmaGeneratorControllerBlockEntity.componentType(stack) != PlasmaGrid.EMPTY;
                }
                @Override public int getMaxStackSize() { return 1; }
            });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 143 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 201));
    }

    private static PlasmaGeneratorControllerBlockEntity controllerAt(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof PlasmaGeneratorControllerBlockEntity controller
            ? controller : null;
    }

    public PlasmaGeneratorControllerBlockEntity controller() { return controller; }
    @Override public boolean stillValid(Player player) {
        return controller != null && stillValid(access, player, CrystalnexusModBlocks.PLASMA_GENERATOR_CONTROLLER.get());
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < PlasmaGrid.SIZE ? !moveItemStackTo(stack, PlasmaGrid.SIZE, slots.size(), true)
            : !moveItemStackTo(stack, 0, PlasmaGrid.SIZE, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }
}
