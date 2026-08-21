package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.crystalnexus.block.entity.SolarSimulatorControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;

public final class SolarSimulatorRenderer implements BlockEntityRenderer<SolarSimulatorControllerBlockEntity> {
    private static final int STACKS = 32;
    private static final int SLICES = 64;
    private static final int HALO_SEGMENTS = 32;
    private static final float[][] BACKGROUND_STARS = createBackgroundStars();
    private static final RenderType VOID_RENDER_TYPE = RenderType.create(
        "crystalnexus_solar_simulator_void",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        8192,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
            .createCompositeState(false)
    );
    private static final RenderType GLOW_RENDER_TYPE = RenderType.create(
        "crystalnexus_solar_simulator_glow",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.TRIANGLES,
        2048,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(false)
    );
    
    private final ItemRenderer itemRenderer;

    public SolarSimulatorRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override public int getViewDistance() { return 192; }

    @Override public AABB getRenderBoundingBox(SolarSimulatorControllerBlockEntity controller) {
        Vec3 center = controller.getFormationCenter();
        return center == null ? new AABB(controller.getBlockPos()) : new AABB(center, center).inflate(5.0D);
    }

    @Override
    public void render(SolarSimulatorControllerBlockEntity controller, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Vec3 center = controller.getFormationCenter();
        if (!controller.isRenderActive() || center == null || controller.getLevel() == null) return;

        double time = controller.getLevel().getGameTime() + partialTick;
        poseStack.pushPose();
        poseStack.translate(center.x - controller.getBlockPos().getX(),
            center.y - controller.getBlockPos().getY(), center.z - controller.getBlockPos().getZ());

        Vec3 cameraOffset = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
            .subtract(center);
        float voidRadius = 3.85F + (float) Math.sin(time * 0.04D) * 0.08F;
        VertexConsumer backdrop = buffers.getBuffer(VOID_RENDER_TYPE);
        drawVoidSphere(poseStack.last().pose(), backdrop, voidRadius, cameraOffset);
        drawBackgroundStars(poseStack.last().pose(), backdrop, voidRadius - 0.06F, cameraOffset);

        ItemStack star = controller.getItem(4);
        renderSunGlow(star, poseStack, buffers, (float) time);
        renderBody(star, poseStack, buffers, (float) (time * 1.8D % 360.0D), 1.15F, controller);

        for (int slot = 0; slot < 4; slot++) {
            ItemStack planet = controller.getItem(slot);
            if (planet.isEmpty()) continue;
            float radius = 1.35F + slot * 0.62F;
            double speed = 0.025D + slot * 0.004D;
            float angle = (float) (time * speed % Mth.TWO_PI) + slot * Mth.HALF_PI;
            float verticalAngle = (float) (time * speed * 0.73D % Mth.TWO_PI) + slot;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * radius,
                Mth.sin(verticalAngle) * 0.28F, Mth.sin(angle) * radius);
            renderBody(planet, poseStack, buffers,
                (float) (-time * (1.2D + slot * 0.2D) % 360.0D), 0.58F, controller);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private void renderBody(ItemStack stack, PoseStack poseStack, MultiBufferSource buffers, float rotation,
                            float scale, SolarSimulatorControllerBlockEntity controller) {
        if (stack.isEmpty()) return;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY, poseStack, buffers, controller.getLevel(),
            (int) (controller.getBlockPos().asLong() + stack.getItem().hashCode()));
        poseStack.popPose();
    }

