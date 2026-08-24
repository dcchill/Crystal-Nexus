package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.crystalnexus.block.entity.PlasmaGeneratorControllerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class PlasmaGeneratorRenderer implements BlockEntityRenderer<PlasmaGeneratorControllerBlockEntity> {
    private static final int SEGMENTS = 48;
    private static final RenderType PLASMA = RenderType.create("crystalnexus_plasma_generator",
        DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 4096, false, false,
        RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false));

    public PlasmaGeneratorRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public int getViewDistance() { return 192; }
    @Override public AABB getRenderBoundingBox(PlasmaGeneratorControllerBlockEntity controller) {
        Vec3 center = controller.getFormationCenter();
        return center == null ? new AABB(controller.getBlockPos()) : new AABB(center, center).inflate(5.0D);
    }

    @Override
    public void render(PlasmaGeneratorControllerBlockEntity controller, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Vec3 center = controller.getFormationCenter();
        if (!controller.isOperating() || center == null || controller.getLevel() == null) return;
        float time = controller.getLevel().getGameTime() + partialTick;
        poseStack.pushPose();
        poseStack.translate(center.x - controller.getBlockPos().getX(), center.y - controller.getBlockPos().getY(),
            center.z - controller.getBlockPos().getZ());
        VertexConsumer consumer = buffers.getBuffer(PLASMA);
        renderCore(poseStack, consumer, time);
        renderRing(poseStack, consumer, time, 0F, 0F, 0.0F);
        renderRing(poseStack, consumer, time, 58F, 24F, 2.1F);
        renderRing(poseStack, consumer, time, -48F, 67F, 4.2F);
        poseStack.popPose();
    }

    private static void renderCore(PoseStack poseStack, VertexConsumer consumer, float time) {
        float radius = 1.25F + Mth.sin(time * 0.22F) * 0.18F;
        poseStack.pushPose();
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        Matrix4f matrix = poseStack.last().pose();
        for (int segment = 0; segment < SEGMENTS; segment++) {
            float a0 = Mth.TWO_PI * segment / SEGMENTS;
            float a1 = Mth.TWO_PI * (segment + 1) / SEGMENTS;
            vertex(consumer, matrix, 0F, 0F, 0F, 224, 245, 255, 220);
            vertex(consumer, matrix, Mth.cos(a0) * radius, Mth.sin(a0) * radius, 0.01F, 104, 34, 255, 0);
            vertex(consumer, matrix, Mth.cos(a1) * radius, Mth.sin(a1) * radius, 0.01F, 34, 226, 255, 0);
        }
        poseStack.popPose();
    }

    private static void renderRing(PoseStack poseStack, VertexConsumer consumer, float time,
            float xRotation, float zRotation, float phase) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(xRotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(zRotation + time * (2.3F + phase * 0.2F)));
        Matrix4f matrix = poseStack.last().pose();
        float inner = 1.5F, outer = 1.72F;
        for (int segment = 0; segment < SEGMENTS; segment++) {
            float a0 = Mth.TWO_PI * segment / SEGMENTS;
            float a1 = Mth.TWO_PI * (segment + 1) / SEGMENTS;
            float wave0 = Mth.sin(a0 * 5F + time * 0.35F + phase) * 0.22F;
            float wave1 = Mth.sin(a1 * 5F + time * 0.35F + phase) * 0.22F;
            ringVertex(consumer, matrix, a0, inner, wave0, 70, 218, 255, 190);
            ringVertex(consumer, matrix, a0, outer, wave0, 185, 74, 255, 25);
            ringVertex(consumer, matrix, a1, outer, wave1, 185, 74, 255, 25);
            ringVertex(consumer, matrix, a0, inner, wave0, 70, 218, 255, 190);
            ringVertex(consumer, matrix, a1, outer, wave1, 185, 74, 255, 25);
            ringVertex(consumer, matrix, a1, inner, wave1, 70, 218, 255, 190);
        }
        poseStack.popPose();
    }

    private static void ringVertex(VertexConsumer consumer, Matrix4f matrix, float angle, float radius, float y,
            int red, int green, int blue, int alpha) {
        vertex(consumer, matrix, Mth.cos(angle) * radius, y, Mth.sin(angle) * radius, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
            int red, int green, int blue, int alpha) {
        consumer.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
    }
}
