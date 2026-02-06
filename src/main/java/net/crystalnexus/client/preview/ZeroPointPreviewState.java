package net.crystalnexus.client.preview;

import net.minecraft.core.BlockPos;

public class ZeroPointPreviewState {
    public static BlockPos pos;
    public static String templateId;

    public static void activate(BlockPos p, String id) {
        pos = p;
        templateId = id;
    }

    public static boolean isActive() {
        return pos != null && templateId != null;
    }

    public static void clear() {
        pos = null;
        templateId = null;
    }
}
