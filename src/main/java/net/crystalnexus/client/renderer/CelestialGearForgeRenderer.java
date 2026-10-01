package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.crystalnexus.block.entity.CelestialGearForgeBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;

public final class CelestialGearForgeRenderer implements BlockEntityRenderer<CelestialGearForgeBlockEntity> {
	private final ItemRenderer itemRenderer;

	public CelestialGearForgeRenderer(BlockEntityRendererProvider.Context context) {
		itemRenderer = context.getItemRenderer();
	}

	@Override public AABB getRenderBoundingBox(CelestialGearForgeBlockEntity forge) {
		return new AABB(forge.getBlockPos()).expandTowards(0.0D, 3.0D, 0.0D).inflate(0.25D);
	}

	@Override
	public void render(CelestialGearForgeBlockEntity forge, float partialTick, PoseStack poseStack,
			MultiBufferSource buffers, int packedLight, int packedOverlay) {
		if (!forge.isFormed() || !forge.isProcessing() || forge.getLevel() == null) return;
		float progress = Mth.clamp((forge.getProgress() + partialTick) / (float) CelestialGearForgeBlockEntity.DURATION, 0.0F, 1.0F);
		float rise = Mth.clamp(progress / 0.2F, 0.0F, 1.0F);
		float starY = 0.78F + rise * 1.45F;
		float starScale = 0.24F + rise * 0.56F;
		ItemStack star = forge.getItem(CelestialGearForgeBlockEntity.STAR_SLOT);
		
		poseStack.pushPose();
		poseStack.translate(0.5D, starY, 0.5D);
		poseStack.scale(starScale, starScale, starScale);
		SolarSimulatorRenderer.renderSunGlow(star, poseStack, buffers, forge.getLevel().getGameTime() + partialTick);
		poseStack.mulPose(Axis.YP.rotationDegrees((forge.getLevel().getGameTime() + partialTick) * 1.3F));
		itemRenderer.renderStatic(star, ItemDisplayContext.FIXED, 0x00F000F0, OverlayTexture.NO_OVERLAY,
			poseStack, buffers, forge.getLevel(), forge.getBlockPos().hashCode());
		poseStack.popPose();

		float spiralProgress = Mth.clamp((progress - 0.2F) / 0.8F, 0.0F, 1.0F);
		for (int index = 0; index < CelestialGearForgeBlockEntity.STAR_SLOT; index++) {
			float start = index * 0.06F;
			if (spiralProgress < start) continue;
			float t = Mth.clamp((spiralProgress - start) / 0.58F, 0.0F, 1.0F);
			if (t >= 1.0F) continue;
			float eased = t * t * (3.0F - 2.0F * t);
			float angle = t * Mth.TWO_PI * 1.5F + index * Mth.PI / 4.0F;
			float radius = (1.0F - eased) * 0.55F;
			float x = Mth.lerp(eased, 0.5F + Mth.sin(index * Mth.PI / 4.0F) * 0.38F, 0.5F) + Mth.cos(angle) * radius;
			float z = Mth.lerp(eased, 0.5F - Mth.cos(index * Mth.PI / 4.0F) * 0.38F, 0.5F) + Mth.sin(angle) * radius;
			float y = Mth.lerp(eased, 0.84F, starY) + Mth.sin(angle * 2.0F) * 0.12F;
			poseStack.pushPose();
			poseStack.translate(x, y, z);
			poseStack.mulPose(Axis.YP.rotationDegrees(angle * Mth.RAD_TO_DEG));
			poseStack.scale(0.28F, 0.28F, 0.28F);
			itemRenderer.renderStatic(forge.getItem(index), ItemDisplayContext.FIXED, 0x00F000F0,
				OverlayTexture.NO_OVERLAY, poseStack, buffers, forge.getLevel(), forge.getBlockPos().hashCode() + index);
			poseStack.popPose();
		}
	}
}
