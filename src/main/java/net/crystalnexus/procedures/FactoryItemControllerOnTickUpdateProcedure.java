package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;

public class FactoryItemControllerOnTickUpdateProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        BlockPos distributorPos = BlockPos.containing(x, y, z);

        // Slot pairs: {linkSlot, inputSlot}
        int[][] slotPairs = {
        	 {9, 14},
            {10, 15},
            {11, 16},
            {12, 17},
            {13, 18}
        };

        for (int[] pair : slotPairs) {
            int linkSlot = pair[0];
            int sourceSlot = pair[1];

            ItemStack linkStack = itemFromBlockInventory(world, distributorPos, linkSlot).copy();
            if (linkStack.isEmpty()) continue;

            CustomData linkData = linkStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (linkData == null || !linkData.copyTag().contains("linkX") ||
                !linkData.copyTag().contains("linkY") || !linkData.copyTag().contains("linkZ")) continue;

            int linkX = (int) linkData.copyTag().getDouble("linkX");
            int linkY = (int) linkData.copyTag().getDouble("linkY");
            int linkZ = (int) linkData.copyTag().getDouble("linkZ");
            BlockPos targetPos = BlockPos.containing(linkX, linkY, linkZ);

            if (!(world instanceof ILevelExtension _ext)) continue;

            IItemHandler distributorHandler = _ext.getCapability(Capabilities.ItemHandler.BLOCK, distributorPos, null);
            IItemHandler targetHandler = _ext.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, null);
            if (distributorHandler == null || targetHandler == null) continue;

            ItemStack inputStack = distributorHandler.getStackInSlot(sourceSlot).copy();
            if (inputStack.isEmpty()) continue;

            // Try to insert 1 item into target slot safely
            for (int i = 0; i < targetHandler.getSlots(); i++) {
                ItemStack toInsert = inputStack.copy().split(1);
                ItemStack simulated = targetHandler.insertItem(i, toInsert.copy(), true); // simulate insertion
                if (simulated.getCount() < toInsert.getCount()) {
                    // Slot accepts item
                    targetHandler.insertItem(i, toInsert, false); // actually insert
                    distributorHandler.extractItem(sourceSlot, 1, false); // remove from distributor
                    break;
                }
            }
        }
    }

    private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
        if (world instanceof ILevelExtension ext) {
            IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            if (itemHandler != null) return itemHandler.getStackInSlot(slot);
        }
        return ItemStack.EMPTY;
    }
}
