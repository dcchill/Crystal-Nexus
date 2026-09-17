package net.crystalnexus.assembly;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Bounded, event-driven discovery of a sealed rectangular shell. */
public final class AssemblyLineStructure {
    public static final int MAX_SIZE = 32;
    public record Bounds(BlockPos min, BlockPos max) {
        public boolean contains(BlockPos p) {
            return p.getX() >= min.getX() && p.getX() <= max.getX()
                && p.getY() >= min.getY() && p.getY() <= max.getY()
                && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
        }
        public int faces(BlockPos p) {
            return (p.getX() == min.getX() || p.getX() == max.getX() ? 1 : 0)
                + (p.getY() == min.getY() || p.getY() == max.getY() ? 1 : 0)
                + (p.getZ() == min.getZ() || p.getZ() == max.getZ() ? 1 : 0);
        }
    }
    public record Result(Bounds bounds, List<BlockPos> machines, String error) {
        public boolean valid() { return error.isEmpty(); }
    }
    private static boolean shell(BlockState state) {
        return state.is(CrystalnexusModBlocks.ASSEMBLY_LINE_CASING.get())
            || state.is(CrystalnexusModBlocks.ASSEMBLY_LINE_CONTROLLER.get())
            || state.is(CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get())
            || state.is(CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get())
            || state.is(CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get())
            || state.is(CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get())
            || state.is(CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
    }
    public static Result scan(Level level, BlockPos controller) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(controller); visited.add(controller);
        BlockPos min = controller, max = controller;
        while (!queue.isEmpty()) {
            BlockPos p = queue.removeFirst();
            min = new BlockPos(Math.min(min.getX(), p.getX()), Math.min(min.getY(), p.getY()), Math.min(min.getZ(), p.getZ()));
            max = new BlockPos(Math.max(max.getX(), p.getX()), Math.max(max.getY(), p.getY()), Math.max(max.getZ(), p.getZ()));
            if (max.getX()-min.getX() >= MAX_SIZE || max.getY()-min.getY() >= MAX_SIZE || max.getZ()-min.getZ() >= MAX_SIZE)
                return new Result(null, List.of(), "Structure exceeds 32 blocks per axis");
            for (Direction d : Direction.values()) {
                BlockPos next = p.relative(d);
                if (!level.hasChunkAt(next)) continue;
                if (shell(level.getBlockState(next)) && visited.add(next)) queue.add(next);
            }
        }
        Bounds bounds = new Bounds(min, max);
        if (max.getX()-min.getX() < 2 || max.getY()-min.getY() < 2 || max.getZ()-min.getZ() < 2 || bounds.faces(controller) != 1)
            return new Result(bounds, List.of(), "Controller must be on a face of an enclosure at least 3x3x3");
        List<BlockPos> machines = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (!level.hasChunkAt(p)) return new Result(bounds, List.of(), "Structure chunk unloaded");
            BlockState state = level.getBlockState(p);
            if (bounds.faces(p) > 0) {
                if (!p.equals(controller) && !shell(state))
                    return new Result(bounds, List.of(), "Invalid shell at " + p.toShortString());
            } else if (!state.isAir()) {
                if (AssemblyLineMachine.at(level, p) == null)
                    return new Result(bounds, List.of(), "Unsupported interior block at " + p.toShortString());
                machines.add(p.immutable());
            }
        }
        return new Result(bounds, List.copyOf(machines), "");
    }
}
