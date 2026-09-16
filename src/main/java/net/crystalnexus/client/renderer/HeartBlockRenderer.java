package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.crystalnexus.block.entity.HeartBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class HeartBlockRenderer implements BlockEntityRenderer<HeartBlockEntity> {
    public HeartBlockRenderer(BlockEntityRendererProvider.Context context) { }

    @Override public void render(HeartBlockEntity heart, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderModel(heart.getBlockState(), heart.isFormed(), heart.getFacing(), heart.beatAge(partialTick), pose, buffers, light, overlay);
    }

    /** Also used by the construction guide, where no world block entity exists. */
    public static void renderModel(BlockState state, boolean formed, Direction facing, float beatAge,
                                   PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float contraction = beatAge >= 0 && beatAge < 8
            ? (beatAge < 2 ? beatAge / 2 : (8 - beatAge) / 6) : 0;
        float scale = formed ? 2.5F * (1 - 0.08F * contraction) : 1;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        pose.scale(scale, scale, scale);
        pose.translate(-0.5, -0.5, -0.5);
        var renderer = Minecraft.getInstance().getBlockRenderer();
        renderer.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)),
            state, renderer.getBlockModel(state), 1, 1, 1, light, overlay);
        pose.popPose();
    }

    @Override public AABB getRenderBoundingBox(HeartBlockEntity heart) {
        return new AABB(heart.getBlockPos()).inflate(0.75);
    }
}
