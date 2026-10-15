package net.crystalnexus.jei_recipes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class OperatingGuideLayout {
    private OperatingGuideLayout() {}

    static void step(GuiGraphics graphics, int y, String number, Component title, Component body) {
        var font = Minecraft.getInstance().font;
        panel(graphics, 4, y, 232, 38, 0xffdedede);
        panel(graphics, 10, y + 7, 16, 22, 0xffbcbcbc);
        graphics.drawString(font, number, 15, y + 14, 0x303030, false);
        graphics.drawString(font, title, 34, y + 5, 0x303030, false);
        graphics.drawWordWrap(font, body, 34, y + 17, 196, 0x404040);
    }

    static void panel(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, 0xff888888);
        graphics.fill(x, y, x + width - 1, y + height - 1, 0xfff4f4f4);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
    }

    static void detailLabel(GuiGraphics graphics, int x, String key, Component label, double mouseX, double mouseY) {
        boolean hovered = key.equals(detailAt(mouseX, mouseY));
        panel(graphics, x, 236, 112, 14, hovered ? 0xffeeeeee : 0xffc6c6c6);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, label, x + 5, 239, 0x303030, false);
    }

    static String detailAt(double x, double y) {
        if (y < 236 || y >= 250) return null;
        if (x >= 4 && x < 116) return "setup";
        if (x >= 124 && x < 236) return "power";
        return null;
    }

}
