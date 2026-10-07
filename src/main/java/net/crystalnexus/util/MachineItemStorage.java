package net.crystalnexus.util;

import net.crystalnexus.block.entity.*;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.processing.TieredMachineBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Full counts stay inside machines; serialized stacks and extracted stacks remain ordinary items. */
public final class MachineItemStorage {
    private static final String COUNTS = "MachineItemCounts";
    private MachineItemStorage() {}

    public static boolean isTiered(Container inventory) {
        return inventory instanceof BlockEntity machine && machine.getBlockState().getBlock() instanceof TieredMachineBlock;
    }

    public static boolean isMaterialSlot(Container inventory, int slot) {
        if (!isTiered(inventory) || slot < 0 || slot >= inventory.getContainerSize()) return false;
        if (inventory instanceof CraftingFactoryBlockEntity) return slot < 10;
        if (inventory instanceof MasticatorBlockEntity) return slot == 0 || slot == 2 || slot == 3;
        if (inventory instanceof ExtractinatorBlockEntity) return slot < 7;
        if (inventory instanceof DustSeparatorBlockEntity) return slot == 0 || slot == 1 || slot == 3;
        if (inventory instanceof CircuitPressBlockEntity || inventory instanceof ArcFurnaceBlockEntity) return slot < 3;
        return slot < 2;
    }

    public static int capacity(Container inventory, ItemStack stack) {
        if (!isTiered(inventory)) return inventory.getMaxStackSize(stack);
        return stack.getMaxStackSize() <= 1 ? stack.getMaxStackSize()
            : MachineTier.from(((BlockEntity) inventory).getBlockState()).itemSlotCapacity();
    }

    public static int slotLimit(Container inventory, int slot, ItemStack stack) {
        if (isMaterialSlot(inventory, slot)) return capacity(inventory, stack);
        if (MachineUpgradeHelper.isMachineUpgrade(stack)) return MachineUpgradeHelper.upgradeStackLimit(stack);
        return Math.min(64, stack.getMaxStackSize());
    }

    public static int slotLimit(Container inventory, int slot) {
        return isMaterialSlot(inventory, slot)
            ? MachineTier.from(((BlockEntity) inventory).getBlockState()).itemSlotCapacity() : 64;
    }

    public static ItemStack insert(Container inventory, int slot, ItemStack offered, boolean simulate) {
        ItemStack current = inventory.getItem(slot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, offered)) return offered;
        int moved = Math.min(offered.getCount(), Math.max(0, slotLimit(inventory, slot, offered) - current.getCount()));
        if (moved == 0) return offered;
        if (!simulate) inventory.setItem(slot, offered.copyWithCount(current.getCount() + moved));
        return offered.copyWithCount(offered.getCount() - moved);
    }

    public static boolean fits(Container inventory, int slot, ItemStack output) {
        ItemStack current = inventory.getItem(slot);
        return output.isEmpty() || (current.isEmpty() || ItemStack.isSameItemSameComponents(current, output))
            && (long) current.getCount() + output.getCount() <= slotLimit(inventory, slot, output);
    }

    public static void save(CompoundTag tag, NonNullList<ItemStack> stacks, HolderLookup.Provider registries) {
        NonNullList<ItemStack> copies = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
        int[] counts = new int[stacks.size()];
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            counts[slot] = stack.getCount();
            copies.set(slot, stack.copyWithCount(Math.min(99, Math.min(stack.getCount(), stack.getMaxStackSize()))));
        }
        ContainerHelper.saveAllItems(tag, copies, registries);
        tag.putIntArray(COUNTS, counts);
    }

    public static void load(CompoundTag tag, NonNullList<ItemStack> stacks, HolderLookup.Provider registries, Container inventory) {
        ContainerHelper.loadAllItems(tag, stacks, registries);
        int[] counts = tag.getIntArray(COUNTS);
        for (int slot = 0; slot < Math.min(counts.length, stacks.size()); slot++) {
            ItemStack stack = stacks.get(slot);
            if (!stack.isEmpty()) stack.setCount(Math.max(0, Math.min(counts[slot], slotLimit(inventory, slot, stack))));
        }
    }
}
