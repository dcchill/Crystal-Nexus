package net.crystalnexus.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.crystalnexus.block.EngineeredHeartBlock;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** One layout for server validation and the JEI construction guide, including required air. */
public final class EngineeredHeartStructure {
    public enum Part { AIR, FRAME, FLESH, CONTROLLER, HEART, INPUT, OUTPUT }
    public record Cell(BlockPos offset, Part part) {
        public BlockState state() {
            return switch (part) {
                case AIR -> Blocks.AIR.defaultBlockState();
                case FRAME -> CrystalnexusModBlocks.FLESH_MACHINE_FRAME.get().defaultBlockState();
                case FLESH -> CrystalnexusModBlocks.FLESH_BLOCK.get().defaultBlockState();
                case CONTROLLER -> CrystalnexusModBlocks.ENGINEERED_HEART.get().defaultBlockState();
                case HEART -> CrystalnexusModBlocks.HEART.get().defaultBlockState();
                case INPUT -> CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get().defaultBlockState();
                case OUTPUT -> CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get().defaultBlockState();
            };
        }
    }
    public static final List<Cell> CELLS = createCells();
    private EngineeredHeartStructure() { }

    private static List<Cell> createCells() {
        List<Cell> cells = new ArrayList<>(125);
        for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) for (int x = -2; x <= 2; x++) {
            int boundaries = (Math.abs(x) == 2 ? 1 : 0) + (Math.abs(y) == 2 ? 1 : 0) + (Math.abs(z) == 2 ? 1 : 0);
            Part part = boundaries == 3 ? Part.FLESH : boundaries == 2 ? Part.FRAME : Part.AIR;
            if (x == 0 && y == -2 && z == -2) part = Part.CONTROLLER;
            if (x == -2 && y == -2 && z == 0) part = Part.INPUT;
            if (x == 2 && y == -2 && z == 0) part = Part.OUTPUT;
            if (x == 0 && y == 0 && z == 0) part = Part.HEART;
            cells.add(new Cell(new BlockPos(x, y, z), part));
        }
        return List.copyOf(cells);
    }

    public static BlockPos rotate(BlockPos offset, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default -> offset;
        };
    }

    public static BlockPos center(BlockPos controller, Direction facing) {
        return controller.subtract(rotate(new BlockPos(0, -2, -2), facing));
    }

    public static boolean matches(Predicate<Cell> matchesCell) {
        return CELLS.stream().allMatch(matchesCell);
    }

    public static boolean matches(Level level, BlockPos controller, Direction facing) {
        BlockPos center = center(controller, facing);
        return matches(cell -> {
            BlockPos pos = center.offset(rotate(cell.offset(), facing));
            if (!level.hasChunkAt(pos)) return false;
            BlockState actual = level.getBlockState(pos);
            if (cell.part() == Part.AIR) return actual.isAir();
            if (!actual.is(cell.state().getBlock())) return false;
            return cell.part() != Part.CONTROLLER || actual.getValue(EngineeredHeartBlock.FACING) == facing;
        });
    }
}