    private static void renderSunGlow(ItemStack star, PoseStack poseStack, MultiBufferSource buffers, float time) {
        if (star.isEmpty()) return;
        int color = star.is(CrystalnexusModItems.BLUE_STAR.get()) ? 0x62C8FF
            : star.is(CrystalnexusModItems.PINK_STAR.get()) ? 0xFF62D8
            : star.is(CrystalnexusModItems.ORANGE_STAR.get()) ? 0xFF8A32 : 0xFFE06A;
        float pulse = 1.0F + Mth.sin(time * 0.16F) * 0.08F;
        float outer = 1.48F * pulse;

        poseStack.pushPose();
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        VertexConsumer consumer = buffers.getBuffer(GLOW_RENDER_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        for (int segment = 0; segment < HALO_SEGMENTS; segment++) {
            float angle0 = Mth.TWO_PI * segment / HALO_SEGMENTS;
            float angle1 = Mth.TWO_PI * (segment + 1) / HALO_SEGMENTS;
            haloVertex(consumer, matrix, 0.0F, 0.0F, color, 185);
            haloVertex(consumer, matrix, outer, angle0, color, 0);
            haloVertex(consumer, matrix, outer, angle1, color, 0);
        }
        poseStack.popPose();
    }

    private static void haloVertex(VertexConsumer consumer, Matrix4f matrix, float radius, float angle,
                                   int color, int alpha) {
        consumer.addVertex(matrix, Mth.cos(angle) * radius, Mth.sin(angle) * radius, 0.02F)
            .setColor(color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
    }

    private static void drawVoidSphere(Matrix4f matrix, VertexConsumer consumer, float radius, Vec3 cameraOffset) {
        for (int stack = 0; stack < STACKS; stack++) {
            float phi0 = -Mth.HALF_PI + Mth.PI * stack / STACKS;
            float phi1 = -Mth.HALF_PI + Mth.PI * (stack + 1) / STACKS;
            for (int slice = 0; slice < SLICES; slice++) {
                float theta0 = Mth.TWO_PI * slice / SLICES;
                float theta1 = Mth.TWO_PI * (slice + 1) / SLICES;
                float middlePhi = (phi0 + phi1) * 0.5F;
                float middleTheta = (theta0 + theta1) * 0.5F;
                float middleCosPhi = Mth.cos(middlePhi);
                double facing = Mth.cos(middleTheta) * middleCosPhi * cameraOffset.x
                    + Mth.sin(middlePhi) * cameraOffset.y
                    + Mth.sin(middleTheta) * middleCosPhi * cameraOffset.z - radius;
                if (facing >= 0.0D) continue;

                vertex(consumer, matrix, radius, phi0, theta0);
                vertex(consumer, matrix, radius, phi1, theta0);
                vertex(consumer, matrix, radius, phi1, theta1);
                vertex(consumer, matrix, radius, phi0, theta1);
            }
        }
    }

    private static void drawBackgroundStars(Matrix4f matrix, VertexConsumer consumer, float radius,
                                            Vec3 cameraOffset) {
        for (float[] star : BACKGROUND_STARS) {
            float phi = star[0];
            float theta = star[1];
            float cosPhi = Mth.cos(phi);
            double facing = Mth.cos(theta) * cosPhi * cameraOffset.x + Mth.sin(phi) * cameraOffset.y
                + Mth.sin(theta) * cosPhi * cameraOffset.z - radius;
            if (facing >= 0.0D) continue;

            float size = star[2];
            int brightness = (int) star[3];
            starVertex(consumer, matrix, radius, phi - size, theta - size, brightness);
            starVertex(consumer, matrix, radius, phi + size, theta - size, brightness);
            starVertex(consumer, matrix, radius, phi + size, theta + size, brightness);
            starVertex(consumer, matrix, radius, phi - size, theta + size, brightness);
        }
    }

    private static float[][] createBackgroundStars() {
        Random random = new Random(0x534F4C41524CL);
        float[][] stars = new float[110][4];
        for (float[] star : stars) {
            star[0] = (float) Math.asin(random.nextDouble() * 2.0D - 1.0D);
            star[1] = random.nextFloat() * Mth.TWO_PI;
            star[2] = 0.007F + random.nextFloat() * 0.012F;
            star[3] = 175 + random.nextInt(81);
        }
        return stars;
    }

    private static void starVertex(VertexConsumer consumer, Matrix4f matrix, float radius, float phi,
                                   float theta, int brightness) {
        float cosPhi = Mth.cos(phi);
        consumer.addVertex(matrix, Mth.cos(theta) * cosPhi * radius, Mth.sin(phi) * radius,
            Mth.sin(theta) * cosPhi * radius).setColor(brightness, brightness, 255, 255);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float radius, float phi, float theta) {
        float cosPhi = Mth.cos(phi);
        consumer.addVertex(matrix, Mth.cos(theta) * cosPhi * radius, Mth.sin(phi) * radius,
            Mth.sin(theta) * cosPhi * radius).setColor(0, 0, 2, 255);
    }
}
