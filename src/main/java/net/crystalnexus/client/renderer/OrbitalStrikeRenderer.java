package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.OrbitalStrikeRemoteItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class OrbitalStrikeRenderer {
	private static final double TARGET_RANGE = 180.0D;
	private static final int CIRCLE_SEGMENTS = 72;
	private static final List<StrikeVisual> STRIKES = new CopyOnWriteArrayList<>();

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		LocalPlayer player = mc.player;
		if (level == null || player == null) {
			return;
		}

		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		BlockHitResult targetHit = heldOrbitalRemote(player) ? raycast(level, player, partialTick) : null;
		boolean hasTarget = targetHit != null && targetHit.getType() == HitResult.Type.BLOCK;
		boolean hasBeam = !STRIKES.isEmpty();
		if (!hasTarget && !hasBeam) {
			return;
		}

		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
		Matrix4f matrix = poseStack.last().pose();
		Vec3 camera = event.getCamera().getPosition();
		double renderTime = level.getGameTime() + partialTick;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();

		if (hasTarget) {
			renderGroundTarget(consumer, matrix, targetHit, camera, renderTime);
		}
		if (hasBeam) {
			renderStrikes(consumer, matrix, camera, renderTime);
		}

		buffers.endBatch(RenderType.lightning());
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	public static void addStrike(double x, double y, double z, double skyY, int durationTicks, int impactDelayTicks) {
		Minecraft mc = Minecraft.getInstance();
		long startTick = mc.level == null ? 0L : mc.level.getGameTime();
		STRIKES.add(new StrikeVisual(new Vec3(x, y, z), skyY, durationTicks, impactDelayTicks, startTick));
	}

	private static boolean heldOrbitalRemote(LocalPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof OrbitalStrikeRemoteItem) {
				return true;
			}
		}
		return false;
	}

	private static BlockHitResult raycast(Level level, LocalPlayer player, float partialTick) {
		Vec3 eye = player.getEyePosition(partialTick);
		Vec3 look = player.getLookAngle().normalize();
		Vec3 end = eye.add(look.scale(TARGET_RANGE));
		return level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
	}

	private static void renderGroundTarget(VertexConsumer consumer, Matrix4f matrix, BlockHitResult hit, Vec3 camera, double renderTime) {
		BlockPos pos = hit.getBlockPos();
		Vec3 location = hit.getLocation();
		double y = pos.getY() + 1.035D;
		Vector3f center = toVector(new Vec3(location.x, y, location.z).subtract(camera));
		float pulse = 0.5F + 0.5F * (float) Math.sin(renderTime * 0.45D);
		float spin = (float) (renderTime * 0.08D);

		drawHorizontalRing(consumer, matrix, center, 1.12F, 1.28F, 245, 245, 255, 150 + (int) (65 * pulse));
		drawHorizontalRing(consumer, matrix, center, 0.32F, 0.42F, 255, 74, 38, 185);
		drawHorizontalStrip(consumer, matrix, center, spin, 1.55F, 0.08F, 255, 74, 38, 210);
		drawHorizontalStrip(consumer, matrix, center, spin + (float) (Math.PI * 0.5D), 1.55F, 0.08F, 255, 74, 38, 210);
	}

	private static void renderStrikes(VertexConsumer consumer, Matrix4f matrix, Vec3 camera, double renderTime) {
		Iterator<StrikeVisual> iterator = STRIKES.iterator();
		while (iterator.hasNext()) {
			StrikeVisual strike = iterator.next();
			double age = renderTime - strike.startTick();
			if (age > strike.durationTicks()) {
				STRIKES.remove(strike);
				continue;
			}

			float fadeIn = (float) Math.min(1.0D, age / 3.0D);
			float fadeOut = (float) Math.min(1.0D, (strike.durationTicks() - age) / 10.0D);
			float alpha = Math.max(0.0F, Math.min(fadeIn, fadeOut));
			renderBeam(consumer, matrix, strike, camera, renderTime, alpha);
			renderImpactGlow(consumer, matrix, strike.target().subtract(camera), renderTime, alpha);
			renderImpactFrame(consumer, matrix, strike, camera, age);
		}
	}

	private static void renderBeam(VertexConsumer consumer, Matrix4f matrix, StrikeVisual strike, Vec3 camera, double renderTime, float alpha) {
		Vector3f bottom = toVector(strike.target().subtract(camera));
		Vector3f top = toVector(new Vec3(strike.target().x, strike.skyY(), strike.target().z).subtract(camera));
		float shimmer = 0.06F * (float) Math.sin(renderTime * 1.7D);

		drawVerticalTube(consumer, matrix, bottom, top, 0.44F + shimmer, 255, 255, 255, (int) (235 * alpha));
		drawVerticalTube(consumer, matrix, bottom, top, 0.76F + shimmer, 80, 225, 255, (int) (150 * alpha));
		drawVerticalTube(consumer, matrix, bottom, top, 1.12F + shimmer, 38, 138, 255, (int) (70 * alpha));
	}

	private static void renderImpactGlow(VertexConsumer consumer, Matrix4f matrix, Vec3 relativeTarget, double renderTime, float alpha) {
		Vector3f center = toVector(relativeTarget.add(0.0D, 0.05D, 0.0D));
		float pulse = 0.5F + 0.5F * (float) Math.sin(renderTime * 0.9D);
		drawHorizontalRing(consumer, matrix, center, 1.0F, 2.2F + pulse * 0.45F, 255, 255, 255, (int) (120 * alpha));
		drawHorizontalRing(consumer, matrix, center, 2.4F, 3.0F + pulse * 0.6F, 52, 186, 255, (int) (80 * alpha));
	}

	private static void renderImpactFrame(VertexConsumer consumer, Matrix4f matrix, StrikeVisual strike, Vec3 camera, double age) {
		double frameAge = age - strike.impactDelayTicks();
		if (frameAge < 0.0D || frameAge > 5.0D) {
			return;
		}

		float punch = 1.0F - (float) (frameAge / 5.0D);
		Vector3f center = toVector(strike.target().subtract(camera).add(0.0D, 0.07D, 0.0D));
		Vector3f top = toVector(new Vec3(strike.target().x, strike.skyY(), strike.target().z).subtract(camera));
		float radius = 3.0F + (1.0F - punch) * 6.0F;

		drawHorizontalDisk(consumer, matrix, center, radius, 255, 255, 255, (int) (230 * punch));
		drawHorizontalRing(consumer, matrix, center, radius, radius + 1.2F, 56, 210, 255, (int) (210 * punch));
		drawVerticalTube(consumer, matrix, center, top, 1.65F + (1.0F - punch) * 0.8F, 255, 255, 255, (int) (245 * punch));
	}

	private static void drawVerticalTube(VertexConsumer consumer, Matrix4f matrix, Vector3f bottom, Vector3f top, float radius, int r, int g, int b, int alpha) {
		for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
			float a0 = (float) (Math.PI * 2.0D * i / CIRCLE_SEGMENTS);
			float a1 = (float) (Math.PI * 2.0D * (i + 1) / CIRCLE_SEGMENTS);
			Vector3f b0 = aroundY(bottom, radius, a0);
			Vector3f b1 = aroundY(bottom, radius, a1);
			Vector3f t1 = aroundY(top, radius, a1);
			Vector3f t0 = aroundY(top, radius, a0);
			quad(consumer, matrix, b0, b1, t1, t0, r, g, b, alpha);
		}
	}

	private static void drawHorizontalRing(VertexConsumer consumer, Matrix4f matrix, Vector3f center, float inner, float outer, int r, int g, int b, int alpha) {
		for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
			float a0 = (float) (Math.PI * 2.0D * i / CIRCLE_SEGMENTS);
			float a1 = (float) (Math.PI * 2.0D * (i + 1) / CIRCLE_SEGMENTS);
			quad(consumer, matrix,
					horizontalPoint(center, inner, a0),
					horizontalPoint(center, outer, a0),
					horizontalPoint(center, outer, a1),
					horizontalPoint(center, inner, a1),
					r, g, b, alpha);
		}
	}

	private static void drawHorizontalDisk(VertexConsumer consumer, Matrix4f matrix, Vector3f center, float radius, int r, int g, int b, int alpha) {
		for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
			float a0 = (float) (Math.PI * 2.0D * i / CIRCLE_SEGMENTS);
			float a1 = (float) (Math.PI * 2.0D * (i + 1) / CIRCLE_SEGMENTS);
			quad(consumer, matrix,
					new Vector3f(center),
					horizontalPoint(center, radius, a0),
					horizontalPoint(center, radius, a1),
					new Vector3f(center),
					r, g, b, alpha);
		}
	}

	private static void drawHorizontalStrip(VertexConsumer consumer, Matrix4f matrix, Vector3f center, float angle, float halfLength, float halfWidth, int r, int g, int b, int alpha) {
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);
		Vector3f along = new Vector3f(cos, 0.0F, sin);
		Vector3f side = new Vector3f(-sin, 0.0F, cos);

		quad(consumer, matrix,
				stripPoint(center, along, side, -halfLength, -halfWidth),
				stripPoint(center, along, side, halfLength, -halfWidth),
				stripPoint(center, along, side, halfLength, halfWidth),
				stripPoint(center, along, side, -halfLength, halfWidth),
				r, g, b, alpha);
	}

	private static Vector3f stripPoint(Vector3f center, Vector3f along, Vector3f side, float length, float width) {
		return new Vector3f(center)
				.add(new Vector3f(along).mul(length))
				.add(new Vector3f(side).mul(width));
	}

	private static Vector3f aroundY(Vector3f center, float radius, float angle) {
		return new Vector3f(center.x + (float) Math.cos(angle) * radius, center.y, center.z + (float) Math.sin(angle) * radius);
	}

	private static Vector3f horizontalPoint(Vector3f center, float radius, float angle) {
		return aroundY(center, radius, angle);
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

	private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vector3f point, int r, int g, int b, int a) {
		Vector4f transformed = new Vector4f(point.x, point.y, point.z, 1.0F).mul(matrix);
		consumer.addVertex(transformed.x, transformed.y, transformed.z).setColor(r, g, b, a);
	}

	private record StrikeVisual(Vec3 target, double skyY, int durationTicks, int impactDelayTicks, long startTick) {
	}
}
