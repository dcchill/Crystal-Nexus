package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.crystalnexus.item.MiningLaserItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class MiningLaserBeamRenderer {
	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || !player.isUsingItem()) {
			return;
		}

		ItemStack usedStack = player.getUseItem();
		if (!(usedStack.getItem() instanceof MiningLaserItem)) {
			return;
		}

		renderBeam(event, player, player.getUsedItemHand());
	}

	private static void renderBeam(RenderLevelStageEvent event, LocalPlayer player, InteractionHand hand) {
		Minecraft mc = Minecraft.getInstance();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		Vec3 look = player.getLookAngle().normalize();
		Vec3 eye = player.getEyePosition(partialTick);
		Vec3 target = eye.add(look.scale(MiningLaserItem.range()));
		HitResult hit = player.level().clip(new ClipContext(eye, target, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		Vec3 endWorld = hit.getType() == HitResult.Type.MISS ? target : hit.getLocation();
		Vec3 startWorld = getLaserTip(player, partialTick, look, hand);
		Vec3 camera = event.getCamera().getPosition();

		Vector3f start = toVector(startWorld.subtract(camera));
		Vector3f end = toVector(endWorld.subtract(camera));
		Vector3f forward = new Vector3f(end).sub(start);
		if (forward.lengthSquared() < 1.0e-6F) {
			return;
		}
		forward.normalize();

		Vector3f toCamera = new Vector3f(-start.x, -start.y, -start.z);
		if (toCamera.lengthSquared() < 1.0e-6F) {
			toCamera.set(0.0F, 1.0F, 0.0F);
		}
		toCamera.normalize();

		Vector3f side = new Vector3f(forward).cross(toCamera);
		if (side.lengthSquared() < 1.0e-6F) {
			side = new Vector3f(forward).cross(new Vector3f(0.0F, 1.0F, 0.0F));
		}
		side.normalize();
		Vector3f up = new Vector3f(side).cross(forward).normalize();

		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
		Matrix4f matrix = poseStack.last().pose();
		double renderTime = player.level().getGameTime() + partialTick;

		drawLaser(consumer, matrix, start, end, side, up, renderTime);

		buffer.endBatch(RenderType.lightning());
		poseStack.popPose();
	}

	private static Vec3 getLaserTip(LocalPlayer player, float partialTick, Vec3 look, InteractionHand hand) {
		Vec3 eye = player.getEyePosition(partialTick);
		Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (right.lengthSqr() < 1.0e-6D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		}
		right = right.normalize();

		double mainHandSide = player.getMainArm() == HumanoidArm.RIGHT ? 1.0D : -1.0D;
		double sideSign = hand == InteractionHand.OFF_HAND ? -mainHandSide : mainHandSide;
		boolean firstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();
		double side = (firstPerson ? 0.62D : 0.35D) * sideSign;
		double down = firstPerson ? -0.30D : -0.16D;
		double forward = firstPerson ? 0.82D : 0.38D;

		return eye.add(right.scale(side)).add(0.0D, down, 0.0D).add(look.scale(forward));
	}

	private static void drawLaser(VertexConsumer consumer, Matrix4f matrix, Vector3f start, Vector3f end, Vector3f side, Vector3f up, double renderTime) {
		Vector3f direction = new Vector3f(end).sub(start);
		float length = Math.max(direction.length(), 0.001F);
		Vector3f normalized = new Vector3f(direction).div(length);
		float sway = (float) Math.sin(renderTime * 0.38D) * Math.min(0.65F, length * 0.11F);
		float lift = (float) Math.cos(renderTime * 0.31D) * Math.min(0.36F, length * 0.07F);
		float sag = Math.min(0.45F, length * 0.08F);
		Vector3f controlOne = new Vector3f(start)
				.lerp(end, 0.30F)
				.add(new Vector3f(side).mul(sway))
				.add(new Vector3f(up).mul(lift + sag * 0.20F));
		Vector3f controlTwo = new Vector3f(start)
				.lerp(end, 0.70F)
				.add(new Vector3f(side).mul(-sway * 0.72F))
				.add(new Vector3f(up).mul(-lift * 0.55F - sag));

		for (int layer = 0; layer < 4; layer++) {
			int segments = 40;
			Vector3f previous = splinePoint(start, controlOne, controlTwo, end, side, up, renderTime, layer, 0.0F);
			for (int i = 1; i <= segments; i++) {
				float progress = i / (float) segments;
				Vector3f next = splinePoint(start, controlOne, controlTwo, end, side, up, renderTime, layer, progress);
				if (layer == 0) {
					drawBeamTube(consumer, matrix, previous, next, side, up, 0.155F, 186, 64, 255, 72);
				} else if (layer == 1) {
					drawBeamTube(consumer, matrix, previous, next, side, up, 0.088F, 186, 64, 255, 140);
				} else if (layer == 2) {
					drawBeamTube(consumer, matrix, previous, next, side, up, 0.050F, 186, 64, 255, 230);
				} else {
					drawBeamTube(consumer, matrix, previous, next, side, up, 0.020F, 255, 255, 255, 250);
				}
				previous = next;
			}
		}

		Vector3f sparkStart = new Vector3f(end).sub(new Vector3f(normalized).mul(Math.min(0.35F, length)));
		drawBeamTube(consumer, matrix, sparkStart, end, side, up, 0.22F, 186, 64, 255, 120);
	}

	private static Vector3f splinePoint(Vector3f start, Vector3f controlOne, Vector3f controlTwo, Vector3f end, Vector3f side, Vector3f up, double renderTime, int layer, float t) {
		float inv = 1.0F - t;
		Vector3f point = new Vector3f(start).mul(inv * inv * inv)
				.add(new Vector3f(controlOne).mul(3.0F * inv * inv * t))
				.add(new Vector3f(controlTwo).mul(3.0F * inv * t * t))
				.add(new Vector3f(end).mul(t * t * t));

		float envelope = (float) Math.sin(t * Math.PI);
		float ripple = (float) Math.sin(renderTime * 0.78D + t * 9.5D + layer * 1.8D);
		float twist = (float) Math.cos(renderTime * 0.62D + t * 8.0D + layer * 1.25D);
		float amount = envelope * (0.080F + layer * 0.024F);
		point.add(new Vector3f(side).mul(ripple * amount));
		point.add(new Vector3f(up).mul(twist * amount * 0.65F));
		return point;
	}

	private static void drawBeamTube(VertexConsumer consumer, Matrix4f matrix, Vector3f start, Vector3f end, Vector3f side, Vector3f up, float halfWidth, int r, int g, int b, int a) {
		Vector3f sideWidth = new Vector3f(side).mul(halfWidth);
		Vector3f upWidth = new Vector3f(up).mul(halfWidth);
		quad(consumer, matrix, new Vector3f(start).add(sideWidth), new Vector3f(start).sub(sideWidth), new Vector3f(end).sub(sideWidth), new Vector3f(end).add(sideWidth), r, g, b, a);
		quad(consumer, matrix, new Vector3f(start).add(upWidth), new Vector3f(start).sub(upWidth), new Vector3f(end).sub(upWidth), new Vector3f(end).add(upWidth), r, g, b, a);
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
}
