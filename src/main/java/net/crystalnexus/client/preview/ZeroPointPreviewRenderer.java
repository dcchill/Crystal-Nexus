package net.crystalnexus.client.preview;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.crystalnexus.init.CrystalnexusModBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

public class ZeroPointPreviewRenderer {

    public record GhostBlock(int dx, int dy, int dz, BlockState state) {}

    // Mini-block scale (0..1)
    private static final float MINI_SCALE = 0.45f;

    // Pulse parameters (alpha)
    private static final float BASE_ALPHA  = 0.32f;
    private static final float PULSE_ALPHA = 0.5f;
    private static final float PULSE_SPEED = 0.12f;

    // Outline color/alpha
    private static final float OUTLINE_R = 0.2f;
    private static final float OUTLINE_G = 0.9f;
    private static final float OUTLINE_B = 1.0f;
    private static final float OUTLINE_A = 0.15f;

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        if (!ZeroPointPreviewState.isActive()) return;

        BlockPos controller = ZeroPointPreviewState.pos;
        if (!level.hasChunkAt(controller)) return;

        // Clear preview if controller removed/broken
        if (level.getBlockState(controller).getBlock() != CrystalnexusModBlocks.ZERO_POINT.get()) {
            ZeroPointPreviewState.clear();
            return;
        }

        List<GhostBlock> blocks = ZeroPointTemplates.getTemplate(ZeroPointPreviewState.templateId);
        if (blocks == null || blocks.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        // DeltaTracker -> partial tick float (your mappings)
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float tt = (((float) level.getGameTime()) + pt) * PULSE_SPEED;
        float pulse01 = 0.5f + 0.5f * (float) Math.sin(tt);
        float alpha = BASE_ALPHA + PULSE_ALPHA * pulse01;

        BlockRenderDispatcher brd = mc.getBlockRenderer();
        RenderType ghostType = Sheets.translucentCullBlockSheet();

        boolean allCorrect = true;

        // ---------- PASS 1: Ghost mini-blocks ----------
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        try {
            for (GhostBlock gb : blocks) {
                BlockPos worldPos = controller.offset(gb.dx, gb.dy, gb.dz);

                if (!level.hasChunkAt(worldPos)) {
                    allCorrect = false;
                    continue;
                }

                BlockState existing = level.getBlockState(worldPos);

                // Block-only compare for guide
                if (existing.getBlock() == gb.state().getBlock()) continue;

                allCorrect = false;

                int packedLight = LevelRenderer.getLightColor(level, worldPos);

                poseStack.pushPose();

                // Camera-relative translate
                poseStack.translate(
                        worldPos.getX() - cam.x,
                        worldPos.getY() - cam.y,
                        worldPos.getZ() - cam.z
                );

                // Center -> scale -> uncenter
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(MINI_SCALE, MINI_SCALE, MINI_SCALE);
                poseStack.translate(-0.5, -0.5, -0.5);

                BakedModel model = brd.getBlockModel(gb.state());

                VertexConsumer base = buffer.getBuffer(ghostType);
                VertexConsumer ghost = new AlphaVC(base, alpha);

                brd.getModelRenderer().renderModel(
                        poseStack.last(),
                        ghost,
                        gb.state(),
                        model,
                        1.0f, 1.0f, 1.0f,
                        packedLight,
                        OverlayTexture.NO_OVERLAY
                );

                poseStack.popPose();
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }

        // ---------- PASS 2: Outline boxes ----------
        // Use lines render type; draw in camera space (AABB moved by -cam)
        for (GhostBlock gb : blocks) {
            BlockPos worldPos = controller.offset(gb.dx, gb.dy, gb.dz);

            if (!level.hasChunkAt(worldPos)) continue;

            BlockState existing = level.getBlockState(worldPos);
            if (existing.getBlock() == gb.state().getBlock()) continue;

            AABB box = new AABB(worldPos).move(-cam.x, -cam.y, -cam.z);

            LevelRenderer.renderLineBox(
                    poseStack,
                    buffer.getBuffer(RenderType.lines()),
                    box,
                    OUTLINE_R, OUTLINE_G, OUTLINE_B, OUTLINE_A
            );
        }

        buffer.endBatch();

        // Auto-hide when complete
        if (allCorrect) {
            ZeroPointPreviewState.clear();
            if (mc.player != null) {
                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Multiblock complete!"));
            }
        }
    }

    /**
     * VertexConsumer wrapper for your 1.21-style API (MCreator mappings):
     * old vertex()/uv() do NOT exist; you have addVertex/setUv/setColor/etc.
     */
    private static class AlphaVC implements VertexConsumer {
        private final VertexConsumer d;
        private final float alphaMul;

        AlphaVC(VertexConsumer delegate, float alphaMul) {
            this.d = delegate;
            this.alphaMul = alphaMul;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            d.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            int na = (int) Math.max(0, Math.min(255, a * alphaMul));
            d.setColor(r, g, b, na);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            d.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            d.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            d.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            d.setNormal(x, y, z);
            return this;
        }
    }
}
