package net.crystalnexus.client.gui;

import com.google.gson.JsonParser;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.world.inventory.MachineGuiLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Pixel-aligned holographic console primitives, used only by opt-in screens. */
public final class MachineGuiTheme {
    private static final int[] COLORS = {0xE4F2FF, 0xC88AFF, 0x65F73B, 0xFF7843, 0x81CFFF,
        0xC0D2DF, 0xEBA5A0, 0xDA87EF, 0xFFD15B};
    private static final Map<String, Integer> TIERS = loadTiers();
    private static final String[] MOTIFS = {
        "1111101000101010100011111", "0010001110111110111000100", "0010011100001110010011100",
        "1001001001001000100110010", "1111110001101011000111111", "1010101010101010101010101",
        "1111100000111110000011111", "0111010001101011000101110", "0010001010101010101000100"
    };
    private MachineGuiTheme() {}

    private static Map<String, Integer> loadTiers() {
        Map<String, Integer> result = new HashMap<>();
        try (var stream = MachineGuiTheme.class.getResourceAsStream("/assets/crystalnexus/machine_gui_tiers.json")) {
            if (stream == null) throw new IllegalStateException("Missing machine GUI tier assignments");
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            json.getAsJsonObject("machines").entrySet().forEach(entry ->
                result.put(entry.getKey(), entry.getValue().getAsJsonObject().get("tier").getAsInt()));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot load machine GUI tier assignments", exception);
        }
        return Map.copyOf(result);
    }
    public static int accent(int tier) { return COLORS[Math.clamp(tier, 0, COLORS.length - 1)]; }
    public static int tier(Level level, BlockPos pos, String fallback) {
        String id = level != null && pos != null ? BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath() : fallback;
        return TIERS.getOrDefault(id, TIERS.getOrDefault(fallback, 0));
    }
    public static int tier(Level level, BlockPos pos) { return tier(level, pos, "machine_frame"); }
    public static boolean hasEnergy(Level level, BlockPos pos) {
        return level != null && pos != null && level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null;
    }
    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, 0xF00A1424);
        g.renderOutline(x, y, w, h, 0xFF000000 | color);
        if (w > 5 && h > 5) {
            g.renderOutline(x + 1, y + 1, w - 2, h - 2, 0x50000000 | color);
            g.fill(x + 2, y + 2, x + w - 2, y + 3, 0x24000000 | color);
        }
    }
    public static void inset(GuiGraphics g, int x, int y, int w, int h) { frame(g, x, y, w, h, 0x517387); }
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int color) {
        frame(g, x, y, w, h, color);
        int fill = (int) (Math.clamp(Double.isFinite(fraction) ? fraction : 0, 0, 1) * (w - 2));
        g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, 0xCF000000 | color);
        for (int i = 8; i < w - 1; i += 8) g.fill(x + i, y + 1, x + i + 1, y + h - 1, 0xB00A1424);
    }
    public static void background(GuiGraphics g, AbstractContainerMenu menu, Level level, BlockPos pos,
            Component title, int x, int y, int w, int h, int tier) {
        int color = accent(tier);
        frame(g, x, y - MachineGuiLayout.HEADER_HEIGHT, w, h + MachineGuiLayout.HEADER_HEIGHT, color);
        // Quiet content area: motifs occupy only the header separator and outer rails.
        g.fill(x + 4, y, x + w - 4, y + h - 4, 0xFF0D192A);
        g.fill(x + 4, y - 17, x + w - 4, y - 16, 0xFF000000 | color);
        details(g, x, y, w, h, tier, color);
        var font = Minecraft.getInstance().font;
        var actualTitle = level != null && pos != null ? level.getBlockState(pos).getBlock().getName() : title;
        g.drawString(font, font.plainSubstrByWidth(actualTitle.getString(), w - 16), x + 8, y - 37, 0xF3FAFF, false);
        g.drawString(font, "T" + (tier + 1) + " / " + MachineTier.forLevel(tier).displayName(), x + 8, y - 26, color, false);
        String motif = MOTIFS[Math.clamp(tier, 0, 8)];
        for (int i = 0; i < motif.length(); i++) if (motif.charAt(i) == '1')
            g.fill(x + w - 20 + i % 5 * 2, y - 29 + i / 5 * 2, x + w - 18 + i % 5 * 2, y - 27 + i / 5 * 2, 0xFF000000 | color);
        MachineGuiLayout.Rect bay = null;
        for (var slot : menu.slots) if (slot.isActive() && (slot.x >= w || slot.x < 0)) {
            var r = new MachineGuiLayout.Rect(slot.x - 4, slot.y - 4, 24, 24);
            bay = bay == null ? r : bay.union(r);
        }
        if (bay != null) frame(g, x + bay.x(), y + bay.y(), bay.width(), bay.height(), color);
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            var rect = MachineGuiLayout.slot(slot.x, slot.y);
            frame(g, x + rect.x(), y + rect.y(), rect.width(), rect.height(), slot.container instanceof Inventory ? 0x52788F : color);
        }
        if (hasEnergy(level, pos)) {
            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            var r = MachineGuiLayout.energy();
            frame(g, x + r.x(), y, r.width(), r.height(), color);
            g.drawString(font, "FE", x + r.x() + 8, y + 5, 0xECF3FF, false);
            double fraction = energy.getMaxEnergyStored() <= 0 ? 0 : (double) energy.getEnergyStored() / energy.getMaxEnergyStored();
            for (int i = 0; i < 6; i++) g.fill(x + r.x() + 6, y + 38 - i * 4, x + r.right() - 6, y + 41 - i * 4,
                fraction * 6 > i ? 0xFF000000 | color : 0xFF293B50);
        }
    }
    private static void details(GuiGraphics g, int x, int y, int w, int h, int tier, int color) {
        int c = 0xFF000000 | color;
        for (int side = 0; side < 2; side++) {
            int edge = x + (side == 0 ? 1 : w - 3);
            for (int i = 4; i < h - 10; i += 16) {
                switch (tier) {
                    case 0 -> g.fill(edge, y + i, edge + 2, y + i + 5, c);
                    case 1 -> { g.fill(edge, y + i, edge + 1, y + i + 4, c); g.fill(edge + 1, y + i + 4, edge + 2, y + i + 8, c); }
                    case 2 -> { g.fill(edge, y + i, edge + 1, y + i + 12, c); g.fill(edge, y + i + 4, edge + 3, y + i + 5, c); }
                    case 3 -> { for (int j = 0; j < 3; j++) g.fill(edge + j % 2, y + i + j * 3, edge + j % 2 + 1, y + i + j * 3 + 2, c); }
                    case 4 -> g.renderOutline(edge, y + i, 2, 10, c);
                    case 5 -> { for (int j = 0; j < 8; j += 2) g.fill(edge + j % 3, y + i + j, edge + j % 3 + 1, y + i + j + 1, c); }
                    case 6 -> { g.fill(edge, y + i, edge + 2, y + i + 3, c); g.fill(edge, y + i + 6, edge + 2, y + i + 9, c); }
                    case 7 -> { g.renderOutline(edge, y + i, 3, 10, c); g.fill(edge + 1, y + i + 3, edge + 2, y + i + 7, c); }
                    default -> { g.fill(edge, y + i, edge + 2, y + i + 9, c); g.fill(edge, y + i + 12, edge + 2, y + i + 13, c); }
                }
            }
        }
        if (tier == 8) {
            int phase = (int) ((System.currentTimeMillis() / 120) % Math.max(1, w - 24));
            g.fill(x + 8 + phase, y - 17, x + 16 + phase, y - 16, 0xFFFFF1C3);
        }
    }
    public static boolean tooltip(GuiGraphics g, Level level, BlockPos pos, Component title,
            int left, int top, int width, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        if (new MachineGuiLayout.Rect(0, -MachineGuiLayout.HEADER_HEIGHT, width, MachineGuiLayout.HEADER_HEIGHT).contains(mouseX - left, mouseY - top)) {
            g.renderTooltip(font, level == null || pos == null ? title : level.getBlockState(pos).getBlock().getName(), mouseX, mouseY);
            return true;
        }
        if (hasEnergy(level, pos) && MachineGuiLayout.energy().contains(mouseX - left, mouseY - top)) {
            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            g.renderTooltip(font, Component.translatable("gui.crystalnexus.scanner.energy", energy.getEnergyStored(), energy.getMaxEnergyStored()), mouseX, mouseY);
            return true;
        }
        return false;
    }
}
