package net.crystalnexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;

public class ConveyerBeltBER implements BlockEntityRenderer<ConveyerBeltBaseBlockEntity> {

    public ConveyerBeltBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ConveyerBeltBaseBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        if (be.getLevel() == null) return;

        BlockState state = be.getBlockState();
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return;

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        // render each segment stack as a small item sitting on top of the belt
        for (int i = 0; i < ConveyerBeltBaseBlockEntity.SEGMENTS; i++) {
            ItemStack stack = be.getSegment(i);
            if (stack.isEmpty()) continue;

            // Smooth progression along belt (interpolated)
            float progress = be.getRenderProgress(partialTick); // 0..1, holds at 1
            float segPos = i + progress;

            // EDGE-BASED mapping:
            // t=0 near back edge, t=1 near front edge (reduces between-block jump)
            float t = segPos / (float) ConveyerBeltBaseBlockEntity.SEGMENTS;
            if (t < 0f) t = 0f;
            if (t > 1f) t = 1f;

            // Travel across most of the block (0.90 means ~0.05..0.95)
            double travel = 0.95;
            double along = (t - 0.5) * travel;

            // start at block center then move along facing direction
            double x = 0.5 + facing.getStepX() * along;
            double z = 0.5 + facing.getStepZ() * along;

            poseStack.pushPose();
            poseStack.translate(x, 0.55, z);

            // Lay flat
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));

            // Your special axis fix (kept)
            if (facing.getAxis() == Direction.Axis.X) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
            }

            // Align with belt direction
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facing.toYRot()));

            // scale down so it fits nicely
            poseStack.scale(0.45f, 0.45f, 0.45f);

            itemRenderer.renderStatic(
                    stack,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    buffer,
                    be.getLevel(),
                    (int) (be.getBlockPos().asLong() ^ (long)i)
            );

            poseStack.popPose();
        }
    }
}
