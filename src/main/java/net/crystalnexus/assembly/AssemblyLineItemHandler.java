package net.crystalnexus.assembly;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;


public final class AssemblyLineItemHandler extends net.crystalnexus.world.inventory.MachineUpgradeItemHandler {
    private final BlockEntity machine;
    public AssemblyLineItemHandler(BlockEntity be, Direction side) {
        super((WorldlyContainer) be, side, be instanceof net.crystalnexus.block.entity.CircuitPressBlockEntity ? 3 : 2,
            be instanceof net.crystalnexus.block.entity.CircuitPressBlockEntity ? 4
                : be instanceof net.crystalnexus.block.entity.CrystalCrusherBlockEntity ? 3 : Integer.MAX_VALUE);
        machine = be;
    }
    private boolean locked() { return machine.getPersistentData().contains(AssemblyLineMachine.OWNER); }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return locked() ? stack : super.insertItem(slot, stack, simulate); }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return locked() ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate); }
}
