package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.GradientItemName.Palette;
import net.crystalnexus.item.MeteorDataDiskItem;
import net.crystalnexus.multiblock.HeavenshatterMeteor;
import net.crystalnexus.multiblock.RecordedMeteor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, value = Dist.CLIENT)
public final class MeteorDataDiskPreview {
    private static RecordedMeteor cachedRecord;
    private static List<HeavenshatterMeteor.Cell> surface = List.of();

    private MeteorDataDiskPreview() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof MeteorDataDiskItem)
            event.getToolTip().addAll(details(MeteorDataDiskItem.read(event.getItemStack()), Screen.hasShiftDown()));
    }

    static List<Component> details(Optional<RecordedMeteor> record, boolean expanded) {
        if (!expanded) return List.of(Component.translatable("tooltip.crystalnexus.meteor_data_disk.hold",
            Component.translatable("tooltip.crystalnexus.meteor_data_disk.shift").withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.DARK_GRAY));
        List<Component> lines = new ArrayList<>();
        if (record.isEmpty()) {
            lines.add(Component.translatable("tooltip.crystalnexus.meteor_data_disk.blank"));
        } else {
            RecordedMeteor meteor = record.orElseThrow();
            lines.add(Component.translatable("tooltip.crystalnexus.meteor_data_disk.recorded", meteor.diameter()));
            for (int i = 0; i < meteor.palette().size(); i++)
                lines.add(Component.translatable("tooltip.crystalnexus.meteor_data_disk.material",
                    meteor.palette().get(i).getBlock().getName(),
                    String.format(Locale.ROOT, "%.1f", 100.0 * meteor.weights().get(i) / meteor.totalWeight())));
            lines.add(Component.translatable("tooltip.crystalnexus.meteor_data_disk.simulation"));
            lines.add(Component.translatable("tooltip.crystalnexus.meteor_data_disk.overwrite"));
        }
        return lines.stream().<Component>map(line -> line.copy().withStyle(style -> style.withColor(Palette.FERROSTEEL.midpoint()))).toList();
    }

    @SubscribeEvent
    public static void closing(ScreenEvent.Closing event) {
        cachedRecord = null;
        surface = List.of();
    }

    @SubscribeEvent
    public static void render(ContainerScreenEvent.Render.Foreground event) {
        var screen = event.getContainerScreen();
        var slot = screen.getSlotUnderMouse();
        var record = slot == null ? Optional.<RecordedMeteor>empty() : MeteorDataDiskItem.read(slot.getItem());
        if (record.isEmpty()) {
            cachedRecord = null;
            surface = List.of();
            return;
        }
        RecordedMeteor meteor = record.orElseThrow();
        if (!meteor.equals(cachedRecord)) {
            surface = surfaceCells(meteor.diameter(), meteor.seed(), meteor.palette().size());
            cachedRecord = meteor;
        }
        var graphics = event.getGuiGraphics();
        var pose = graphics.pose();
        var mc = Minecraft.getInstance();
        // Foreground coordinates are relative to the container, not the screen.
        int originX = slot.x + 8, originY = slot.y + 8;
        int centerX = (int) Math.clamp(screen.getGuiLeft() + originX, 26, Math.max(26, screen.width - 26)) - screen.getGuiLeft();
        int centerY = (int) Math.clamp(screen.getGuiTop() + originY - 42, 26, Math.max(26, screen.height - 26)) - screen.getGuiTop();
        graphics.flush();
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        pose.pushPose();
        try {
            pose.translate(0, 0, 180);
            // Thin translucent strips form a soft cone from the disk to the floating meteor.
            int beamHeight = Math.max(1, originY - (centerY + 12));
            for (int y = 0; y < beamHeight; y++) {
                float progress = y / (float) beamHeight;
                int x = Math.round(centerX + (originX - centerX) * progress);
                int halfWidth = Math.round(12 * (1 - progress)) + 1;
                graphics.fill(x - halfWidth, centerY + 12 + y, x + halfWidth, centerY + 13 + y, 0x2266FFE5);
            }
            graphics.flush();
            RenderSystem.enableDepthTest();
            Lighting.setupFor3DItems();
            pose.translate(centerX, centerY, 20);
            float scale = 42.0f / meteor.diameter();
            pose.scale(scale, -scale, scale);
            float angle = (System.currentTimeMillis() % 24000L) * (float) (Math.PI * 2 / 24000);
            pose.mulPose(new Quaternionf().rotateX(0.35f).rotateY(angle));
            pose.translate(-meteor.diameter() / 2.0, -meteor.diameter() / 2.0, -meteor.diameter() / 2.0);
            var buffers = mc.renderBuffers().bufferSource();
            for (var cell : surface) {
                pose.pushPose();
                try {
                    pose.translate(cell.x(), cell.y(), cell.z());
                    mc.getBlockRenderer().renderSingleBlock(meteor.palette().get(cell.material()), pose, buffers,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                } finally {
                    pose.popPose();
                }
            }
            buffers.endBatch();
        } finally {
            pose.popPose();
            Lighting.setupForFlatItems();
            if (depthEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        }
    }

    static List<HeavenshatterMeteor.Cell> surfaceCells(int diameter, long seed, int materials) {
        var cells = HeavenshatterMeteor.cells(diameter, seed, materials);
        var occupied = new HashSet<Integer>();
        for (var cell : cells) occupied.add(cell.x() + 32 * cell.y() + 1024 * cell.z());
        return cells.stream().filter(cell -> {
            int key = cell.x() + 32 * cell.y() + 1024 * cell.z();
            return !occupied.contains(key - 1) || !occupied.contains(key + 1)
                || !occupied.contains(key - 32) || !occupied.contains(key + 32)
                || !occupied.contains(key - 1024) || !occupied.contains(key + 1024);
        }).toList();
    }
}
