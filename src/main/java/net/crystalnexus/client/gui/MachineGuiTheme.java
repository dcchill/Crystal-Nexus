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
import net.crystalnexus.processing.TieredMachineBlock;
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
            if (stream == null) {
                BuiltInRegistries.BLOCK.forEach(block -> {
                    if (block instanceof TieredMachineBlock tiered)
                        result.put(BuiltInRegistries.BLOCK.getKey(block).getPath(), tiered.machineTier().level());
                });
                return Map.copyOf(result);
            }
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            json.getAsJsonObject("machines").entrySet().forEach(entry ->
                result.put(entry.getKey(), entry.getValue().getAsJsonObject().get("tier").getAsInt()));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot load machine GUI tier assignments", exception);
        }
        return Map.copyOf(result);
    }
    public static int accent(int tier) { return COLORS[Math.clamp(tier, 0, COLORS.length - 1)]; }
    public static int slotColor(String menu, int index, boolean playerInventory, int tier) {
        if (playerInventory) return 0x52788F;
        if (menu.equals("MatterTransmutationGUIMenu") && index >= 0 && index < MachineGuiLayout.TRANSMUTATION_OUTPUT)
            return net.minecraft.util.Mth.hsvToRgb(index / 8f, 0.75f, 1f);
        return accent(tier);
    }
    public static int tier(Level level, BlockPos pos, String fallback) {
        String id = level != null && pos != null ? BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath() : fallback;
        return TIERS.getOrDefault(id, TIERS.getOrDefault(fallback, 0));
    }
    public static int tier(Level level, BlockPos pos) { return tier(level, pos, "machine_frame"); }
    public static boolean hasEnergy(Level level, BlockPos pos) {
        return level != null && pos != null && level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null;
    }
    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        int radius = frameRadius(w, h);
        int accent = 0xFF000000 | color;
        roundedGradient(g, x, y, w, h, radius,
            net.minecraft.util.FastColor.ARGB32.lerp(0.3f, accent, 0xFFFFFFFF),
            net.minecraft.util.FastColor.ARGB32.lerp(0.55f, accent, 0xFF000000));
        roundedGradient(g, x + 1, y + 1, w - 2, h - 2, radius - 1,
            net.minecraft.util.FastColor.ARGB32.lerp(0.7f, accent, 0xFF0A1424),
            net.minecraft.util.FastColor.ARGB32.lerp(0.35f, accent, 0xFF0A1424));
        if (w > 5 && h > 5) {
            roundedGradient(g, x + 2, y + 2, w - 4, h - 4, radius - 2, 0xFF08111E, 0xFF101E2E);
        }
    }
    public static void slotFrame(GuiGraphics g, int x, int y, int color) {
        int size = MachineGuiLayout.SLOT_SIZE;
        g.fill(x, y, x + size, y + size, 0xF00A1424);
        g.renderOutline(x, y, size, size, 0xFF000000 | color);
        g.renderOutline(x + 1, y + 1, size - 2, size - 2, 0x50000000 | color);
        g.fill(x + 2, y + 2, x + size - 2, y + 3, 0x24000000 | color);
    }
    /** Integer corner cuts keep the bevel crisp at Minecraft's GUI scales. */
    static int frameRadius(int width, int height) { return Math.min(width, height) > 64 ? 6 : 3; }

    static int cornerInset(int radius, int row) {
        return radius - (int) Math.sqrt(radius * radius - (radius - row) * (radius - row));
    }
    private static void roundedGradient(GuiGraphics g, int x, int y, int w, int h, int radius, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        radius = Math.clamp(radius, 0, Math.min(w, h) / 2);
        for (int row = 0; row < radius; row++) {
            int inset = cornerInset(radius, row);
            int upper = net.minecraft.util.FastColor.ARGB32.lerp(row / (float) Math.max(1, h - 1), top, bottom);
            int lower = net.minecraft.util.FastColor.ARGB32.lerp((h - row - 1) / (float) Math.max(1, h - 1), top, bottom);
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, upper);
            g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, lower);
        }
        g.fillGradient(x, y + radius, x + w, y + h - radius,
            net.minecraft.util.FastColor.ARGB32.lerp(radius / (float) Math.max(1, h - 1), top, bottom),
            net.minecraft.util.FastColor.ARGB32.lerp((h - radius - 1) / (float) Math.max(1, h - 1), top, bottom));
    }
    public static void inset(GuiGraphics g, int x, int y, int w, int h) { frame(g, x, y, w, h, 0x517387); }
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int color) {
        frame(g, x, y, w, h, color);
        int fill = (int) (Math.clamp(Double.isFinite(fraction) ? fraction : 0, 0, 1) * (w - 2));
        if (fill > 0) g.fill(x + 1, y, x + 1 + fill, y + h, 0x30000000 | color);
        g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, 0xCF000000 | color);
        for (int i = 8; i < w - 1; i += 8) g.fill(x + i, y + 1, x + i + 1, y + h - 1, 0xB00A1424);
    }
    public static void background(GuiGraphics g, AbstractContainerMenu menu, Level level, BlockPos pos,
            Component title, int x, int y, int w, int h, int tier) {
        int color = accent(tier);
        frame(g, x, y - MachineGuiLayout.HEADER_HEIGHT, w, h + MachineGuiLayout.HEADER_HEIGHT, color);
        g.fillGradient(x + 4, y, x + w - 4, y + h - 4, 0xFF142238, 0xFF0A1423);
        g.fill(x + 3, y - 18, x + w - 3, y - 15, 0x30000000 | color);
        g.fill(x + 4, y - 17, x + w - 4, y - 16, 0xFF000000 | color);
        if (tier == 8) {
            int phase = (int) ((System.currentTimeMillis() / 120) % Math.max(1, w - 24));
            g.fill(x + 8 + phase, y - 17, x + 16 + phase, y - 16, 0xFFFFF1C3);
        }
        var font = Minecraft.getInstance().font;
        var actualTitle = level != null && pos != null ? level.getBlockState(pos).getBlock().getName() : title;
        g.drawString(font, font.plainSubstrByWidth(actualTitle.getString(), w - 16), x + 8, y - 37, 0xF3FAFF, false);
        g.drawString(font, "T" + (tier + 1) + " / " + MachineTier.forLevel(tier).displayName(), x + 8, y - 26, color, false);
        String motif = MOTIFS[Math.clamp(tier, 0, 8)];
        for (int i = 0; i < motif.length(); i++) if (motif.charAt(i) == '1') {
            g.fill(x + w - 21 + i % 5 * 2, y - 30 + i / 5 * 2, x + w - 17 + i % 5 * 2, y - 26 + i / 5 * 2, 0x38000000 | color);
            g.fill(x + w - 20 + i % 5 * 2, y - 29 + i / 5 * 2, x + w - 18 + i % 5 * 2, y - 27 + i / 5 * 2, 0xFF000000 | color);
        }
        var bay = MachineGuiLayout.upgradeBay(menu.slots, w, h);
        if (bay != null) frame(g, x + bay.x(), y + bay.y(), bay.width(), bay.height(), color);
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            var rect = MachineGuiLayout.slot(slot.x, slot.y);
            slotFrame(g, x + rect.x(), y + rect.y(),
                slotColor(menu.getClass().getSimpleName(), slot.getSlotIndex(), slot.container instanceof Inventory, tier));
        }
        if (hasEnergy(level, pos)) {
            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            double fraction = energy.getMaxEnergyStored() <= 0 ? 0 : (double) energy.getEnergyStored() / energy.getMaxEnergyStored();
            energyPanel(g, x, y, MachineGuiLayout.energy(menu.slots, h), fraction, color);
        }
    }
    public static void energyPanel(GuiGraphics g, int x, int y, MachineGuiLayout.Rect r, double fraction, int color) {
        frame(g, x + r.x(), y + r.y(), r.width(), r.height(), color);
        g.drawString(Minecraft.getInstance().font, "FE", x + r.x() + 8, y + r.y() + 5, 0xECF3FF, false);
        int segments = Math.max(0, (r.height() - 23) / 4);
        fraction = Math.clamp(Double.isFinite(fraction) ? fraction : 0, 0, 1);
        for (int i = 0; i < segments; i++) {
            int bottom = y + r.bottom() - 6 - i * 4;
            g.fill(x + r.x() + 6, bottom - 3, x + r.right() - 6, bottom,
                    fraction * segments > i ? 0xFF000000 | color : 0xFF293B50);
        }
    }
    public static boolean tooltip(GuiGraphics g, AbstractContainerMenu menu, Level level, BlockPos pos, Component title,
            int left, int top, int width, int height, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        if (new MachineGuiLayout.Rect(0, -MachineGuiLayout.HEADER_HEIGHT, width, MachineGuiLayout.HEADER_HEIGHT).contains(mouseX - left, mouseY - top)) {
            g.renderTooltip(font, level == null || pos == null ? title : level.getBlockState(pos).getBlock().getName(), mouseX, mouseY);
            return true;
        }
        if (hasEnergy(level, pos) && MachineGuiLayout.energy(menu.slots, height).contains(mouseX - left, mouseY - top)) {
            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            g.renderTooltip(font, Component.translatable("gui.crystalnexus.scanner.energy", energy.getEnergyStored(), energy.getMaxEnergyStored()), mouseX, mouseY);
            return true;
        }
        return false;
    }
}
