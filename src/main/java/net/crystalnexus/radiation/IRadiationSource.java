package net.crystalnexus.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IRadiationSource {

    Level getLevel();

    BlockPos getRadiationPos();

    double getRadiationStrength();

    double getRadiationRadius();

    default boolean isValid() {
        return true;
    }
}
