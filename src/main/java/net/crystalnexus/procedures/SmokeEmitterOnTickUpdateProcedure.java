package net.crystalnexus.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public class SmokeEmitterOnTickUpdateProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {

        if (world instanceof ServerLevel serverLevel &&
            !serverLevel.hasNeighborSignal(BlockPos.containing(x, y, z))) {

            // Slow it down so it's not every tick
            if (serverLevel.getGameTime() % 3 != 0) return;

            RandomSource random = serverLevel.getRandom();

            for (int i = 0; i < 3; i++) {

                double offsetX = (random.nextDouble() - 0.5) * 0.08;
                double offsetZ = (random.nextDouble() - 0.5) * 0.08;
						serverLevel.sendParticles(
						        ParticleTypes.CAMPFIRE_COSY_SMOKE,
						        x + 0.5 + offsetX,
						        y + 1.05,
						        z + 0.5 + offsetZ,
						        0,          // IMPORTANT: set count to 0
						        0.0,
						        0.16,       // direct upward motion
						        0.0,
						        1.0         // ignored when count = 0
						);
            }
        }
    }
}