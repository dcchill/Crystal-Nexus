package net.crystalnexus.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/** Vanilla container colors and stepped pixel corners, at any panel size. */
final class ControllerGuiStyle {
    private ControllerGuiStyle() {}

    static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 2, y, x + width - 2, y + height, 0xff000000);
        graphics.fill(x, y + 2, x + width, y + height - 2, 0xff000000);
        graphics.fill(x + 2, y + 1, x + width - 2, y + height - 1, 0xff555555);
        graphics.fill(x + 1, y + 2, x + width - 1, y + height - 2, 0xff555555);
        graphics.fill(x + 2, y + 2, x + width - 3, y + height - 3, 0xffffffff);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xffc6c6c6);
    }

    static void inset(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xff373737);
        graphics.fill(x + 1, y + 1, x + width, y + height, 0xffffffff);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xff8b8b8b);
    }
}
