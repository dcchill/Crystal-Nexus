package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.BlockPos;

public class FactoryLightOnBlockRightClickedProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {

        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = world.getBlockState(pos);

        Property<?> prop = state.getBlock().getStateDefinition().getProperty("blockstate");

        if (prop instanceof IntegerProperty intProp) {
            int current = state.getValue(intProp);
            int next = current == 1 ? 2 : 1;

            world.setBlock(pos, state.setValue(intProp, next), 2);
        }
    }
}
