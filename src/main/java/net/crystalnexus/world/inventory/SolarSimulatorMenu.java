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
import net.minecraft.world.inventory.ClickType;
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
        addSlot(new Slot(container, 0, 53, 37) { @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(0, stack); } });
        addSlot(new Slot(container, 1, 107, 37) { @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(1, stack); } });
        addSlot(new Slot(container, 2, 80, 11) { @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(2, stack); } });
        addSlot(new Slot(container, 3, 80, 63) { @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(3, stack); } });
        addSlot(new Slot(container, 4, 80, 37) { @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(4, stack); } });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 99 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 157));
    }
    private static SolarSimulatorControllerBlockEntity controllerAt(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof SolarSimulatorControllerBlockEntity controller ? controller : null;
    }
    public SolarSimulatorControllerBlockEntity controller() { return controller; }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || !stillValid(player)) return false;
        if (player.level().isClientSide) return true; 
        return controller.setDysonMode(!controller.isDysonMode());
    }
    @Override public boolean stillValid(Player player) {
        return controller != null && player.level().getBlockEntity(controller.getBlockPos()) == controller
            && stillValid(access, player, CrystalnexusModBlocks.SOLAR_SIMULATOR_CONTROLLER.get());
    }
    @Override public boolean canDragTo(Slot slot) {
        return !(controller != null && controller.isDysonMode() && slots.indexOf(slot) < SolarSimulatorControllerBlockEntity.DYSON_SUPPLY_SLOTS) && super.canDragTo(slot);
    }
    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        if (controller == null || !stillValid(player)) return;
        if (controller.isDysonMode() && slotId >= 0 && slotId < SolarSimulatorControllerBlockEntity.DYSON_SUPPLY_SLOTS) {
            if (type == ClickType.QUICK_CRAFT || type == ClickType.PICKUP_ALL) return;
            if (type == ClickType.QUICK_MOVE) { quickMoveStack(player, slotId); return; }
            if (type != ClickType.PICKUP || button < 0 || button > 1) return;
            ItemStack carried = getCarried();
            if (!carried.isEmpty()) {
                carried.shrink(controller.insertDyson(slotId, carried, button == 1 ? 1 : carried.getCount(), false));
                setCarried(carried);
            } else setCarried(controller.extractDyson(slotId, button == 1 ? 1 : Integer.MAX_VALUE, false));
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, type, player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || controller == null || !stillValid(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        if (controller.isDysonMode()) {
            if (index < SolarSimulatorControllerBlockEntity.DYSON_SUPPLY_SLOTS) {
                ItemStack preview = controller.extractDyson(index, Integer.MAX_VALUE, true);
                if (preview.isEmpty() || !moveItemStackTo(preview, 5, slots.size(), true)) return ItemStack.EMPTY;
                int moved = Math.min(controller.getDysonCount(index), slot.getItem().getMaxStackSize()) - preview.getCount();
                return controller.extractDyson(index, moved, false);
            }
            if (index == SolarSimulatorControllerBlockEntity.DYSON_REPAIR_SLOT) {
                ItemStack stack = slot.getItem(), copy = stack.copy();
                if (!moveItemStackTo(stack, 5, slots.size(), true)) return ItemStack.EMPTY;
                if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
                return copy;
            }
            if (index >= 5 && controller.canPlaceItem(SolarSimulatorControllerBlockEntity.DYSON_REPAIR_SLOT, slot.getItem())) {
                ItemStack input = slot.getItem(), original = input.copy();
                if (!moveItemStackTo(input, SolarSimulatorControllerBlockEntity.DYSON_REPAIR_SLOT,
                    SolarSimulatorControllerBlockEntity.DYSON_REPAIR_SLOT + 1, false)) return ItemStack.EMPTY;
                if (input.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
                return original;
            }
            if (index >= 5 && controller.canPlaceItem(0, slot.getItem())) {
                ItemStack input = slot.getItem(), original = input.copy();
                for (int i = 0; i < SolarSimulatorControllerBlockEntity.DYSON_SUPPLY_SLOTS && !input.isEmpty(); i++)
                    if (!controller.getItem(i).isEmpty() && ItemStack.isSameItemSameComponents(controller.getItem(i), input))
                        input.shrink(controller.insertDyson(i, input, input.getCount(), false));
                for (int i = 0; i < SolarSimulatorControllerBlockEntity.DYSON_SUPPLY_SLOTS && !input.isEmpty(); i++)
                    if (controller.getItem(i).isEmpty()) input.shrink(controller.insertDyson(i, input, input.getCount(), false));
                if (input.getCount() == original.getCount()) return ItemStack.EMPTY;
                if (input.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
                return original;
            }
            ItemStack stack = slot.getItem(), copy = stack.copy();
            if (index == 4 ? !moveItemStackTo(stack, 5, slots.size(), true)
                : !moveItemStackTo(stack, SolarSimulatorControllerBlockEntity.DYSON_REPAIR_SLOT,
                    SolarSimulatorControllerBlockEntity.DYSON_REPAIR_SLOT + 1, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            return copy;
        }
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < 5 ? !moveItemStackTo(stack, 5, slots.size(), true) : !moveItemStackTo(stack, 0, 5, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
