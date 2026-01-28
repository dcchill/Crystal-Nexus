package net.crystalnexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.crystalnexus.block.entity.ParticleAcceleratorControllerBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ParticleAcceleratorControllerRenderer implements BlockEntityRenderer<ParticleAcceleratorControllerBlockEntity> {

	public ParticleAcceleratorControllerRenderer(BlockEntityRendererProvider.Context ctx) {}

@Override
public void render(ParticleAcceleratorControllerBlockEntity be, float partialTick, PoseStack poseStack,
		MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

	Level level = be.getLevel();
	if (level == null) return;

	// Only show beam when formed
	if (be.getPersistentData().getDouble("formed") != 1) return;

	// Only render when machine is actually working
	double progress = be.getPersistentData().getDouble("progress");
	if (progress <= 0) return;

	// (Optional) also hide while stalled
	if (be.getPersistentData().getDouble("stalled") == 1) return;

	int len = (int) be.getPersistentData().getDouble("linacLen");
	if (len <= 0) return;

	Direction forward = be.getBlockState()
			.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);

	// ===== Speed =====
	// Smaller = faster loop
	float loopSeconds = 0.75f; // was 2.0f
	float ticksPerLoop = loopSeconds * 20.0f;

	float t = ((level.getGameTime() + partialTick) % ticksPerLoop) / ticksPerLoop;

	float start = 0.5f;
	float end = len + 0.5f;
	float headDist = Mth.lerp(t, start, end);

	// The "cube" to draw
	BlockState beamState = Blocks.SEA_LANTERN.defaultBlockState();
	BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();

	// ===== Trail tuning =====
	int trailCount = 7;        // number of segments
	float trailSpacing = 0.35f; // blocks between segments (bigger = longer trail)
	float baseScale = 0.22f;   // head size

	for (int i = 0; i < trailCount; i++) {
		// i=0 is head, increasing i goes backward
		float dist = headDist - i * trailSpacing;

		// wrap around so the trail loops cleanly
		float range = (end - start);
		while (dist < start) dist += range;

		// fade by shrinking (no color changes needed)
		float segScale = baseScale * (1.0f - (i / (float) trailCount));
		if (segScale <= 0.02f) continue;

		poseStack.pushPose();

		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.translate(forward.getStepX() * dist, forward.getStepY() * dist, forward.getStepZ() * dist);

		poseStack.scale(segScale, segScale, segScale);
		poseStack.translate(-0.5, -0.5, -0.5);

		brd.renderSingleBlock(beamState, poseStack, bufferSource, packedLight, packedOverlay);

		poseStack.popPose();
	}
}

}
