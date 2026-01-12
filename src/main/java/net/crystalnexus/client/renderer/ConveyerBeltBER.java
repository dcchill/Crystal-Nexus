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

            // 0..1 progression along belt
            float t = (i + 0.5f) / (float) ConveyerBeltBaseBlockEntity.SEGMENTS;

            // start at block center
            double x = 0.5;
            double z = 0.5;

            // spread along facing direction, slightly compressed so it stays within the block
            double along = (t - 0.5) * 0.85;
            x += facing.getStepX() * along;
            z += facing.getStepZ() * along;

            poseStack.pushPose();
            poseStack.translate(x, 0.40, z);

            // rotate the item so it faces the belt direction a bit (optional)
            // poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facing.toYRot()));

            // scale down so it fits nicely
            poseStack.scale(0.66f, 0.66f, 0.66f);

            itemRenderer.renderStatic(
                    stack,
                    ItemDisplayContext.GROUND,
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
