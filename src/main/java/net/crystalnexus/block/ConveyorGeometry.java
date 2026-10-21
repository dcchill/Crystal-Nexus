package net.crystalnexus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.*;

/** Shared surface and port geometry; ramp blocks are anchored at their lower end. */
public final class ConveyorGeometry {
    public enum Slope implements StringRepresentable {
        FLAT, UP, DOWN;
        public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    public static final EnumProperty<Slope> SLOPE = EnumProperty.create("slope", Slope.class);
    private ConveyorGeometry() {}
    public static BlockState placementState(BlockState base, net.minecraft.world.item.context.BlockPlaceContext context) {
        if (context instanceof net.crystalnexus.util.ConveyorPlacement.Context route) return route.state(base);
        return automaticState(context.getLevel(), context.getClickedPos(),
            base.setValue(ConveyerBeltBlock.FACING, context.getHorizontalDirection()));
    }
    private static BlockState automaticState(Level level, BlockPos pos, BlockState state) {
        BlockState flat = state.setValue(SLOPE, Slope.FLAT).setValue(ConveyerBeltBlock.CURVE, ConveyerBeltBlock.CurveShape.STRAIGHT);
        if (!level.isOutsideBuildHeight(pos.above()) && level.getBlockState(pos.above()).canBeReplaced()) {
            for (Slope slope : List.of(Slope.UP, Slope.DOWN)) {
                BlockState ramp = flat.setValue(SLOPE, slope);
                boolean out = slope == Slope.UP;
                BlockPos high = port(pos, ramp, out);
                if (!level.hasChunkAt(high)) continue;
                BlockState other = level.getBlockState(high);
                if (out ? connects(pos, ramp, high, other) : connects(high, other, pos, ramp)) return ramp;
            }
        }
        for (ConveyerBeltBlock.CurveShape curve : ConveyerBeltBlock.CurveShape.values()) {
            BlockState candidate = flat.setValue(ConveyerBeltBlock.CURVE, curve);
            BlockPos prev = neighbor(level, pos, candidate, false);
            if (level.hasChunkAt(prev) && connects(prev, level.getBlockState(prev), pos, candidate)) return candidate;
        }
        return flat;
    }
    public static void updateAutomatic(Level level, BlockPos pos) {
        if (level.isClientSide || net.crystalnexus.util.ConveyorPlacement.placing) return;
        BlockState state = level.getBlockState(pos);
        if (!isBelt(state)) return;
        // Wrench routes retain their explicitly planned corners and ramps.
        if (level.getBlockEntity(pos) instanceof net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity belt
                && (belt.getSplinePrevPos() != null || belt.getSplineNextPos() != null)) return;
        BlockState next = automaticState(level, pos, state);
        if (next != state) level.setBlock(pos, next, Block.UPDATE_ALL);
    }
    public static void scheduleUpdates(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        level.scheduleTick(pos, level.getBlockState(pos).getBlock(), 1);
        // Raised neighbors are diagonal, outside vanilla's six direct neighbor notifications.
        for (Direction side : Direction.Plane.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
            BlockPos neighbor = pos.relative(side).offset(0, dy, 0);
            if (!level.hasChunkAt(neighbor)) continue;
            BlockState state = level.getBlockState(neighbor);
            if (isBelt(state)) level.scheduleTick(neighbor, state.getBlock(), 1);
        }
    }
    public static boolean isBelt(BlockState s) {
        return s.getBlock() instanceof ConveyerBeltBlock || s.getBlock() instanceof ConveyerBeltInputBlock
            || s.getBlock() instanceof ConveyerBeltOutputBlock;
    }
    public static Direction output(BlockState s) { return s.getValue(ConveyerBeltBlock.FACING); }
    public static Direction input(BlockState s) {
        Direction f = output(s);
        return switch (s.getValue(ConveyerBeltBlock.CURVE)) {
            case LEFT -> f.getCounterClockWise();
            case RIGHT -> f.getClockWise();
            default -> f.getOpposite();
        };
    }
    public static int inputHeight(BlockState s) { return s.getValue(SLOPE) == Slope.DOWN ? 1 : 0; }
    public static int outputHeight(BlockState s) { return s.getValue(SLOPE) == Slope.UP ? 1 : 0; }
    public static BlockPos port(BlockPos p, BlockState s, boolean out) {
        return p.relative(out ? output(s) : input(s)).above(out ? outputHeight(s) : inputHeight(s));
    }
    public static boolean connects(BlockPos a, BlockState as, BlockPos b, BlockState bs) {
        return isBelt(as) && isBelt(bs) && output(as) == input(bs).getOpposite()
            && a.relative(output(as)).getX() == b.getX() && a.relative(output(as)).getZ() == b.getZ()
            && a.getY() + outputHeight(as) == b.getY() + inputHeight(bs);
    }
    public static BlockPos neighbor(Level level, BlockPos p, BlockState s, boolean out) {
        BlockPos edge = port(p, s, out);
        for (BlockPos q : List.of(edge, edge.below())) {
            if (!level.hasChunkAt(q)) continue;
            BlockState other = level.getBlockState(q);
            if (out ? connects(p,s,q,other) : connects(q,other,p,s)) return q;
        }
        return edge;
    }
    public static Vec3 point(BlockState s, double t) {
        Direction in = input(s), out = output(s);
        double y = .5 + inputHeight(s) * (1-t) + outputHeight(s) * t;
        if (s.getValue(ConveyerBeltBlock.CURVE) == ConveyerBeltBlock.CurveShape.STRAIGHT)
            return new Vec3(.5 + out.getStepX()*(t-.5), y, .5 + out.getStepZ()*(t-.5));
        // A quarter circle parameterized by arc length, tangent to both block edges.
        double angle = t * Math.PI/2;
        return new Vec3(.5 + .5*(in.getStepX()+out.getStepX())
            - .5*out.getStepX()*Math.cos(angle)-.5*in.getStepX()*Math.sin(angle), y,
            .5 + .5*(in.getStepZ()+out.getStepZ())
            - .5*out.getStepZ()*Math.cos(angle)-.5*in.getStepZ()*Math.sin(angle));
    }
    public static double progress(BlockState state, double x, double z) {
        Direction out = output(state), in = input(state);
        double t;
        if (state.getValue(ConveyerBeltBlock.CURVE) == ConveyerBeltBlock.CurveShape.STRAIGHT) {
            t = .5 + (x - .5) * out.getStepX() + (z - .5) * out.getStepZ();
        } else {
            double dx = x - .5 - .5 * (in.getStepX() + out.getStepX());
            double dz = z - .5 - .5 * (in.getStepZ() + out.getStepZ());
            t = Math.atan2(-(dx * in.getStepX() + dz * in.getStepZ()),
                -(dx * out.getStepX() + dz * out.getStepZ())) / (Math.PI / 2);
        }
        return Math.clamp(t, 0, 1);
    }
    public static double supportHeight(BlockState state, net.minecraft.world.phys.AABB bounds) {
        return shape(state).toAabbs().stream().filter(box ->
            bounds.maxX > box.minX && bounds.minX < box.maxX && bounds.maxZ > box.minZ && bounds.minZ < box.maxZ)
            .mapToDouble(box -> box.maxY).max().orElse(Double.NaN);
    }
    private static final Map<BlockState,VoxelShape> SHAPES = new java.util.concurrent.ConcurrentHashMap<>();
    static int curveEdgePixels(int row, boolean mirror) {
        int edge=(int)Math.ceil(Math.sqrt(256-Math.pow(row,2)));
        return mirror ? 16-edge : edge;
    }
    public static VoxelShape shape(BlockState s) {
        return SHAPES.computeIfAbsent(s, key -> {
            VoxelShape shape = Shapes.empty();
            if (key.getValue(ConveyerBeltBlock.CURVE) != ConveyerBeltBlock.CurveShape.STRAIGHT) {
                Direction in=input(key), out=output(key);
                int cornerX=(1+in.getStepX()+out.getStepX())/2;
                int cornerZ=(1+in.getStepZ()+out.getStepZ())/2;
                for(int i=0;i<16;i++) {
                    double z0=i/16.0, z1=(i+1)/16.0;
                    double x0=cornerX==0 ? 0 : curveEdgePixels(i,true)/16.0;
                    double x1=cornerX==0 ? curveEdgePixels(i,false)/16.0 : 1;
                    shape=Shapes.or(shape,Block.box(x0*16,5,(cornerZ==0?z0:1-z1)*16,
                        x1*16,8,(cornerZ==0?z1:1)*16));
                }
                return shape.optimize();
            }
            for(int i=0;i<16;i++) {
                Vec3 p = point(key,(i+.5)/16.0);
                double hx = output(key).getAxis()==Direction.Axis.X ? 1.0/32 : .5;
                double hz = output(key).getAxis()==Direction.Axis.Z ? 1.0/32 : .5;
                shape=Shapes.or(shape,Block.box(Math.max(0,p.x-hx)*16,(p.y-.1875)*16,Math.max(0,p.z-hz)*16,
                    Math.min(1,p.x+hx)*16,p.y*16,Math.min(1,p.z+hz)*16));
            }
            return shape.optimize();
        });
    }
    public record Segment(BlockPos pos, Direction facing, ConveyerBeltBlock.CurveShape curve, Slope slope) {
        public BlockState state(BlockState base) {
            return base.setValue(ConveyerBeltBlock.FACING,facing).setValue(ConveyerBeltBlock.CURVE,curve).setValue(SLOPE,slope);
        }
    }
    public static List<Segment> plan(BlockPos start, BlockPos end, boolean xFirst) {
        long distance = Math.abs((long)end.getX()-start.getX()) + Math.abs((long)end.getZ()-start.getZ());
        if(distance==0 || distance>257) throw new IllegalArgumentException("Route needs 1-256 new belts and horizontal space");
        List<BlockPos> flat=new ArrayList<>(); flat.add(start);
        BlockPos cursor=start;
        for(Direction.Axis axis : xFirst ? List.of(Direction.Axis.X,Direction.Axis.Z) : List.of(Direction.Axis.Z,Direction.Axis.X)) {
            int target=axis==Direction.Axis.X?end.getX():end.getZ();
            while((axis==Direction.Axis.X?cursor.getX():cursor.getZ())!=target) {
                int step=Integer.compare(target,axis==Direction.Axis.X?cursor.getX():cursor.getZ());
                cursor=cursor.offset(axis==Direction.Axis.X?step:0,0,axis==Direction.Axis.Z?step:0); flat.add(cursor);
            }
        }
        List<Segment> result=new ArrayList<>();
        int y=start.getY(), remaining=Math.abs(end.getY()-y), sign=Integer.compare(end.getY(),y);
        for(int i=0;i<flat.size();i++) {
            Direction incoming=direction(flat.get(Math.max(0,i-1)),flat.get(i==0?1:i));
            Direction outgoing=direction(flat.get(i==flat.size()-1?i-1:i),flat.get(Math.min(i+1,flat.size()-1)));
            ConveyerBeltBlock.CurveShape curve=incoming==outgoing?ConveyerBeltBlock.CurveShape.STRAIGHT:
                incoming.getOpposite()==outgoing.getCounterClockWise()?ConveyerBeltBlock.CurveShape.LEFT:ConveyerBeltBlock.CurveShape.RIGHT;
            Slope slope=Slope.FLAT;
            if(i>0 && i<flat.size()-1 && curve==ConveyerBeltBlock.CurveShape.STRAIGHT && remaining>0) {
                slope=sign>0?Slope.UP:Slope.DOWN; remaining--; if(sign<0)y--;
            }
            result.add(new Segment(new BlockPos(flat.get(i).getX(),y,flat.get(i).getZ()),outgoing,curve,slope));
            if(slope==Slope.UP)y++;
        }
        if(remaining>0) throw new IllegalArgumentException("Not enough straight space for ramps; corners and endpoints must stay flat");
        return List.copyOf(result);
    }
    private static Direction direction(BlockPos a,BlockPos b) { return Direction.getNearest(b.getX()-a.getX(),0,b.getZ()-a.getZ()); }
}
