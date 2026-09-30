package net.crystalnexus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.capabilities.Capabilities;

/** GUI reads must never run a machine's processing procedure. */
public final class MachineEnergyDisplay {
    private MachineEnergyDisplay() {}

    public static String text(LevelAccessor world, double x, double y, double z) {
        var energy = world instanceof Level level
            ? level.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null) : null;
        return "FE: " + (energy == null ? 0 : energy.getEnergyStored());
    }
}
