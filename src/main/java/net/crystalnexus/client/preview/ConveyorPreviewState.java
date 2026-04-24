package net.crystalnexus.client.preview;

import net.minecraft.core.BlockPos;
import net.crystalnexus.util.ConveyorSplinePlanner;

public final class ConveyorPreviewState {
    private static BlockPos sourcePos;
    private static ConveyorSplinePlanner.PathMode pathMode = ConveyorSplinePlanner.PathMode.SPLINE;

    private ConveyorPreviewState() {
    }

    public static void setSource(BlockPos pos) {
        sourcePos = pos;
    }

    public static BlockPos getSource() {
        return sourcePos;
    }

    public static ConveyorSplinePlanner.PathMode getPathMode() {
        return pathMode;
    }

    public static void setPathMode(ConveyorSplinePlanner.PathMode mode) {
        pathMode = mode;
    }

    public static void togglePathMode() {
        pathMode = pathMode == ConveyorSplinePlanner.PathMode.SPLINE
                ? ConveyorSplinePlanner.PathMode.ANGLED
                : ConveyorSplinePlanner.PathMode.SPLINE;
    }

    public static boolean hasSource() {
        return sourcePos != null;
    }

    public static void clear() {
        sourcePos = null;
    }
}
