package net.crystalnexus.world.inventory;

import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class MachineUpgradeSlot extends SlotItemHandler {
    private final BlockState state;
    private final int ordinal;

    public MachineUpgradeSlot(IItemHandler items, int index, BlockState state, int ordinal) {
        super(items, index, 180 + ordinal / 5 * 32, 8 + ordinal % 5 * 32);
        this.state = state;
        this.ordinal = ordinal;
    }

    @Override public boolean isActive() { return ordinal < MachineTier.from(state).upgradeSlots(); }
    @Override public boolean mayPlace(ItemStack stack) {
        return MachineUpgradeHelper.acceptsUpgrade(state, ordinal, stack) && super.mayPlace(stack);
    }
}
