package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ContainerHelper;

public class FactoryOutputControllerOnTickUpdateProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        BlockPos controllerPos = BlockPos.containing(x, y, z);

        // Slot pairs: {linkSlot, controllerSlot}
        int[][] slotPairs = {
            {9, 14},
            {10, 15},
            {11, 16},
            {12, 17},
            {13, 18}
        };

        if (!(world instanceof ILevelExtension _ext)) return;
        IItemHandler controllerHandler = _ext.getCapability(Capabilities.ItemHandler.BLOCK, controllerPos, null);
        if (controllerHandler == null) return;

        for (int[] pair : slotPairs) {
            int linkSlot = pair[0];
            int controllerSlot = pair[1];

            // Read link item from controller's link slot
            ItemStack linkStack = controllerHandler.getStackInSlot(linkSlot).copy();
            if (linkStack.isEmpty()) continue;

            CustomData linkData = linkStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (linkData == null || !linkData.copyTag().contains("linkX")
                    || !linkData.copyTag().contains("linkY") || !linkData.copyTag().contains("linkZ"))
                continue;

            int linkX = (int) linkData.copyTag().getDouble("linkX");
            int linkY = (int) linkData.copyTag().getDouble("linkY");
            int linkZ = (int) linkData.copyTag().getDouble("linkZ");
            BlockPos targetPos = BlockPos.containing(linkX, linkY, linkZ);

            // First try WorldlyContainer (like a hopper would)
            var blockEntity = world.getBlockEntity(targetPos);
            if (blockEntity instanceof WorldlyContainer worldly) {
                int[] outputSlots = worldly.getSlotsForFace(Direction.DOWN);
                for (int slot : outputSlots) {
                    ItemStack stack = worldly.getItem(slot);
                    if (stack.isEmpty()) continue;

                    if (!worldly.canTakeItemThroughFace(slot, stack, Direction.DOWN)) continue;

                    // Simulate controller insert
                    ItemStack simulated = controllerHandler.insertItem(controllerSlot, stack.copy().split(1), true);
                    if (simulated.isEmpty()) {
                        // Do actual transfer
                        ItemStack toMove = stack.split(1);
                        controllerHandler.insertItem(controllerSlot, toMove, false);
                        worldly.setItem(slot, stack);
                        break; // one item per tick per link
                    }
                }
                continue; // done with this pair
            }

            // Fallback: normal IItemHandler, but beware this may include input slots
            IItemHandler targetHandler = _ext.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, null);
            if (targetHandler == null) continue;

            for (int i = 0; i < targetHandler.getSlots(); i++) {
                ItemStack extracted = targetHandler.extractItem(i, 1, true);
                if (extracted.isEmpty()) continue;

                ItemStack simulated = controllerHandler.insertItem(controllerSlot, extracted.copy(), true);
                if (simulated.getCount() < extracted.getCount()) {
                    controllerHandler.insertItem(controllerSlot, extracted.copy(), false);
                    targetHandler.extractItem(i, 1, false);
                    break;
                }
            }
        }
    }
}
