package net.crystalnexus.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class CopperSingularityRightclickedOnBlockProcedure {
    public static void execute(LevelAccessor world, BlockPos pos, boolean reverse) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // 🌱 Crops
        if (block instanceof CropBlock cropBlock) {
            int age = cropBlock.getAge(state);
            int maxAge = cropBlock.getMaxAge();

            if (!reverse && age < maxAge) {
                world.setBlock(pos, cropBlock.getStateForAge(age + 1), 3);
            } else if (reverse && age > 0) {
                world.setBlock(pos, cropBlock.getStateForAge(age - 1), 3);
            }
            return;
        }

        // 🪨 Weathering copper
        if (block instanceof WeatheringCopper) {
            Block next = reverse
                    ? WeatheringCopper.getPrevious(block).orElse(null) // de-oxidize
                    : WeatheringCopper.getNext(block).orElse(null);    // oxidize

            if (next != null) {
                world.setBlock(pos, next.defaultBlockState(), 3);
            }
            return;
        }

        // 🔮 Generic integer properties (like bamboo age, kelp, etc.)
        for (var entry : state.getValues().entrySet()) {
            if (entry.getKey() instanceof IntegerProperty property) {
                int value = (Integer) entry.getValue();
                int min = property.getPossibleValues().stream().min(Integer::compareTo).orElse(value);
                int max = property.getPossibleValues().stream().max(Integer::compareTo).orElse(value);

                if (!reverse && value < max) {
                    world.setBlock(pos, state.setValue(property, value + 1), 3);
                    break;
                } else if (reverse && value > min) {
                    world.setBlock(pos, state.setValue(property, value - 1), 3);
                    break;
                }
            }
        }

        // ✨ Play different sounds for forward/backward
        if (world instanceof ServerLevel level) {
            level.playSound(
                null,
                pos,
                reverse ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
            );
        }
    }
}
