package net.crystalnexus.client.orescanner;

import java.util.OptionalDouble;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class OreOutlineRenderEvents {

    // ✅ RenderType that renders THROUGH walls (no depth test) + no cull + no depth writes
    private static final RenderType ORE_XRAY_LINES = RenderType.create(
            "crystalnexus_ore_xray_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(RenderStateShard.NO_LAYERING)          // ✅ important for "close weirdness"
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)       // ✅ through blocks
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)         // ✅ don't write depth
                    .createCompositeState(false)
    );

    @SubscribeEvent
public static void onRenderLevel(RenderLevelStageEvent event) {
    // Try a later stage if you still see depth issues:
    // AFTER_WEATHER and AFTER_PARTICLES are often “most on top”.
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
    if (!OreOutlineClient.isActive()) return;

    Minecraft mc = Minecraft.getInstance();
    var level = mc.level;
    var camera = mc.gameRenderer.getMainCamera();
    if (level == null || camera == null) return;

    Vec3 camPos = camera.getPosition();
    float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
    float fade = OreOutlineClient.getFade(partial);

    PoseStack poseStack = event.getPoseStack();
    poseStack.pushPose();
    poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

    MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
    VertexConsumer vc = buffers.getBuffer(ORE_XRAY_LINES);

    float a = 0.35f + 0.65f * fade;

    for (BlockPos pos : OreOutlineClient.getOutlined()) {
    if (!level.hasChunkAt(pos)) continue;

    var state = level.getBlockState(pos);
    if (state.isAir()) continue;

    float r = 1.0f, g = 1.0f, b = 1.0f; // ✅ always white
    AABB box = new AABB(pos).inflate(0.01);
    drawLineBox(poseStack, vc, box, r, g, b, a);
}

    // ✅ Force through-walls at flush time (this is the reliable part)
    RenderSystem.disableDepthTest();
    RenderSystem.depthMask(false);
    RenderSystem.disableCull();

    buffers.endBatch(ORE_XRAY_LINES);

    RenderSystem.enableCull();
    RenderSystem.depthMask(true);
    RenderSystem.enableDepthTest();

    poseStack.popPose();
}


    private static void drawLineBox(PoseStack poseStack, VertexConsumer vc, AABB bb, float r, float g, float b, float a) {
        double x0 = bb.minX, y0 = bb.minY, z0 = bb.minZ;
        double x1 = bb.maxX, y1 = bb.maxY, z1 = bb.maxZ;

        line(poseStack, vc, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(poseStack, vc, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(poseStack, vc, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(poseStack, vc, x0, y0, z1, x0, y0, z0, r, g, b, a);

        line(poseStack, vc, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(poseStack, vc, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(poseStack, vc, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(poseStack, vc, x0, y1, z1, x0, y1, z0, r, g, b, a);

        line(poseStack, vc, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(poseStack, vc, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(poseStack, vc, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(poseStack, vc, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void line(PoseStack poseStack, VertexConsumer vc,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1,
                             float r, float g, float b, float a) {

        var last = poseStack.last();
        var mat = last.pose();

        // Direction vector for a stable normal input
        float dx = (float) (x1 - x0);
        float dy = (float) (y1 - y0);
        float dz = (float) (z1 - z0);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-6f) len = 1.0f;
        dx /= len;
        dy /= len;
        dz /= len;

        vc.addVertex(mat, (float) x0, (float) y0, (float) z0)
                .setColor(r, g, b, a)
                .setNormal(last, dx, dy, dz);

        vc.addVertex(mat, (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a)
                .setNormal(last, dx, dy, dz);
    }


    private static int hsvFromHash(int h) {
        float hue = ((h & 0x7FFFFFFF) % 360) / 360.0f;
        float sat = 0.85f;
        float val = 0.95f;
        return hsvToRgb(hue, sat, val);
    }

    private static int hsvToRgb(float h, float s, float v) {
        float r, g, b;

        int i = (int) Math.floor(h * 6.0f);
        float f = h * 6.0f - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);

        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }

        int ri = (int) (r * 255.0f);
        int gi = (int) (g * 255.0f);
        int bi = (int) (b * 255.0f);

        return (ri << 16) | (gi << 8) | bi;
    }
}
