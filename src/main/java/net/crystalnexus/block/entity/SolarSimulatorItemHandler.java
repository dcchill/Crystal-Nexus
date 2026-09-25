package net.crystalnexus.block.entity;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

/** Bridges four virtual Dyson reserves to bounded item transfers without exposing oversized stacks. */
public final class SolarSimulatorItemHandler implements IItemHandler {
    private final SolarSimulatorControllerBlockEntity controller;
    private final SidedInvWrapper normal;

    public SolarSimulatorItemHandler(SolarSimulatorControllerBlockEntity controller, Direction side) {
        this.controller = controller;
        this.normal = new SidedInvWrapper(controller, side);
    }

    @Override public int getSlots() { return normal.getSlots(); }
    @Override public ItemStack getStackInSlot(int slot) { return normal.getStackInSlot(slot); }
    @Override public int getSlotLimit(int slot) {
        return controller.isDysonMode() && slot >= 0 && slot < 4
            ? SolarSimulatorControllerBlockEntity.DYSON_SLOT_LIMIT : normal.getSlotLimit(slot);
    }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return normal.isItemValid(slot, stack); }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!controller.isDysonMode() || slot < 0 || slot >= 4) return normal.insertItem(slot, stack, simulate);
        int moved = controller.insertDyson(slot, stack, stack.getCount(), simulate);
        if (moved == 0) return stack;
        ItemStack remainder = stack.copy();
        remainder.shrink(moved);
        return remainder;
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return controller.isDysonMode() && slot >= 0 && slot < 4
            ? controller.extractDyson(slot, amount, simulate) : normal.extractItem(slot, amount, simulate);
    }
}
