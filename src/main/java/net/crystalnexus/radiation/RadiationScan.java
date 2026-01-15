package net.crystalnexus.radiation;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.crystalnexus.init.CrystalnexusModItems;

public class RadiationScan {

    public static void scanAndRadiate(ServerLevel level, BlockPos center, int radius, int yHalfHeight) {
        BlockPos min = center.offset(-radius, -yHalfHeight, -radius);
        BlockPos max = center.offset(radius, yHalfHeight, radius);

        BlockPos.betweenClosed(min, max).forEach(pos -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || be.isRemoved()) return;

            IItemHandler handler = getAnySidedItemHandler(level, pos);
            if (handler == null) return;

            int amount = countWaste(handler);
            if (amount <= 0) return;

            RadiationLogic.radiateFrom(level, pos, amount);
        });
    }

    private static IItemHandler getAnySidedItemHandler(ServerLevel level, BlockPos pos) {
        IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (h != null) return h;

        for (Direction dir : Direction.values()) {
            h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
            if (h != null) return h;
        }
        return null;
    }

    private static int countWaste(IItemHandler handler) {
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            var stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() == CrystalnexusModItems.BLUTONIUM_WASTE.get()) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
