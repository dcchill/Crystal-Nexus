package net.crystalnexus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.crystalnexus.client.preview.ConveyorPreviewState;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.util.ConveyorSplinePlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ConveyorSplinePreviewRenderer {
    private static final float BASE_ALPHA = 0.28f;
    private static final float PULSE_ALPHA = 0.22f;
    private static final float PULSE_SPEED = 0.15f;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (minecraft.player == null || level == null) {
            return;
        }

        ItemStack held = minecraft.player.getMainHandItem();
        if (!held.is(CrystalnexusModItems.CONVEYER_BELT.get()) && !minecraft.player.getOffhandItem().is(CrystalnexusModItems.CONVEYER_BELT.get())) {
            return;
        }

        BlockPos sourcePos = ConveyorPreviewState.getSource();
        if (sourcePos == null || !level.isLoaded(sourcePos)) {
            return;
        }

        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() == HitResult.Type.MISS) {
            return;
        }

        BlockPos targetPos = hit.getBlockPos();
        if (sourcePos.equals(targetPos) || !ConveyorSplinePlanner.isConveyorBlock(level.getBlockState(targetPos))) {
            return;
        }

        ConveyorSplinePlanner.ConnectionPlan plan = ConveyorSplinePlanner.plan(level, sourcePos, targetPos, ConveyorPreviewState.getPathMode());
        if (plan == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Vec3 cam = minecraft.gameRenderer.getMainCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float pulse = 0.5f + 0.5f * (float) Math.sin((level.getGameTime() + partialTick) * PULSE_SPEED);
        float alpha = BASE_ALPHA + PULSE_ALPHA * pulse;

        renderGhostBelts(level, minecraft, poseStack, buffers, cam, plan, alpha);
        renderSplineLine(poseStack, buffers, cam, plan, alpha);
        buffers.endBatch();
    }

    private static void renderGhostBelts(Level level, Minecraft minecraft, PoseStack poseStack, MultiBufferSource.BufferSource buffers, Vec3 cam, ConveyorSplinePlanner.ConnectionPlan plan, float alpha) {
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        RenderType ghostType = Sheets.translucentCullBlockSheet();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        try {
            for (ConveyorSplinePlanner.PlacedBelt belt : plan.placedBelts()) {
                if (!level.hasChunkAt(belt.pos())) {
                    continue;
                }

                int light = LevelRenderer.getLightColor(level, belt.pos());
                poseStack.pushPose();
                poseStack.translate(belt.pos().getX() - cam.x, belt.pos().getY() - cam.y, belt.pos().getZ() - cam.z);

                BakedModel model = dispatcher.getBlockModel(belt.state());
                dispatcher.getModelRenderer().renderModel(
                        poseStack.last(),
                        new AlphaVertexConsumer(buffers.getBuffer(ghostType), alpha),
                        belt.state(),
                        model,
                        1.0f, 1.0f, 1.0f,
                        light,
                        OverlayTexture.NO_OVERLAY
                );
                poseStack.popPose();

                AABB box = new AABB(belt.pos()).move(-cam.x, -cam.y, -cam.z);
                LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), box, 0.18f, 0.85f, 1.0f, 0.18f + alpha * 0.45f);
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    private static void renderSplineLine(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Vec3 cam, ConveyorSplinePlanner.ConnectionPlan plan, float alpha) {
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        int a = Math.max(80, Math.min(255, (int) (alpha * 255.0f)));

        for (int i = 0; i < plan.splinePoints().size() - 1; i++) {
            Vec3 start = plan.splinePoints().get(i).subtract(cam);
            Vec3 end = plan.splinePoints().get(i + 1).subtract(cam);
            addLineVertex(lines, matrix, start, 70, 220, 255, a);
            addLineVertex(lines, matrix, end, 70, 220, 255, a);
        }
    }

    private static void addLineVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 point, int r, int g, int b, int a) {
        Vector4f transformed = new Vector4f((float) point.x, (float) point.y, (float) point.z, 1.0f).mul(matrix);
        consumer.addVertex(transformed.x, transformed.y, transformed.z).setColor(r, g, b, a).setNormal(0.0f, 1.0f, 0.0f);
    }

    private static class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;

        private AlphaVertexConsumer(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            delegate.setColor(r, g, b, Math.max(0, Math.min(255, (int) (a * alpha))));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
