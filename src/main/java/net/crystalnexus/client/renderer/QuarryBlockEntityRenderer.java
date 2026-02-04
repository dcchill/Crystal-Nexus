package net.crystalnexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.crystalnexus.block.entity.QuarryBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

public class QuarryBlockEntityRenderer implements BlockEntityRenderer<QuarryBlockEntity> {

	public QuarryBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

	@Override
	public boolean shouldRenderOffScreen(QuarryBlockEntity be) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 512;
	}

	// NOTE: In some 1.21.x mappings this exists but doesn't override cleanly in MCreator.
	// Keep it WITHOUT @Override. If it still errors, delete this method.
	public boolean isGlobalRenderer(QuarryBlockEntity be) {
		return true;
	}

	@Override
	public boolean shouldRender(QuarryBlockEntity be, Vec3 camPos) {
		if (be.getTargetPos() == null) return false;

		double max = 512.0;
		return be.getBlockPos().getCenter().distanceToSqr(camPos) < (max * max);
	}

	@Override
	public void render(QuarryBlockEntity be, float partialTick, PoseStack poseStack,
	                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

		BlockPos target = be.getTargetPos();
		if (target == null) return;

		// Thickness: higher = thicker (0.02 small, 0.06 medium, 0.12 chunky)
		double thickness = 0.01;

		// Start at bottom center of quarry block (local space)
		Vec3 start = new Vec3(0.5, 0.05, 0.5);

		// End is target center converted into local space relative to this BE
		Vec3 endWorld = Vec3.atCenterOf(target);
		Vec3 end = new Vec3(
			endWorld.x - be.getBlockPos().getX(),
			endWorld.y - be.getBlockPos().getY(),
			endWorld.z - be.getBlockPos().getZ()
		);

		Vec3 dir = end.subtract(start);
		if (dir.lengthSqr() < 1.0E-6) return;

		Vec3 dirNorm = dir.normalize();

		// Camera-facing thickness direction
		var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
		Vec3 camWorld = cam.getPosition();
		Vec3 camLocal = camWorld.subtract(be.getBlockPos().getX(), be.getBlockPos().getY(), be.getBlockPos().getZ());

		Vec3 toCam = camLocal.subtract(start);
		if (toCam.lengthSqr() < 1.0E-6) toCam = new Vec3(0, 1, 0);
		else toCam = toCam.normalize();

		// Right vector perpendicular to beam and camera direction
		Vec3 right = dirNorm.cross(toCam);
		if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
		else right = right.normalize();

		// Up-ish vector perpendicular to beam and right
		Vec3 up = right.cross(dirNorm);
		if (up.lengthSqr() < 1.0E-6) up = new Vec3(0, 1, 0);
		else up = up.normalize();

		right = right.scale(thickness);
		up = up.scale(thickness);

		poseStack.pushPose();

		VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
		Matrix4f mat = poseStack.last().pose();

		// Color (RGBA) - #D17DF3
		float r = 1.0f, g = 0.231f, b = 0.0f, a = 0.0f;

		// Draw main beam + 4 offset beams to simulate thickness
		addLine(vc, mat, r, g, b, a, start, end);
		addLine(vc, mat, r, g, b, a, start.add(right), end.add(right));
		addLine(vc, mat, r, g, b, a, start.subtract(right), end.subtract(right));
		addLine(vc, mat, r, g, b, a, start.add(up), end.add(up));
		addLine(vc, mat, r, g, b, a, start.subtract(up), end.subtract(up));

		poseStack.popPose();
	}

	private static void addLine(VertexConsumer vc, Matrix4f mat,
	                            float r, float g, float b, float a,
	                            Vec3 start, Vec3 end) {

		// 1.21.x VertexConsumer API (MCreator mappings): addVertex + setColor + setNormal
		vc.addVertex(mat, (float) start.x, (float) start.y, (float) start.z)
			.setColor(r, g, b, a)
			.setNormal(0f, 1f, 0f);

		vc.addVertex(mat, (float) end.x, (float) end.y, (float) end.z)
			.setColor(r, g, b, a)
			.setNormal(0f, 1f, 0f);
	}
}
