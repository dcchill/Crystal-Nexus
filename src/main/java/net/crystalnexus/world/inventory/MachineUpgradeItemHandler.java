package net.crystalnexus.world.inventory;

import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.util.MachineItemStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

public class MachineUpgradeItemHandler extends SidedInvWrapper {
    private final int firstUpgrade;
    private final int extraStart;

    public MachineUpgradeItemHandler(WorldlyContainer inventory, @Nullable Direction side, int firstUpgrade, int extraStart) {
        super(inventory, side);
        this.firstUpgrade = firstUpgrade;
        this.extraStart = extraStart;
    }

    private boolean isUpgradeSlot(int slot) {
        int index = getSlot(inv, slot, side);
        return index >= 0 && (index == firstUpgrade || index >= extraStart);
    }

    @Override public int getSlotLimit(int slot) {
        if (MachineItemStorage.isTiered(inv) && !isUpgradeSlot(slot))
            return MachineItemStorage.slotLimit(inv, getSlot(inv, slot, side));
        return isUpgradeSlot(slot) && !getStackInSlot(slot).isEmpty()
            ? Math.min(super.getSlotLimit(slot), MachineUpgradeHelper.upgradeStackLimit(getStackInSlot(slot)))
            : super.getSlotLimit(slot);
    }

    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (MachineItemStorage.isTiered(inv) && !stack.isEmpty() && !isUpgradeSlot(slot)) {
            int index = getSlot(inv, slot, side);
            if (index < 0 || !inv.canPlaceItem(index, stack) || !inv.canPlaceItemThroughFace(index, stack, side)) return stack;
            return MachineItemStorage.insert(inv, index, stack, simulate);
        }
        if (!isUpgradeSlot(slot) || stack.isEmpty()) return super.insertItem(slot, stack, simulate);
        int room = MachineUpgradeHelper.upgradeStackLimit(stack) - getStackInSlot(slot).getCount();
        int offered = Math.min(stack.getCount(), Math.max(0, room));
        if (offered == 0) return stack;
        ItemStack rejected = super.insertItem(slot, stack.copyWithCount(offered), simulate);
        return stack.copyWithCount(stack.getCount() - offered + rejected.getCount());
    }

    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return super.extractItem(slot, Math.min(Math.max(0, amount), getStackInSlot(slot).getMaxStackSize()), simulate);
    }
}
