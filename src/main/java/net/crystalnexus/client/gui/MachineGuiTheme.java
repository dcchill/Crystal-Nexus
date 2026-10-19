package net.crystalnexus.client.gui;

import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.processing.TieredMachineBlock;
import net.crystalnexus.util.MachineInspection;
import net.crystalnexus.world.inventory.MachineUpgradeSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Shared industrial panel geometry; all coordinates remain relative to existing menu slots. */
public final class MachineGuiTheme {
    private MachineGuiTheme() {}
    public static int accent(int tier) {
        return tier < 0 ? 0xBC75DE : tier == MachineTier.CARBON.level() ? 0xBFC8D4
            : tier == MachineTier.TUNGSTEN.level() ? 0xBD8DCD : MachineTier.forLevel(tier).displayColor();
    }
    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, 0xF0101720);
        g.renderOutline(x, y, w, h, 0xFF000000 | color);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, 0xFF344352);
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, 0xFF080D12);
    }
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int color) {
        g.fill(x, y, x + w, y + h, 0xFF080E16);
        int fill = (int) (Math.clamp(Double.isFinite(fraction) ? fraction : 0, 0, 1) * (w - 2));
        g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, 0xFF000000 | color);
        for (int i = 8; i < w - 1; i += 8) g.fill(x + i, y + 1, x + i + 1, y + h - 1, 0xA0101720);
    }
    public static int tier(Level level, BlockPos pos) {
        return level != null && level.getBlockState(pos).getBlock() instanceof TieredMachineBlock t ? t.machineTier().level() : -1;
    }
    public static void background(GuiGraphics g, AbstractContainerMenu menu, Level level, BlockPos pos,
            Component title, int x, int y, int w, int h) {
        int tier = tier(level, pos), color = accent(tier);
        frame(g, x, y - 29, w, h + 29, color);
        g.fill(x + 4, y + 3, x + w - 4, y + 73, 0xFF182330);
        g.fill(x + 4, y + 77, x + w - 4, y + h - 4, 0xFF111923);
        details(g, x, y, w, h, tier, color);
        var font = Minecraft.getInstance().font;
        var actualTitle = level != null && tier >= 0 ? level.getBlockState(pos).getBlock().getName() : title;
        g.drawString(font, font.plainSubstrByWidth(actualTitle.getString(), w - 16), x + 8, y - 23, 0xECF3FF, false);
        Component tierText = tier < 0 ? Component.empty() : Component.translatable("gui.crystalnexus.machine.material_tier",
            tier + 1, MachineTier.forLevel(tier).displayName());
        g.drawString(font, tierText, x + 8, y - 12, color, false);
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            boolean upgrade = slot instanceof MachineUpgradeSlot;
            frame(g, sx - (upgrade ? 2 : 0), sy - (upgrade ? 2 : 0), upgrade ? 22 : 18, upgrade ? 22 : 18,
                upgrade ? color : 0x405363);
            if (upgrade) { g.fill(sx + 2, sy - 2, sx + 6, sy, 0xFF000000 | color); }
        }
        if (level != null) {
            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            if (energy != null) {
                frame(g, x - 32, y, 29, 48, color);
                g.drawString(font, "FE", x - 24, y + 5, 0xECF3FF, false);
                double fraction = MachineInspection.fraction(energy.getEnergyStored(), energy.getMaxEnergyStored());
                for (int i = 0; i < 6; i++) g.fill(x - 25, y + 38 - i * 4, x - 10, y + 41 - i * 4,
                    fraction * 6 > i ? 0xFF000000 | color : 0xFF293441);
            }
        }
    }
    private static void details(GuiGraphics g, int x, int y, int w, int h, int tier, int c) {
        int color = 0xFF000000 | c;
        int muted = 0xFF000000 | ((c & 0xF8F8F8) >>> 3);
        // Broad material texture beneath the slot wells makes tiers recognizable at a glance.
        switch (tier) {
            case 0 -> {
                g.fill(x + 8, y + 65, x + w - 8, y + 67, 0xFF35424C);
                for (int i = 10; i < w - 10; i += 24) g.fill(x + i, y + 68, x + i + 2, y + 70, 0xFF7C8790);
            }
            case 1 -> {
                for (int i = 0; i < 12; i++) {
                    g.fill(x + 8 + i, y + 7 + i, x + 10 + i, y + 8 + i, muted);
                    g.fill(x + w - 10 - i, y + 7 + i, x + w - 8 - i, y + 8 + i, muted);
                }
                g.fill(x + 18, y + 68, x + w - 18, y + 69, muted);
            }
            case 2 -> {
                for (int i = 14; i < w - 14; i += 28) {
                    g.fill(x + i, y + 5, x + i + 1, y + 72, muted);
                    for (int j = 12; j < 70; j += 18) g.fill(x + i, y + j, x + i + 12, y + j + 1, muted);
                }
            }
            case 3 -> {
                for (int i = 10; i < w - 18; i += 18) for (int j = 0; j < 8; j++)
                    g.fill(x + i + j, y + 64 + j, x + i + j + 3, y + 65 + j, muted);
            }
            case 4 -> {
                for (int i = 8; i < w - 8; i += 8) g.fill(x + i, y + 5, x + i + 1, y + 72, 0xFF213141);
                for (int j = 8; j < 72; j += 8) g.fill(x + 8, y + j, x + w - 8, y + j + 1, 0xFF213141);
            }
            case 5 -> {
                for (int i = 8; i < w - 8; i += 4) for (int j = 5; j < 72; j += 4)
                    g.fill(x + i, y + j, x + i + 3, y + j + 2, ((i + j) % 8 < 4) ? 0xFF27313B : 0xFF111923);
            }
            case 6 -> {
                for (int j : new int[]{6, 9, 66, 69}) g.fill(x + 8, y + j, x + w - 8, y + j + 1, muted);
                for (int i : new int[]{8, w - 14}) g.renderOutline(x + i, y + 12, 6, 50, 0xFF49343A);
            }
            case 7 -> {
                for (int i = 8; i < w - 18; i += 24) {
                    g.fill(x + i, y + 5, x + i + 20, y + 72, 0xFF211D2D);
                    g.fill(x + i + 2, y + 6, x + i + 18, y + 7, muted);
                }
            }
            case 8 -> {
                g.renderOutline(x + 7, y + 5, w - 14, 67, 0xFF574126);
                g.renderOutline(x + 10, y + 8, w - 20, 61, 0xFF332B25);
                for (int i = 16; i < w - 16; i += 16) g.fill(x + i, y + 66, x + i + 6, y + 68, muted);
            }
            default -> { }
        }
        // Material patterns stay in the panel margins, away from slots and text.
        for (int side = 0; side < 2; side++) {
            int edge = x + (side == 0 ? 2 : w - 5);
            for (int i = 6; i < h - 6; i += 12) {
                switch (tier) {
                    case 0 -> { g.fill(edge, y + i, edge + 2, y + i + 2, 0xFF9CA4AB); }
                    case 1 -> { g.fill(edge, y + i, edge + 2, y + i + 5, color); g.fill(edge - 1, y + i + 2, edge + 3, y + i + 3, color); }
                    case 2 -> { g.fill(edge, y + i, edge + 1, y + i + 9, color); g.fill(edge, y + i + 3, edge + 3, y + i + 4, color); }
                    case 3 -> { for (int j = 0; j < 3; j++) g.fill(edge + j, y + i + j, edge + j + 1, y + i + j + 2, color); }
                    case 4 -> { g.renderOutline(edge - 1, y + i, 4, 8, color); }
                    case 5 -> { for (int j = 0; j < 6; j++) g.fill(edge + j % 2, y + i + j, edge + j % 2 + 2, y + i + j + 1, 0xFF65717E); }
                    case 6 -> { g.fill(edge - 1, y + i, edge, y + i + 8, color); g.fill(edge + 2, y + i, edge + 3, y + i + 8, color); }
                    case 7 -> { g.fill(edge - 1, y + i, edge + 3, y + i + 6, 0xFF47334F); g.fill(edge, y + i, edge + 2, y + i + 4, color); }
                    default -> { g.fill(edge, y + i, edge + 2, y + i + 7, color); }
                }
            }
        }
        g.fill(x + 6, y + 75, x + w - 6, y + 76, 0xFF344352);
        if (tier == 8) {
            int phase = (int) ((System.currentTimeMillis() / 100) % (w - 24));
            g.fill(x + 8 + phase, y + 75, x + 16 + phase, y + 76, color);
            g.renderOutline(x - 2, y - 31, w + 4, h + 33, 0xFF84532B);
        }
    }
    public static void progress(GuiGraphics g, Level level, BlockPos pos, int x, int y, int width) {
        var be = level == null ? null : level.getBlockEntity(pos);
        double value = be == null ? -1 : MachineInspection.fraction(be.getPersistentData().getDouble("progress"), be.getPersistentData().getDouble("maxProgress"));
        bar(g, x, y + 11, width, 9, value, accent(tier(level, pos)));
    }
    public static boolean tooltip(GuiGraphics g, Level level, BlockPos pos, Component title,
            int left, int top, int width, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        if (mouseX >= left && mouseX < left + width && mouseY >= top - 29 && mouseY < top) {
            g.renderTooltip(font, level == null ? title : level.getBlockState(pos).getBlock().getName(), mouseX, mouseY);
            return true;
        }
        if (level != null && mouseX >= left - 32 && mouseX < left - 3 && mouseY >= top && mouseY < top + 48) {
            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            if (energy != null) {
                g.renderTooltip(font, Component.translatable("gui.crystalnexus.scanner.energy", energy.getEnergyStored(), energy.getMaxEnergyStored()), mouseX, mouseY);
                return true;
            }
        }
        return false;
    }
}
