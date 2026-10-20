package net.crystalnexus.world.inventory;

import net.crystalnexus.util.MachineUpgradeHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class MachineUpgradeSlot extends SlotItemHandler {
    private final BlockState state;
    private final int ordinal;

    public MachineUpgradeSlot(IItemHandler items, int index, BlockState state, int ordinal) {
        this(items, index, state, ordinal, 84);
    }

    public MachineUpgradeSlot(IItemHandler items, int index, BlockState state, int ordinal, int inventoryY) {
        this(items, index, state, ordinal, MachineGuiLayout.upgradeSlot(ordinal));
    }

    private MachineUpgradeSlot(IItemHandler items, int index, BlockState state, int ordinal, int[] position) {
        this(items, index, state, ordinal, position[0], position[1]);
    }

    public MachineUpgradeSlot(IItemHandler items, int index, BlockState state, int ordinal, int x, int y) {
        super(items, index, x, y);
        this.state = state;
        this.ordinal = ordinal;
    }

    @Override public boolean isActive() { return ordinal < MachineUpgradeHelper.upgradeSlots(state); }
    @Override public int getMaxStackSize(ItemStack stack) {
        return Math.min(MachineUpgradeHelper.upgradeStackLimit(stack), super.getMaxStackSize(stack));
    }
    @Override public boolean mayPlace(ItemStack stack) {
        return MachineUpgradeHelper.acceptsUpgrade(state, ordinal, stack) && super.mayPlace(stack);
    }
}
