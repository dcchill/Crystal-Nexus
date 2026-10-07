package net.crystalnexus.world.inventory;

import net.crystalnexus.util.MachineItemStorage;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MachineItemSlot extends SlotItemHandler {
    public MachineItemSlot(IItemHandler items, int slot, int x, int y) { super(items, slot, x, y); }

    @Override public int getMaxStackSize(ItemStack stack) {
        return getItemHandler() instanceof MachineItemHandler items
            ? MachineItemStorage.slotLimit(items.getInv(), index, stack) : super.getMaxStackSize(stack);
    }

    @Override public ItemStack remove(int amount) {
        return super.remove(Math.min(Math.max(0, amount), getItem().getMaxStackSize()));
    }
}
