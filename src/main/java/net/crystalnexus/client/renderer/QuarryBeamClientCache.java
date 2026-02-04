package net.crystalnexus.client.render;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuarryBeamClientCache {
    // key = quarry pos, value = target pos (beam end)
    public static final Map<BlockPos, BlockPos> BEAMS = new ConcurrentHashMap<>();

    public static void set(BlockPos quarryPos, BlockPos targetPos) {
        if (targetPos == null) BEAMS.remove(quarryPos);
        else BEAMS.put(quarryPos, targetPos);
    }

    public static void remove(BlockPos quarryPos) {
        BEAMS.remove(quarryPos);
    }
}
