package net.crystalnexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class QuarryBeamWorldRender {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // reliable render phase
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffer.getBuffer(RenderType.lines());
        Matrix4f mat = poseStack.last().pose();

        double max = 512.0;
        double max2 = max * max;

        for (var entry : QuarryBeamClientCache.BEAMS.entrySet()) {
            BlockPos quarryPos = entry.getKey();
            BlockPos targetPos = entry.getValue();
            if (targetPos == null) continue;

            // distance cap so it doesn't draw forever
            if (quarryPos.getCenter().distanceToSqr(cam) > max2) continue;

            drawThickBeam(quarryPos, targetPos, vc, mat, cam, 0.08f);
        }

        buffer.endBatch();
    }

    private static void drawThickBeam(BlockPos quarryPos, BlockPos targetPos,
                                      VertexConsumer vc, Matrix4f mat, Vec3 cam,
                                      float thickness) {

        // Start: bottom-center of quarry
        Vec3 startWorld = new Vec3(quarryPos.getX() + 0.5, quarryPos.getY() + 0.05, quarryPos.getZ() + 0.5);
        Vec3 endWorld   = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);

        // camera-relative
        Vec3 start = startWorld.subtract(cam);
        Vec3 end   = endWorld.subtract(cam);

        Vec3 dir = end.subtract(start);
        if (dir.lengthSqr() < 1.0E-6) return;
        Vec3 dirNorm = dir.normalize();

        // camera direction relative to start
        Vec3 toCam = new Vec3(0, 1, 0);
        // Right vector = perpendicular to beam & camera-ish direction
        Vec3 right = dirNorm.cross(toCam);
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        else right = right.normalize();

        Vec3 up = right.cross(dirNorm);
        if (up.lengthSqr() < 1.0E-6) up = new Vec3(0, 1, 0);
        else up = up.normalize();

        right = right.scale(thickness);
        up    = up.scale(thickness);

        // #D17DF3
        float r = 1.0f, g = 0.231f, b = 0.0f, a = 1.0f;

        // 5 lines to fake thickness (like your BER)
        addLine(vc, mat, r, g, b, a, start, end);
        addLine(vc, mat, r, g, b, a, start.add(right), end.add(right));
        addLine(vc, mat, r, g, b, a, start.subtract(right), end.subtract(right));
        addLine(vc, mat, r, g, b, a, start.add(up), end.add(up));
        addLine(vc, mat, r, g, b, a, start.subtract(up), end.subtract(up));
    }

    private static void addLine(VertexConsumer vc, Matrix4f mat,
                                float r, float g, float b, float a,
                                Vec3 start, Vec3 end) {

        vc.addVertex(mat, (float) start.x, (float) start.y, (float) start.z)
          .setColor(r, g, b, a)
          .setNormal(0f, 1f, 0f);

        vc.addVertex(mat, (float) end.x, (float) end.y, (float) end.z)
          .setColor(r, g, b, a)
          .setNormal(0f, 1f, 0f);
    }
}
