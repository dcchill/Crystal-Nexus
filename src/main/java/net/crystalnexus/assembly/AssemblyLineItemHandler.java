package net.crystalnexus.assembly;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

/** Processing uses setStackInSlot; external insertion/extraction is locked during an assignment. */
public final class AssemblyLineItemHandler extends SidedInvWrapper {
    private final BlockEntity machine;
    public AssemblyLineItemHandler(BlockEntity be, Direction side) { super((WorldlyContainer) be, side); machine = be; }
    private boolean locked() { return machine.getPersistentData().contains(AssemblyLineMachine.OWNER); }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return locked() ? stack : super.insertItem(slot, stack, simulate); }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return locked() ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate); }
}
