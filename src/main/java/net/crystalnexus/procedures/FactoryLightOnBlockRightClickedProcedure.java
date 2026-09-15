package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.crystalnexus.init.CrystalnexusModBlocks;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class FactoryLightOnBlockRightClickedProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate, Player player) {

        BlockPos pos = BlockPos.containing(x, y, z);
        if (player.isShiftKeyDown()) {
            toggle(world, pos);
            return;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(pos);
        while (!queue.isEmpty() && visited.size() < 128) {
            BlockPos current = queue.remove();
            if (!visited.add(current) || !isFactoryLight(world, current)) {
                continue;
            }
            toggle(world, current);
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = current.relative(direction);
                if (!visited.contains(adjacent)) {
                    queue.add(adjacent);
                }
            }
        }
    }

    private static boolean isFactoryLight(LevelAccessor world, BlockPos pos) {
        return world.getBlockState(pos).is(CrystalnexusModBlocks.FACTORY_LIGHT.get())
                || world.getBlockState(pos).is(CrystalnexusModBlocks.FACTORY_LIGHT_FLOOR.get());
    }

    private static void toggle(LevelAccessor world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Property<?> prop = state.getBlock().getStateDefinition().getProperty("blockstate");
        if (prop instanceof IntegerProperty intProp) {
            int current = state.getValue(intProp);
            world.setBlock(pos, state.setValue(intProp, current == 1 ? 2 : 1), 2);
        }
    }
}
