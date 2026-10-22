package net.crystalnexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.crystalnexus.block.ConveyorGeometry;
import net.crystalnexus.block.ConveyerBeltMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class ConveyerBeltBER implements BlockEntityRenderer<ConveyerBeltBaseBlockEntity> {

    public ConveyerBeltBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ConveyerBeltBaseBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (be.getLevel() == null) {
            return;
        }

        BlockState state = be.getBlockState();
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return;
        }

        renderItems(be, partialTick, poseStack, buffer, packedLight, packedOverlay, state);
    }

    private void renderItems(ConveyerBeltBaseBlockEntity be, float partialTick, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay, BlockState curve) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        for (int i = 0; i < ConveyerBeltBaseBlockEntity.SEGMENTS; i++) {
            ItemStack stack = be.getSegment(i);
            if (stack.isEmpty()) {
                continue;
            }

            boolean advancing = be.canAdvanceForRender(i);
            float progress = advancing ? be.getRenderProgress(i, partialTick) : 0.0F;
            // Interpolate the whole tick, including tiers that cross several segments per tick.
            float segPos = advancing
                    ? ConveyerBeltMovement.renderPosition(i, progress, be.getRenderDistance()) : i;
            double t = Math.max(0.0D, Math.min(1.0D, segPos / (double) ConveyerBeltBaseBlockEntity.SEGMENTS));
            Vec3 point = ConveyorGeometry.point(curve, t);
            Vec3 tangent = ConveyorGeometry.point(curve, Math.min(1,t+.001)).subtract(ConveyorGeometry.point(curve, Math.max(0,t-.001)));
            float yaw = (float) Math.toDegrees(Math.atan2(tangent.x, tangent.z));

            poseStack.pushPose();
            poseStack.translate(point.x, point.y + .06D, point.z);
            float pitch = (float) Math.toDegrees(Math.atan2(tangent.y, Math.hypot(tangent.x, tangent.z)));
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(90 - pitch));
            poseStack.scale(0.45f, 0.45f, 0.45f);

            itemRenderer.renderStatic(
                    stack,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    buffer,
                    be.getLevel(),
                    (int) (be.getBlockPos().asLong() ^ (long) i)
            );

            poseStack.popPose();
        }
    }


}
