package net.crystalnexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class ConveyerBeltBER implements BlockEntityRenderer<ConveyerBeltBaseBlockEntity> {
    private static final double ITEM_Y = 0.56D;

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

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        LocalCurve curve = curveFor(be, facing);

        renderItems(be, partialTick, poseStack, buffer, packedLight, packedOverlay, curve);
    }

    private void renderItems(ConveyerBeltBaseBlockEntity be, float partialTick, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay, LocalCurve curve) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        float incomingProgress = be.getIncomingTransferProgress(partialTick);
        PreviousCurve previousCurve = incomingProgress >= 0.0F ? previousCurveFor(be) : null;

        for (int i = 0; i < ConveyerBeltBaseBlockEntity.SEGMENTS; i++) {
            ItemStack stack = be.getSegment(i);
            if (stack.isEmpty()) {
                continue;
            }

            float progress = be.getRenderProgress(partialTick);
            float segPos = i + progress;
            Vec3 point;
            Vec3 tangent;
            if (i == 0 && incomingProgress >= 0.0F && previousCurve != null) {
                point = incomingPosition(previousCurve, curve, incomingProgress);
                tangent = incomingTangent(previousCurve, curve, incomingProgress);
            } else {
                double t = Math.max(0.0D, Math.min(1.0D, segPos / (double) ConveyerBeltBaseBlockEntity.SEGMENTS));
                point = curve.position(t);
                tangent = curve.tangent(t);
            }
            float yaw = (float) Math.toDegrees(Math.atan2(tangent.x, tangent.z));

            poseStack.pushPose();
            poseStack.translate(point.x, ITEM_Y, point.z);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
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

    private Vec3 incomingPosition(PreviousCurve previousCurve, LocalCurve currentCurve, float progress) {
        Vec3 start = previousCurve.position(1.0D);
        Vec3 end = currentCurve.position(1.0D / ConveyerBeltBaseBlockEntity.SEGMENTS);
        Vec3 control = start.add(end).scale(0.5D);
        double t = Math.max(0.0D, Math.min(1.0D, progress));
        double inv = 1.0D - t;
        return start.scale(inv * inv).add(control.scale(2.0D * inv * t)).add(end.scale(t * t));
    }

    private Vec3 incomingTangent(PreviousCurve previousCurve, LocalCurve currentCurve, float progress) {
        Vec3 start = previousCurve.position(1.0D);
        Vec3 end = currentCurve.position(1.0D / ConveyerBeltBaseBlockEntity.SEGMENTS);
        Vec3 control = start.add(end).scale(0.5D);
        double t = Math.max(0.0D, Math.min(1.0D, progress));
        Vec3 tangent = control.subtract(start).scale(2.0D * (1.0D - t))
                .add(end.subtract(control).scale(2.0D * t));
        return tangent.lengthSqr() < 1.0E-6D ? currentCurve.tangent(0.0D) : tangent.normalize();
    }

    private LocalCurve curveFor(ConveyerBeltBaseBlockEntity be, Direction facing) {
        Vec3 center = new Vec3(0.5D, 0.0D, 0.5D);
        BlockPos pos = be.getBlockPos();

        Vec3 start = midpointOrEdge(be, pos, be.getSplinePrevPos(), center, facing.getOpposite());
        Vec3 end = midpointOrEdge(be, pos, be.getSplineNextPos(), center, facing);
        return new LocalCurve(start, center, end);
    }

    private PreviousCurve previousCurveFor(ConveyerBeltBaseBlockEntity be) {
        if (be.getLevel() == null) {
            return null;
        }

        BlockPos prevPos = be.getSplinePrevPos();
        if (prevPos == null) {
            prevPos = inferAdjacentBelt(be, be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
        }
        if (prevPos == null) {
            return null;
        }
        if (!(be.getLevel().getBlockEntity(prevPos) instanceof ConveyerBeltBaseBlockEntity previousBelt)) {
            return null;
        }

        BlockState previousState = previousBelt.getBlockState();
        if (!previousState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return null;
        }
        LocalCurve previousCurve = curveFor(previousBelt, previousState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        Vec3 offset = new Vec3(
                previousBelt.getBlockPos().getX() - be.getBlockPos().getX(),
                previousBelt.getBlockPos().getY() - be.getBlockPos().getY(),
                previousBelt.getBlockPos().getZ() - be.getBlockPos().getZ()
        );
        return new PreviousCurve(previousCurve, offset);
    }

    private Vec3 midpointOrEdge(ConveyerBeltBaseBlockEntity be, BlockPos currentPos, BlockPos connectionPos, Vec3 center, Direction fallbackEdge) {
        BlockPos resolvedConnection = connectionPos != null ? connectionPos : inferAdjacentBelt(be, fallbackEdge);
        if (resolvedConnection != null) {
            int dx = resolvedConnection.getX() - currentPos.getX();
            int dz = resolvedConnection.getZ() - currentPos.getZ();
            if (Math.abs(dx) + Math.abs(dz) == 1) {
                Vec3 neighborCenter = new Vec3(0.5D + dx, 0.0D, 0.5D + dz);
                return center.add(neighborCenter).scale(0.5D);
            }
        }

        return new Vec3(
                0.5D + fallbackEdge.getStepX() * 0.5D,
                0.0D,
                0.5D + fallbackEdge.getStepZ() * 0.5D
        );
    }

    private BlockPos inferAdjacentBelt(ConveyerBeltBaseBlockEntity be, Direction direction) {
        if (be.getLevel() == null) {
            return null;
        }

        BlockPos candidatePos = be.getBlockPos().relative(direction);
        if (!(be.getLevel().getBlockEntity(candidatePos) instanceof ConveyerBeltBaseBlockEntity neighbor)) {
            return null;
        }

        BlockState neighborState = neighbor.getBlockState();
        if (!neighborState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return null;
        }

        Direction neighborFacing = neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction ownFacing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (direction == ownFacing) {
            return neighborFacing == ownFacing || neighborFacing == ownFacing.getClockWise() || neighborFacing == ownFacing.getCounterClockWise() ? candidatePos : null;
        }
        return neighborFacing == ownFacing || neighborFacing == ownFacing.getClockWise() || neighborFacing == ownFacing.getCounterClockWise() ? candidatePos : null;
    }

    private record LocalCurve(Vec3 start, Vec3 control, Vec3 end) {
        private Vec3 position(double t) {
            double inv = 1.0D - t;
            return start.scale(inv * inv)
                    .add(control.scale(2.0D * inv * t))
                    .add(end.scale(t * t));
        }

        private Vec3 tangent(double t) {
            Vec3 tangent = control.subtract(start).scale(2.0D * (1.0D - t))
                    .add(end.subtract(control).scale(2.0D * t));
            if (tangent.lengthSqr() < 1.0E-6D) {
                return new Vec3(0.0D, 0.0D, 1.0D);
            }
            return tangent.normalize();
        }
    }

    private record PreviousCurve(LocalCurve curve, Vec3 offset) {
        private Vec3 position(double t) {
            return curve.position(t).add(offset);
        }
    }
}
