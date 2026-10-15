package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.crystalnexus.item.MeteorScannerItem;
import net.crystalnexus.network.payload.S2C_MeteorScannerTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT)
public final class MeteorScannerRenderer {
    private static S2C_MeteorScannerTarget target;
    private static ClientLevel targetLevel;
    private static long received;
    private static RenderType markerType;

    public static void update(S2C_MeteorScannerTarget packet) {
        targetLevel = Minecraft.getInstance().level;
        target = packet.diameter() > 0 ? packet : null;
        received = targetLevel == null ? 0 : targetLevel.getGameTime();
    }

    private static boolean holdingScanner() {
        var player = Minecraft.getInstance().player;
        return player != null && (player.getMainHandItem().getItem() instanceof MeteorScannerItem
            || player.getOffhandItem().getItem() instanceof MeteorScannerItem);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null || level != targetLevel || !holdingScanner() || level.getGameTime() - received > 10) {
            target = null;
            targetLevel = null;
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || target == null || !holdingScanner()) return;
        var mc = Minecraft.getInstance();
        if (mc.level != targetLevel || mc.options.hideGui) return;
        if (markerType == null) markerType = RenderType.create("crystalnexus_meteor_scanner",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(RenderStateShard.NO_DEPTH_TEST).setCullState(RenderStateShard.NO_CULL)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false));
        double half = target.diameter() / 2.0;
        Vec3 center = Vec3.atLowerCornerOf(target.base()).add(half, half, half).subtract(event.getCamera().getPosition());
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(center.x, center.y, center.z);
        pose.mulPose(event.getCamera().rotation());
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double time = mc.level.getGameTime() + partial;
        boolean scanning = target.elapsed() >= 0 && mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof MeteorScannerItem;
        double progress = scanning ? Math.clamp((target.elapsed() + time - received) / MeteorScannerItem.SCAN_TICKS, 0, 1) : -1;
        var buffers = mc.renderBuffers().bufferSource();
        drawMarker(buffers.getBuffer(markerType), pose.last().pose(), time, progress);
        // NO_DEPTH_TEST is a no-op in 1.21.1; explicitly expose the marker inside the meteor.
        RenderSystem.disableDepthTest();
        try {
            buffers.endBatch(markerType);
        } finally {
            RenderSystem.enableDepthTest();
            pose.popPose();
        }
    }

    static void drawMarker(VertexConsumer vertices, Matrix4f matrix, double time, double progress) {
        // Billboard geometry stays visible at the meteor's center, even behind its outer blocks.
        matrix = new Matrix4f(matrix).scale(1.7f);
        for (int i = 0; i < 64; i++) {
            double angle = i * Math.PI / 32;
            if (i % 16 < 12) arc(vertices, matrix, angle, angle + Math.PI / 32, 0.65, 0.045, 255);
            if (progress >= 0 && i < progress * 64)
                arc(vertices, matrix, angle, angle + Math.PI / 32, 0.79, 0.055, 255);
        }
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            line(vertices, matrix, Math.cos(angle) * 0.12, Math.sin(angle) * 0.12,
                Math.cos(angle) * 0.32, Math.sin(angle) * 0.32, 0.045, 255);
            double x = Math.cos(angle + Math.PI / 4) * 0.9, y = Math.sin(angle + Math.PI / 4) * 0.9;
            line(vertices, matrix, x, y, x - Math.signum(x) * 0.2, y, 0.055, 255);
            line(vertices, matrix, x, y, x, y - Math.signum(y) * 0.2, 0.055, 255);
        }
        if (progress >= 0) {
            double sweep = time * 0.12;
            line(vertices, matrix, 0, 0, Math.cos(sweep) * 0.6, Math.sin(sweep) * 0.6, 0.055, 255);
            double y = Math.sin(time * 0.1) * 0.55;
            line(vertices, matrix, -0.55, y, 0.55, y, 0.04, 210);
        }
    }

    private static void arc(VertexConsumer vertices, Matrix4f matrix, double a, double b, double radius, double width, int alpha) {
        line(vertices, matrix, Math.cos(a) * radius, Math.sin(a) * radius,
            Math.cos(b) * radius, Math.sin(b) * radius, width, alpha);
    }

    private static void line(VertexConsumer vertices, Matrix4f matrix, double x1, double y1, double x2, double y2, double width, int alpha) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        double dx = -(y2 - y1) / length * width / 2, dy = (x2 - x1) / length * width / 2;
        // A dark outline keeps the bright reticle readable over both ore textures and daylight.
        stroke(vertices, matrix, x1, y1, x2, y2, dx * 1.9, dy * 1.9, 0x100C04, 255);
        stroke(vertices, matrix, x1, y1, x2, y2, dx, dy, 0xFFD23C, alpha);
    }

    private static void stroke(VertexConsumer vertices, Matrix4f matrix, double x1, double y1, double x2, double y2,
                               double dx, double dy, int color, int alpha) {
        vertex(vertices, matrix, x1 + dx, y1 + dy, color, alpha);
        vertex(vertices, matrix, x1 - dx, y1 - dy, color, alpha);
        vertex(vertices, matrix, x2 - dx, y2 - dy, color, alpha);
        vertex(vertices, matrix, x2 + dx, y2 + dy, color, alpha);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, double x, double y, int color, int alpha) {
        vertices.addVertex(matrix, (float) x, (float) y, 0).setColor(color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
    }
}
