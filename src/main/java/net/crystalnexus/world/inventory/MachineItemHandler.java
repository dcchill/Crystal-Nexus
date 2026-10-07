package net.crystalnexus.world.inventory;

import net.crystalnexus.util.MachineItemStorage;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/** Unsided menu access, including inputs which automation cannot extract. */
public final class MachineItemHandler extends InvWrapper {
    public MachineItemHandler(Container inventory) { super(inventory); }

    @Override public int getSlotLimit(int slot) {
        return MachineItemStorage.isTiered(getInv()) ? MachineItemStorage.slotLimit(getInv(), slot) : super.getSlotLimit(slot);
    }

    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!MachineItemStorage.isTiered(getInv()) || stack.isEmpty()) return super.insertItem(slot, stack, simulate);
        if (!getInv().canPlaceItem(slot, stack)) return stack;
        return MachineItemStorage.insert(getInv(), slot, stack, simulate);
    }

    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return super.extractItem(slot, Math.min(Math.max(0, amount), getStackInSlot(slot).getMaxStackSize()), simulate);
    }
}
