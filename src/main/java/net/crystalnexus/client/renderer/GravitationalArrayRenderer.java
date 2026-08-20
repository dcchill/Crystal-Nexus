package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.crystalnexus.block.entity.GravitationalArrayControllerBlockEntity;
import net.crystalnexus.recipe.GravitationalArrayRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class GravitationalArrayRenderer implements BlockEntityRenderer<GravitationalArrayControllerBlockEntity> {
    private static final int HALO_SEGMENTS = 32;
    private final ItemRenderer itemRenderer;

    public GravitationalArrayRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(GravitationalArrayControllerBlockEntity controller) {
        Vec3 center = controller.getFormationRenderCenter();
        return center == null ? new AABB(controller.getBlockPos()) : new AABB(
            Math.min(controller.getBlockPos().getX(), center.x) - 4,
            Math.min(controller.getBlockPos().getY(), center.y) - 4,
            Math.min(controller.getBlockPos().getZ(), center.z) - 4,
            Math.max(controller.getBlockPos().getX() + 1, center.x) + 4,
            Math.max(controller.getBlockPos().getY() + 1, center.y) + 4,
            Math.max(controller.getBlockPos().getZ() + 1, center.z) + 4);
    }

    @Override
    public void render(GravitationalArrayControllerBlockEntity controller, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Vec3 center = controller.getFormationRenderCenter();
        GravitationalArrayRecipe recipe = controller.getActiveRecipeForRender().orElse(null);
        if (center == null || recipe == null || controller.getActiveDuration() <= 0 || controller.getLevel() == null) return;

        float fraction = Mth.clamp(controller.getProgress() / (float) controller.getActiveDuration(), 0.0F, 1.0F);
        float smooth = fraction * fraction * (3.0F - 2.0F * fraction);
        float recipeScale = recipe.visuals().map(GravitationalArrayRecipe.Visuals::scale).orElse(1.0F);
        float time = controller.getLevel().getGameTime() + partialTick;
        float pulse = 1.0F + Mth.sin(time * 0.18F) * (0.04F + fraction * 0.05F);
        float scale = (0.45F + smooth * 2.35F) * recipeScale * pulse;

        poseStack.pushPose();
        poseStack.translate(center.x - controller.getBlockPos().getX(),
            center.y - controller.getBlockPos().getY(), center.z - controller.getBlockPos().getZ());
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        renderHalo(poseStack, buffers, scale, time, recipe);
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * (0.7F + fraction * 1.4F)));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(recipe.output(), ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY, poseStack, buffers, controller.getLevel(),
            (int) controller.getBlockPos().asLong());
        poseStack.popPose();
    }

    private static void renderHalo(PoseStack poseStack, MultiBufferSource buffers, float scale, float time,
                                   GravitationalArrayRecipe recipe) {
        GravitationalArrayRecipe.Visuals visuals = recipe.visuals()
            .orElse(new GravitationalArrayRecipe.Visuals(1.0F, 0.85F, 0.25F, 1.0F));
        float pulse = 1.0F + Mth.sin(time * 0.12F) * 0.08F;
        float inner = scale * 0.68F * pulse;
        float outer = scale * 0.84F * pulse;
        int red = (int) (Mth.clamp(visuals.red(), 0.0F, 1.0F) * 255);
        int green = (int) (Mth.clamp(visuals.green(), 0.0F, 1.0F) * 255);
        int blue = (int) (Mth.clamp(visuals.blue(), 0.0F, 1.0F) * 255);
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < HALO_SEGMENTS; i++) {
            float angle0 = Mth.TWO_PI * i / HALO_SEGMENTS;
            float angle1 = Mth.TWO_PI * (i + 1) / HALO_SEGMENTS;
            haloVertex(consumer, matrix, inner, angle0, red, green, blue, 125);
            haloVertex(consumer, matrix, outer, angle0, red, green, blue, 35);
            haloVertex(consumer, matrix, outer, angle1, red, green, blue, 35);
            haloVertex(consumer, matrix, inner, angle1, red, green, blue, 125);
        }
    }

    private static void haloVertex(VertexConsumer consumer, Matrix4f matrix, float radius, float angle,
                                   int red, int green, int blue, int alpha) {
        consumer.addVertex(matrix, Mth.cos(angle) * radius, Mth.sin(angle) * radius, 0.02F)
            .setColor(red, green, blue, alpha);
    }
}
