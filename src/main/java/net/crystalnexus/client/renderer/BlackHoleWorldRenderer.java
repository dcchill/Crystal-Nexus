package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.crystalnexus.client.blackhole.BlackHoleVisualState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class BlackHoleWorldRenderer {
	private static final int SEGMENTS = 72;

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		long gameTime = mc.level.getGameTime();
		var visuals = BlackHoleVisualState.active(gameTime);
		if (visuals.isEmpty()) {
			return;
		}

		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		Matrix4f matrix = poseStack.last().pose();
		Vec3 camera = event.getCamera().getPosition();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		double renderTime = gameTime + partialTick;

		VertexConsumer glow = buffers.getBuffer(RenderType.lightning());

		for (BlackHoleVisualState.Visual visual : visuals) {
			renderBlackHole(glow, matrix, camera, visual, renderTime);
		}

		buffers.endBatch(RenderType.lightning());
	}

	private static void renderBlackHole(VertexConsumer glow, Matrix4f matrix, Vec3 camera, BlackHoleVisualState.Visual visual, double renderTime) {
		Vec3 centerWorld = visual.center();
		Vec3 centerRel = centerWorld.subtract(camera);
		Vector3f center = toVector(centerRel);

		Vector3f toCamera = new Vector3f(-center.x, -center.y, -center.z);
		if (toCamera.lengthSquared() < 1.0e-6F) {
			toCamera.set(0.0F, 0.0F, 1.0F);
		}
		toCamera.normalize();

		Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
		if (Math.abs(up.dot(toCamera)) > 0.94F) {
			up.set(1.0F, 0.0F, 0.0F);
		}

		Vector3f right = new Vector3f(up).cross(toCamera).normalize();
		up = new Vector3f(toCamera).cross(right).normalize();

		float fade = fade(visual, renderTime);
		float coreRadius = (float) Math.max(2.4D, visual.radius() * 0.42D);
		float rimInner = coreRadius * 1.015F;
		float rimOuter = coreRadius * 1.055F;

		drawBlackSphere(matrix, center, coreRadius, fade);
		drawBillboardRing(glow, matrix, center, right, up, rimInner, rimOuter, 245, 250, 255, (int) (230 * fade));
		drawBillboardRing(glow, matrix, center, right, up, coreRadius * 1.12F, coreRadius * 1.22F, 120, 48, 210, (int) (175 * fade));
		drawLensingDistortion(glow, matrix, center, right, up, coreRadius, renderTime, fade);
	}

	private static float fade(BlackHoleVisualState.Visual visual, double renderTime) {
		double age = renderTime - visual.startTick();
		double fadeIn = Math.min(age / 18.0D, 1.0D);
		double fadeOut = Math.min((visual.durationTicks() - age) / 30.0D, 1.0D);
		return (float) Math.max(0.0D, Math.min(fadeIn, fadeOut));
	}

	private static void drawAccretionDisk(VertexConsumer consumer, Matrix4f matrix, Vector3f center, double blackHoleRadius, double renderTime, float fade) {
		Vector3f major = new Vector3f(1.0F, 0.0F, 0.0F);
		Vector3f minor = new Vector3f(0.0F, 0.22F, 1.0F).normalize();
		float inner = (float) (blackHoleRadius * 0.34D);
		float outer = (float) (blackHoleRadius * 0.82D);
		float angleOffset = (float) (renderTime * 0.075D);

		for (int i = 0; i < SEGMENTS; i++) {
			float a0 = angleOffset + (float) (Math.PI * 2.0D * i / SEGMENTS);
			float a1 = angleOffset + (float) (Math.PI * 2.0D * (i + 1) / SEGMENTS);
			float heat0 = (float) ((Math.sin(a0 * 2.0F + renderTime * 0.05D) + 1.0D) * 0.5D);
			int r = 185 + (int) (55 * heat0);
			int g = 70 + (int) (80 * heat0);
			int b = 18 + (int) (95 * (1.0F - heat0));
			int alpha = (int) ((105 + 65 * heat0) * fade);

			Vector3f p0 = ellipsePoint(center, major, minor, inner, a0, 0.0F);
			Vector3f p1 = ellipsePoint(center, major, minor, outer, a0, wave(a0, renderTime));
			Vector3f p2 = ellipsePoint(center, major, minor, outer, a1, wave(a1, renderTime));
			Vector3f p3 = ellipsePoint(center, major, minor, inner, a1, 0.0F);
			quad(consumer, matrix, p0, p1, p2, p3, r, g, b, alpha);
		}
	}

	private static void drawAccretionWisps(VertexConsumer consumer, Matrix4f matrix, Vector3f center, double blackHoleRadius, double renderTime, float fade) {
		Vector3f major = new Vector3f(1.0F, 0.0F, 0.0F);
		Vector3f minor = new Vector3f(0.0F, 0.22F, 1.0F).normalize();
		for (int strand = 0; strand < 5; strand++) {
			float base = (float) (renderTime * (0.045D + strand * 0.006D) + strand * 1.257D);
			float radius = (float) (blackHoleRadius * (0.56D + strand * 0.07D));
			for (int i = 0; i < 18; i++) {
				float a0 = base + i * 0.18F;
				float a1 = base + (i + 1) * 0.18F;
				Vector3f p0 = ellipsePoint(center, major, minor, radius, a0, wave(a0, renderTime) * 0.65F);
				Vector3f p1 = ellipsePoint(center, major, minor, radius, a1, wave(a1, renderTime) * 0.65F);
				drawTubeSegment(consumer, matrix, p0, p1, 0.035F + strand * 0.006F, 126, 58, 255, (int) ((95 - strand * 9) * fade));
			}
		}
	}

	private static Vector3f ellipsePoint(Vector3f center, Vector3f major, Vector3f minor, float radius, float angle, float lift) {
		return new Vector3f(center)
				.add(new Vector3f(major).mul((float) Math.cos(angle) * radius))
				.add(new Vector3f(minor).mul((float) Math.sin(angle) * radius))
				.add(0.0F, lift, 0.0F);
	}

	private static float wave(float angle, double renderTime) {
		return (float) Math.sin(angle * 3.0F - renderTime * 0.11D) * 0.08F;
	}

	private static void drawBlackSphere(Matrix4f matrix, Vector3f center, float radius, float fade) {
		BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		int stacks = 14;
		int slices = 32;
		int alpha = (int) (255 * fade);

		for (int stack = 0; stack < stacks; stack++) {
			float v0 = stack / (float) stacks;
			float v1 = (stack + 1) / (float) stacks;
			float phi0 = (float) (-Math.PI / 2.0D + Math.PI * v0);
			float phi1 = (float) (-Math.PI / 2.0D + Math.PI * v1);

			for (int slice = 0; slice < slices; slice++) {
				float u0 = slice / (float) slices;
				float u1 = (slice + 1) / (float) slices;
				float theta0 = (float) (Math.PI * 2.0D * u0);
				float theta1 = (float) (Math.PI * 2.0D * u1);

				Vector3f p0 = spherePoint(center, radius, phi0, theta0);
				Vector3f p1 = spherePoint(center, radius, phi1, theta0);
				Vector3f p2 = spherePoint(center, radius, phi1, theta1);
				Vector3f p3 = spherePoint(center, radius, phi0, theta1);

				int shade = stack == 0 || stack == stacks - 1 ? 4 : 0;
				quad(builder, matrix, p0, p1, p2, p3, shade, shade, shade + 1, alpha);
			}
		}

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		BufferUploader.drawWithShader(builder.buildOrThrow());
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
	}

	private static Vector3f spherePoint(Vector3f center, float radius, float phi, float theta) {
		float cosPhi = (float) Math.cos(phi);
		return new Vector3f(center).add(
				(float) Math.cos(theta) * cosPhi * radius,
				(float) Math.sin(phi) * radius,
				(float) Math.sin(theta) * cosPhi * radius
		);
	}

	private static void drawBillboardDisk(VertexConsumer consumer, Matrix4f matrix, Vector3f center, Vector3f right, Vector3f up, float radius, int r, int g, int b, int alpha) {
		for (int i = 0; i < SEGMENTS; i++) {
			float a0 = (float) (Math.PI * 2.0D * i / SEGMENTS);
			float a1 = (float) (Math.PI * 2.0D * (i + 1) / SEGMENTS);
			Vector3f p0 = new Vector3f(center);
			Vector3f p1 = circlePoint(center, right, up, radius, a0);
			Vector3f p2 = circlePoint(center, right, up, radius, a1);
			quad(consumer, matrix, p0, p1, p2, p0, r, g, b, alpha);
		}
	}

	private static void drawBillboardRing(VertexConsumer consumer, Matrix4f matrix, Vector3f center, Vector3f right, Vector3f up, float inner, float outer, int r, int g, int b, int alpha) {
		for (int i = 0; i < SEGMENTS; i++) {
			float a0 = (float) (Math.PI * 2.0D * i / SEGMENTS);
			float a1 = (float) (Math.PI * 2.0D * (i + 1) / SEGMENTS);
			quad(consumer, matrix,
					circlePoint(center, right, up, inner, a0),
					circlePoint(center, right, up, outer, a0),
					circlePoint(center, right, up, outer, a1),
					circlePoint(center, right, up, inner, a1),
					r, g, b, alpha);
		}
	}

	private static void drawLensingDistortion(VertexConsumer consumer, Matrix4f matrix, Vector3f center, Vector3f right, Vector3f up, float coreRadius, double renderTime, float fade) {
		for (int layer = 0; layer < 4; layer++) {
			float baseRadius = coreRadius * (1.42F + layer * 0.18F);
			float thickness = coreRadius * (0.018F + layer * 0.004F);
			float wavePhase = (float) (renderTime * (0.035D + layer * 0.01D) + layer * 1.7D);
			int alpha = (int) ((48 - layer * 7) * fade);
			drawWarpedRing(consumer, matrix, center, right, up, baseRadius, thickness, wavePhase, 185, 210, 255, alpha);
		}
	}

	private static void drawWarpedRing(VertexConsumer consumer, Matrix4f matrix, Vector3f center, Vector3f right, Vector3f up, float radius, float thickness, float phase, int r, int g, int b, int alpha) {
		for (int i = 0; i < SEGMENTS; i++) {
			float a0 = (float) (Math.PI * 2.0D * i / SEGMENTS);
			float a1 = (float) (Math.PI * 2.0D * (i + 1) / SEGMENTS);
			float warp0 = 1.0F + (float) Math.sin(a0 * 5.0F + phase) * 0.035F + (float) Math.cos(a0 * 2.0F - phase * 0.7F) * 0.025F;
			float warp1 = 1.0F + (float) Math.sin(a1 * 5.0F + phase) * 0.035F + (float) Math.cos(a1 * 2.0F - phase * 0.7F) * 0.025F;
			quad(consumer, matrix,
					circlePoint(center, right, up, (radius - thickness) * warp0, a0),
					circlePoint(center, right, up, (radius + thickness) * warp0, a0),
					circlePoint(center, right, up, (radius + thickness) * warp1, a1),
					circlePoint(center, right, up, (radius - thickness) * warp1, a1),
					r, g, b, alpha);
		}
	}

	private static Vector3f circlePoint(Vector3f center, Vector3f right, Vector3f up, float radius, float angle) {
		return new Vector3f(center)
				.add(new Vector3f(right).mul((float) Math.cos(angle) * radius))
				.add(new Vector3f(up).mul((float) Math.sin(angle) * radius));
	}

	private static void drawTubeSegment(VertexConsumer consumer, Matrix4f matrix, Vector3f start, Vector3f end, float halfWidth, int r, int g, int b, int alpha) {
		Vector3f direction = new Vector3f(end).sub(start);
		if (direction.lengthSquared() < 1.0e-6F) {
			return;
		}
		direction.normalize();
		Vector3f side = new Vector3f(direction).cross(new Vector3f(0.0F, 1.0F, 0.0F));
		if (side.lengthSquared() < 1.0e-6F) {
			side.set(1.0F, 0.0F, 0.0F);
		}
		side.normalize().mul(halfWidth);
		quad(consumer, matrix, new Vector3f(start).add(side), new Vector3f(start).sub(side), new Vector3f(end).sub(side), new Vector3f(end).add(side), r, g, b, alpha);
	}

	private static Vector3f toVector(Vec3 value) {
		return new Vector3f((float) value.x, (float) value.y, (float) value.z);
	}

	private static void quad(VertexConsumer consumer, Matrix4f matrix, Vector3f a, Vector3f b, Vector3f c, Vector3f d, int r, int g, int blue, int alpha) {
		vertex(consumer, matrix, a, r, g, blue, alpha);
		vertex(consumer, matrix, b, r, g, blue, alpha);
		vertex(consumer, matrix, c, r, g, blue, alpha);
		vertex(consumer, matrix, d, r, g, blue, alpha);
	}

	private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vector3f point, int r, int g, int b, int alpha) {
		Vector4f transformed = new Vector4f(point.x, point.y, point.z, 1.0F).mul(matrix);
		consumer.addVertex(transformed.x, transformed.y, transformed.z).setColor(r, g, b, alpha);
	}
}
