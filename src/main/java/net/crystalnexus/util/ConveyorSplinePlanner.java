package net.crystalnexus.util;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ConveyorSplinePlanner {
    private static final int MIN_SAMPLES = 24;
    private static final int MAX_SAMPLES = 160;

    private ConveyorSplinePlanner() {
    }

    public static ConnectionPlan plan(Level level, BlockPos sourcePos, BlockPos targetPos) {
        return plan(level, sourcePos, targetPos, PathMode.SPLINE);
    }

    public static ConnectionPlan plan(Level level, BlockPos sourcePos, BlockPos targetPos, PathMode mode) {
        if (sourcePos == null || targetPos == null || sourcePos.equals(targetPos)) {
            return null;
        }
        if (!level.isLoaded(sourcePos) || !level.isLoaded(targetPos)) {
            return null;
        }

        BlockState sourceState = level.getBlockState(sourcePos);
        BlockState targetState = level.getBlockState(targetPos);
        if (!isConveyorBlock(sourceState) || !isConveyorBlock(targetState)) {
            return null;
        }
        if (!sourceState.hasProperty(HorizontalDirectionalBlock.FACING) || !targetState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return null;
        }
        if (sourcePos.getY() != targetPos.getY()) {
            return null;
        }

        Direction sourceFacing = sourceState.getValue(HorizontalDirectionalBlock.FACING);
        Direction targetFacing = targetState.getValue(HorizontalDirectionalBlock.FACING);
        List<Vec3> splinePoints = mode == PathMode.ANGLED
                ? sampleAngledPath(sourcePos, sourceFacing, targetPos, targetFacing)
                : sampleSpline(sourcePos, sourceFacing, targetPos, targetFacing);
        List<BlockPos> cells = mode == PathMode.ANGLED
                ? rasterizeAngled(sourcePos, sourceFacing, targetPos)
                : rasterizeSpline(splinePoints, sourcePos, targetPos);
        if (cells.size() < 2) {
            return null;
        }
        if (!cells.get(0).equals(sourcePos) || !cells.get(cells.size() - 1).equals(targetPos)) {
            return null;
        }

        List<PlacedBelt> placedBelts = new ArrayList<>();
        for (int i = 1; i < cells.size() - 1; i++) {
            BlockPos pos = cells.get(i);
            BlockState existing = level.getBlockState(pos);
            if (!existing.canBeReplaced() && !isConveyorBlock(existing)) {
                return null;
            }

            Direction facing = directionBetween(pos, cells.get(i + 1));
            BlockState beltState = CrystalnexusModBlocks.CONVEYER_BELT.get()
                    .defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, facing);
            placedBelts.add(new PlacedBelt(pos, beltState));
        }

        return new ConnectionPlan(cells, placedBelts, splinePoints);
    }

    public static boolean isConveyorBlock(BlockState state) {
        Block block = state.getBlock();
        return block == CrystalnexusModBlocks.CONVEYER_BELT.get()
                || block == CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get()
                || block == CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get();
    }

    private static List<Vec3> sampleSpline(BlockPos sourcePos, Direction sourceFacing, BlockPos targetPos, Direction targetFacing) {
        Vec3 start = centerOf(sourcePos);
        Vec3 end = centerOf(targetPos);
        double distance = start.distanceTo(end);
        double handle = clamp(distance * 0.35D, 1.5D, 8.0D);

        Vec3 c1 = start.add(directionVector(sourceFacing).scale(handle));
        Vec3 c2 = end.subtract(directionVector(targetFacing).scale(handle));

        int samples = clamp((int) Math.ceil(distance * 10.0D), MIN_SAMPLES, MAX_SAMPLES);
        List<Vec3> points = new ArrayList<>(samples + 1);
        for (int i = 0; i <= samples; i++) {
            double t = (double) i / (double) samples;
            points.add(cubicBezier(start, c1, c2, end, t));
        }
        return points;
    }

    private static List<Vec3> sampleAngledPath(BlockPos sourcePos, Direction sourceFacing, BlockPos targetPos, Direction targetFacing) {
        List<BlockPos> cells = rasterizeAngled(sourcePos, sourceFacing, targetPos);
        List<Vec3> points = new ArrayList<>(cells.size());
        for (BlockPos cell : cells) {
            points.add(centerOf(cell));
        }
        return points;
    }

    private static List<BlockPos> rasterizeSpline(List<Vec3> splinePoints, BlockPos sourcePos, BlockPos targetPos) {
        List<BlockPos> cells = new ArrayList<>();
        cells.add(sourcePos);
        for (Vec3 point : splinePoints) {
            BlockPos sampleCell = BlockPos.containing(point.x, sourcePos.getY(), point.z);
            appendBridge(cells, sampleCell);
        }
        appendBridge(cells, targetPos);
        return cells;
    }

    private static List<BlockPos> rasterizeAngled(BlockPos sourcePos, Direction sourceFacing, BlockPos targetPos) {
        List<BlockPos> cells = new ArrayList<>();
        cells.add(sourcePos);

        BlockPos bend;
        if (sourceFacing.getAxis() == Direction.Axis.X) {
            bend = new BlockPos(targetPos.getX(), sourcePos.getY(), sourcePos.getZ());
        } else {
            bend = new BlockPos(sourcePos.getX(), sourcePos.getY(), targetPos.getZ());
        }

        appendBridge(cells, bend);
        appendBridge(cells, targetPos);
        return cells;
    }

    private static void appendBridge(List<BlockPos> cells, BlockPos next) {
        if (cells.isEmpty()) {
            cells.add(next);
            return;
        }

        BlockPos current = cells.get(cells.size() - 1);
        while (!current.equals(next)) {
            int dx = next.getX() - current.getX();
            int dz = next.getZ() - current.getZ();
            if (Math.abs(dx) >= Math.abs(dz) && dx != 0) {
                current = current.offset(Integer.signum(dx), 0, 0);
            } else if (dz != 0) {
                current = current.offset(0, 0, Integer.signum(dz));
            } else {
                break;
            }
            if (!cells.get(cells.size() - 1).equals(current)) {
                cells.add(current);
            }
        }
    }

    private static Vec3 centerOf(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private static Vec3 directionVector(Direction direction) {
        return new Vec3(direction.getStepX(), 0.0D, direction.getStepZ());
    }

    private static Vec3 cubicBezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double inv = 1.0D - t;
        double inv2 = inv * inv;
        double inv3 = inv2 * inv;
        double t2 = t * t;
        double t3 = t2 * t;
        return new Vec3(
                inv3 * p0.x + 3.0D * inv2 * t * p1.x + 3.0D * inv * t2 * p2.x + t3 * p3.x,
                inv3 * p0.y + 3.0D * inv2 * t * p1.y + 3.0D * inv * t2 * p2.y + t3 * p3.y,
                inv3 * p0.z + 3.0D * inv2 * t * p1.z + 3.0D * inv * t2 * p2.z + t3 * p3.z
        );
    }

    private static Direction directionBetween(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (Math.abs(dx) >= Math.abs(dz) && dx != 0) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record PlacedBelt(BlockPos pos, BlockState state) {
    }

    public record ConnectionPlan(List<BlockPos> cells, List<PlacedBelt> placedBelts, List<Vec3> splinePoints) {
    }

    public enum PathMode {
        SPLINE,
        ANGLED
    }
}
