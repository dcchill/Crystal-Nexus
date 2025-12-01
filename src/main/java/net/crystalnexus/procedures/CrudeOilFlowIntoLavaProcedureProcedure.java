package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class CrudeOilFlowIntoLavaProcedureProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);

            if (world.getBlockState(neighborPos).getBlock() == Blocks.LAVA) {
                // Create Tarrock where the lava was
                world.setBlock(neighborPos, CrystalnexusModBlocks.TARROCK.get().defaultBlockState(), 3);

                // Add particle effects
                if (world instanceof ServerLevel _level) {
                    _level.sendParticles(ParticleTypes.SMOKE, x + 0.5, y + 0.5, z + 0.5,
                            15, 0.3, 0.3, 0.3, 0.02);
                    _level.sendParticles(ParticleTypes.LARGE_SMOKE, x + 0.5, y + 0.5, z + 0.5,
                            5, 0.3, 0.3, 0.3, 0.05);
                    _level.sendParticles(ParticleTypes.FLAME, x + 0.5, y + 0.2, z + 0.5,
                            6, 0.2, 0.1, 0.2, 0.02);
                }

                // Play bubbling/hissing sounds
                if (world instanceof Level _level) {
                    if (!_level.isClientSide()) {
                        _level.playSound(null, pos,
                                BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lava.extinguish")),
                                SoundSource.BLOCKS, 0.8f, 1.0f);
                        _level.playSound(null, pos,
                                BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bubble_column.upwards_inside")),
                                SoundSource.BLOCKS, 0.5f, 1.3f);
                    } else {
                        _level.playLocalSound(x, y, z,
                                BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lava.extinguish")),
                                SoundSource.BLOCKS, 0.8f, 1.0f, false);
                        _level.playLocalSound(x, y, z,
                                BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bubble_column.upwards_inside")),
                                SoundSource.BLOCKS, 0.5f, 1.3f, false);
                    }
                }
                break;
            }
        }
    }
}
