package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.crystalnexus.block.entity.SolarEngineControllerBlockEntity;
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

public final class SolarEngineRenderer implements BlockEntityRenderer<SolarEngineControllerBlockEntity> {
	private static final int HALO_SEGMENTS = 32;
	private static final RenderType GLOW = RenderType.create("crystalnexus_solar_engine_glow",
		DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 2048, false, false,
		RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
			.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
			.setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setCullState(RenderStateShard.NO_CULL)
			.setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false));
	private final ItemRenderer itemRenderer;

	public SolarEngineRenderer(BlockEntityRendererProvider.Context context) { itemRenderer = context.getItemRenderer(); }
	@Override public int getViewDistance() { return 192; }
	@Override public AABB getRenderBoundingBox(SolarEngineControllerBlockEntity controller) {
		Vec3 center = controller.getFormationCenter();
		return center == null ? new AABB(controller.getBlockPos()) : new AABB(center, center).inflate(4.0D);
	}

	@Override
	public void render(SolarEngineControllerBlockEntity controller, float partialTick, PoseStack poseStack,
			MultiBufferSource buffers, int packedLight, int packedOverlay) {
		Vec3 center = controller.getFormationCenter();
		ItemStack star = controller.getItem(0);
		if (!controller.isFormed() || center == null || star.isEmpty() || controller.getLevel() == null) return;
		float time = controller.getLevel().getGameTime() + partialTick;
		poseStack.pushPose();
		poseStack.translate(center.x - controller.getBlockPos().getX(), center.y - controller.getBlockPos().getY(),
			center.z - controller.getBlockPos().getZ());
		renderGlow(star, poseStack, buffers, time);
		poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.8F % 360.0F));
		poseStack.scale(2.5F, 2.5F, 2.5F);
		itemRenderer.renderStatic(star, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
			poseStack, buffers, controller.getLevel(), (int) controller.getBlockPos().asLong());
		poseStack.popPose();
	}

	private static void renderGlow(ItemStack star, PoseStack poseStack, MultiBufferSource buffers, float time) {
		int color = star.is(CrystalnexusModItems.BLUE_STAR.get()) ? 0x62C8FF
			: star.is(CrystalnexusModItems.PINK_STAR.get()) ? 0xFF62D8
			: star.is(CrystalnexusModItems.ORANGE_STAR.get()) ? 0xFF8A32 : 0xFFE06A;
		float radius = 3.3F * (1.0F + Mth.sin(time * 0.16F) * 0.08F);
		poseStack.pushPose();
		poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
		VertexConsumer consumer = buffers.getBuffer(GLOW);
		Matrix4f matrix = poseStack.last().pose();
		for (int segment = 0; segment < HALO_SEGMENTS; segment++) {
			float angle0 = Mth.TWO_PI * segment / HALO_SEGMENTS;
			float angle1 = Mth.TWO_PI * (segment + 1) / HALO_SEGMENTS;
			vertex(consumer, matrix, 0, 0, color, 185);
			vertex(consumer, matrix, radius, angle0, color, 0);
			vertex(consumer, matrix, radius, angle1, color, 0);
		}
		poseStack.popPose();
	}

	private static void vertex(VertexConsumer consumer, Matrix4f matrix, float radius, float angle, int color, int alpha) {
		consumer.addVertex(matrix, Mth.cos(angle) * radius, Mth.sin(angle) * radius, 0.02F)
			.setColor(color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
	}
}
