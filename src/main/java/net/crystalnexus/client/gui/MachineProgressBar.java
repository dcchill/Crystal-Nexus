package net.crystalnexus.client.gui;

import net.crystalnexus.item.GradientItemName.Palette;
import net.crystalnexus.events.TooltipInfoHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;

/** Tier-colored replacements for the 32-pixel progress sprite frames. */
public final class MachineProgressBar {
    private MachineProgressBar() {}

    public static void render(GuiGraphics graphics, Level level, int blockX, int blockY, int blockZ,
            int x, int y, int frameOffset, boolean vertical, boolean reversed) {
        render(graphics, level, level.getBlockState(new BlockPos(blockX, blockY, blockZ)).getBlock().asItem(),
            x, y, frameOffset, vertical, reversed);
    }

    public static void render(GuiGraphics graphics, Level level, Item item,
            int x, int y, int frameOffset, boolean vertical, boolean reversed) {
        Palette palette = TooltipInfoHandler.namePalette(item, level);
        if (palette == null) palette = Palette.IRON;
        int width = vertical ? 14 : 32;
        int height = vertical ? 32 : 13;
        x += vertical ? 9 : 0;
        y += vertical ? 0 : 10;
        graphics.fill(x, y, x + width, y + height, 0xFF373737);
        graphics.renderOutline(x, y, width, height, 0xFF202020);
        int length = (vertical ? height : width) - 2;
        int filled = filledPixels(frameOffset, length);
        for (int i = 0; i < filled; i++) {
            int color = 0xFF000000 | palette.colorAt((float) i / (length - 1));
            if (vertical) {
                int row = reversed ? y + 1 + i : y + height - 2 - i;
                graphics.fill(x + 1, row, x + width - 1, row + 1, color);
            } else {
                graphics.fill(x + 1 + i, y + 1, x + 2 + i, y + height - 1, color);
            }
        }
    }

    static int filledPixels(int frameOffset, int length) {
        return Math.clamp(frameOffset / 32, 0, 10) * length / 10;
    }
}
