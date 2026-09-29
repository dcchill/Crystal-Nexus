package net.crystalnexus.reactor;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Tracks validated reactor bounds so block changes can invalidate a layout immediately. */
public final class ReactorLayoutInvalidation {
    private static final Map<ServerLevel, Map<BlockPos, Bounds>> CONTROLLERS = new WeakHashMap<>();

    private ReactorLayoutInvalidation() {}

    public static void register(ServerLevel level, BlockPos controller, ReactorLayout layout) {
        if (!layout.valid) {
            unregister(level, controller);
            return;
        }
        CONTROLLERS.computeIfAbsent(level, ignored -> new HashMap<>())
            .put(controller.immutable(), new Bounds(layout.minBounds(), layout.maxBounds()));
    }

    public static void unregister(ServerLevel level, BlockPos controller) {
        Map<BlockPos, Bounds> entries = CONTROLLERS.get(level);
        if (entries != null) entries.remove(controller);
    }

    public static void invalidateAt(ServerLevel level, BlockPos changed) {
        Map<BlockPos, Bounds> entries = CONTROLLERS.get(level);
        if (entries == null) return;
        for (Map.Entry<BlockPos, Bounds> entry : entries.entrySet()) {
            if (!entry.getValue().contains(changed)) continue;
            if (level.getBlockEntity(entry.getKey()) instanceof ReactorComputerBlockEntity computer) {
                computer.invalidateLayout();
            }
        }
    }

    private record Bounds(BlockPos min, BlockPos max) {
        private boolean contains(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }
    }
}
