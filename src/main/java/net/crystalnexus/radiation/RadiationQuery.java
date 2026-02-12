package net.crystalnexus.radiation;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.crystalnexus.init.CrystalnexusModItems;

public class RadiationQuery {

    private static final int QUERY_RADIUS = 24;
    private static final int QUERY_Y = 8;

    public static double getRadiationAt(Level level, BlockPos center) {

        double totalRadiation = 0;

        BlockPos min = center.offset(-QUERY_RADIUS, -QUERY_Y, -QUERY_RADIUS);
        BlockPos max = center.offset(QUERY_RADIUS, QUERY_Y, QUERY_RADIUS);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {

            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || be.isRemoved()) continue;

            IItemHandler handler = getAnySidedItemHandler(level, pos);
            if (handler == null) continue;

            int waste = countWaste(handler);
            if (waste <= 0) continue;

            double distance = Math.sqrt(pos.distSqr(center));
            double strength = waste * 5.0;

            double falloff = 1.0 / (distance + 1.0);
            totalRadiation += waste * 10.0 * falloff;

        }

        return totalRadiation;
    }

    private static IItemHandler getAnySidedItemHandler(Level level, BlockPos pos) {
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
