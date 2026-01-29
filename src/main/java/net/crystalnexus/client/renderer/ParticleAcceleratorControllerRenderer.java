package net.crystalnexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.crystalnexus.block.entity.ParticleAcceleratorControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;

public class ParticleAcceleratorControllerRenderer
		implements BlockEntityRenderer<ParticleAcceleratorControllerBlockEntity> {

	public ParticleAcceleratorControllerRenderer(BlockEntityRendererProvider.Context ctx) {}

	/**
	 * Make renderer visible farther away.
	 */
	@Override
	public int getViewDistance() {
		return 256; // bump up/down as you like
	}

	/**
	 * Make renderer not disappear when controller AABB isn't in view.
	 * This is the key fix for “beam disappears when not in sight”.
	 */
	@Override
	public AABB getRenderBoundingBox(ParticleAcceleratorControllerBlockEntity be) {
		int len = (int) be.getPersistentData().getDouble("linacLen");
		if (len <= 0) len = 16;

		// Big box around controller that covers the accelerator area.
		// If you want “always render anywhere”, you can return AABB.INFINITE (heavier).
		double r = Math.min(128, Math.max(8, len + 4));
		return new AABB(be.getBlockPos()).inflate(r, 8, r);
	}

	@Override
	public void render(
			ParticleAcceleratorControllerBlockEntity be,
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int packedLight,
			int packedOverlay
	) {
		Level level = be.getLevel();
		if (level == null) return;

		// Only show when formed + working + not stalled
		if (be.getPersistentData().getDouble("formed") != 1) return;
		if (be.getPersistentData().getDouble("progress") <= 0) return;
		if (be.getPersistentData().getDouble("stalled") == 1) return;

		int len = (int) be.getPersistentData().getDouble("linacLen");
		if (len <= 0) return;

		boolean ringMode = be.getPersistentData().getDouble("ringMode") == 1;

		Direction facing = be.getBlockState()
				.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);

		ArrayList<Vec3> points = ringMode
				? buildRingPoints(be, facing, len)
				: buildLinearPoints(facing, len);

		if (points.size() < 2) return;

		float pulse = 0.85f + 0.35f * Mth.sin((level.getGameTime() + partialTick) * 0.6f);

		// Speed
		float loopSeconds = 0.35f; // smaller = faster
		float ticksPerLoop = loopSeconds * 20f;
		float t = ((level.getGameTime() + partialTick) % ticksPerLoop) / ticksPerLoop;
		float head = t * points.size();

		// Trail
		int trailCount = 9;
		float trailStep = 0.5f;
		float baseScale = 0.20f;

		BlockState coreState = Blocks.SEA_LANTERN.defaultBlockState();
		BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
		int fullBright = 0xF000F0;

		for (int i = 0; i < trailCount; i++) {
			float idx = head - i * trailStep;
			while (idx < 0) idx += points.size();
			while (idx >= points.size()) idx -= points.size();

			int a = Mth.floor(idx);
			int b = (a + 1) % points.size();
			float frac = idx - a;

			Vec3 pa = points.get(a);
			Vec3 pb = points.get(b);

			double px = Mth.lerp(frac, pa.x, pb.x);
			double py = Mth.lerp(frac, pa.y, pb.y);
			double pz = Mth.lerp(frac, pa.z, pb.z);

			float headFactor = 1f - (i / (float) trailCount);
			float segScale = baseScale * headFactor * (0.75f + 0.55f * pulse * headFactor);
			if (segScale <= 0.02f) continue;

			poseStack.pushPose();
			poseStack.translate(0.5, 0.5, 0.5);
			poseStack.translate(px, py, pz);

			// CORE
			poseStack.pushPose();
			poseStack.scale(segScale, segScale, segScale);
			poseStack.translate(-0.5, -0.5, -0.5);
			brd.renderSingleBlock(coreState, poseStack, bufferSource, fullBright, packedOverlay);
			poseStack.popPose();

			// GLOW (transparent white cube)
			poseStack.pushPose();
			float glowScale = segScale * (1.6f + 0.4f * pulse * headFactor);
			poseStack.scale(glowScale, glowScale, glowScale);
			poseStack.translate(-0.5, -0.5, -0.5);
			renderGlowCube(poseStack, bufferSource, 1f, 1f, 1f, 0.35f * headFactor);
			poseStack.popPose();

			poseStack.popPose();
		}
	}

	// =====================================================
	// Linear path
	// =====================================================
	private static ArrayList<Vec3> buildLinearPoints(Direction facing, int len) {
		ArrayList<Vec3> pts = new ArrayList<>();
		for (int i = 1; i <= len; i++) {
			pts.add(new Vec3(
					facing.getStepX() * i,
					facing.getStepY() * i,
					facing.getStepZ() * i
			));
		}
		return pts;
	}

	// =====================================================
	// Ring path (corner-safe) + controller sharp-corner fix
	// =====================================================
	private static ArrayList<Vec3> buildRingPoints(
			ParticleAcceleratorControllerBlockEntity be,
			Direction facing,
			int maxLen
	) {
		Level level = be.getLevel();
		if (level == null) return new ArrayList<>();

		BlockPos origin = be.getBlockPos();
		int baseY = origin.getY();

		Direction[] candidates = {
				facing,
				facing.getClockWise(),
				facing.getCounterClockWise(),
				facing.getOpposite()
		};

		BlockPos start = null;
		Direction dir = null;

		for (Direction d : candidates) {
			BlockPos p = origin.relative(d);
			if (isTubeOrMagnet(level, p)) {
				start = p;
				dir = d;
				break;
			}
		}
		if (start == null || dir == null) return new ArrayList<>();

		ArrayList<Vec3> pts = new ArrayList<>();
		HashSet<Long> visited = new HashSet<>();
		BlockPos pos = start;

		for (int i = 0; i < maxLen; i++) {
			if (pos.getY() != baseY) break;
			if (!isTubeOrMagnet(level, pos)) break;

			long key = pos.asLong();
			if (visited.contains(key)) break;
			visited.add(key);

			pts.add(new Vec3(
					pos.getX() - origin.getX(),
					pos.getY() - origin.getY(),
					pos.getZ() - origin.getZ()
			));

			Direction next = null;
			int options = 0;

			for (Direction d : Direction.Plane.HORIZONTAL) {
				if (d == dir.getOpposite()) continue;

				BlockPos np = pos.relative(d);

				// close back into controller
				if (np.equals(origin) && i >= 3) {
					next = d;
					options = 1;
					break;
				}

				if (isTubeOrMagnet(level, np) && !visited.contains(np.asLong())) {
					next = d;
					options++;
				}
			}

			if (options != 1 || next == null) break;

			// if closing into controller, stop after adding last segment
			if (pos.relative(next).equals(origin)) break;

			dir = next;
			pos = pos.relative(next);
		}

		if (pts.size() < 4) return new ArrayList<>();

		// IMPORTANT: force a sharp corner at controller instead of diagonal “curve”
		pts.add(new Vec3(0, 0, 0));   // controller center
		pts.add(pts.get(0));          // reconnect cleanly

		return pts;
	}

	private static boolean isTubeOrMagnet(Level level, BlockPos pos) {
		BlockState st = level.getBlockState(pos);
		return st.getBlock() == CrystalnexusModBlocks.PARTICLE_ACCELERATOR_TUBE.get()
				|| st.getBlock() == CrystalnexusModBlocks.ELECTROMAGNET.get();
	}

	// =====================================================
	// Transparent white glow cube (no texture)
	// =====================================================
	private static void renderGlowCube(
			PoseStack poseStack,
			MultiBufferSource buffer,
			float r, float g, float b, float a
	) {
		VertexConsumer vc = buffer.getBuffer(RenderType.debugFilledBox());
		PoseStack.Pose pose = poseStack.last();
		int light = 0xF000F0;

		float min = 0f;
		float max = 1f;

		addFace(vc, pose, min, min, min, max, min, min, max, max, min, min, max, min, r, g, b, a, 0, 0, -1, light); // north
		addFace(vc, pose, min, min, max, min, max, max, max, max, max, max, min, max, r, g, b, a, 0, 0, 1, light);  // south
		addFace(vc, pose, min, min, min, min, max, min, min, max, max, min, min, max, r, g, b, a, -1, 0, 0, light); // west
		addFace(vc, pose, max, min, min, max, min, max, max, max, max, max, max, min, r, g, b, a, 1, 0, 0, light);  // east
		addFace(vc, pose, min, max, min, max, max, min, max, max, max, min, max, max, r, g, b, a, 0, 1, 0, light);  // up
		addFace(vc, pose, min, min, min, min, min, max, max, min, max, max, min, min, r, g, b, a, 0, -1, 0, light); // down
	}

	private static void addFace(
			VertexConsumer vc,
			PoseStack.Pose pose,
			float x1, float y1, float z1,
			float x2, float y2, float z2,
			float x3, float y3, float z3,
			float x4, float y4, float z4,
			float r, float g, float b, float a,
			int nx, int ny, int nz,
			int light
	) {
		vc.addVertex(pose.pose(), x1, y1, z1)
				.setColor(r, g, b, a)
				.setUv(0, 0)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);

		vc.addVertex(pose.pose(), x2, y2, z2)
				.setColor(r, g, b, a)
				.setUv(1, 0)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);

		vc.addVertex(pose.pose(), x3, y3, z3)
				.setColor(r, g, b, a)
				.setUv(1, 1)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);

		vc.addVertex(pose.pose(), x4, y4, z4)
				.setColor(r, g, b, a)
				.setUv(0, 1)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
	}
}
