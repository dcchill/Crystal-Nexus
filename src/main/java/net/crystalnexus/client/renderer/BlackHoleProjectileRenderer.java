package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.crystalnexus.entity.BlackHoleProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class BlackHoleProjectileRenderer extends EntityRenderer<BlackHoleProjectileEntity> {
    public BlackHoleProjectileRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public void render(BlackHoleProjectileEntity shot, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 relative = shot.getPosition(partialTick).subtract(entityRenderDispatcher.camera.getPosition());
        pose.pushPose();
        pose.translate(-relative.x, -relative.y, -relative.z);
        BlackHoleWorldRenderer.drawHole(buffers.getBuffer(RenderType.lightning()), pose.last().pose(), relative,
                0.4f, 1, shot.level().getGameTime() + partialTick);
        pose.popPose();
        super.render(shot, yaw, partialTick, pose, buffers, light);
    }

    @Override public ResourceLocation getTextureLocation(BlackHoleProjectileEntity shot) {
        return ResourceLocation.fromNamespaceAndPath("crystalnexus", "textures/item/rpg_texture.png");
    }
}
