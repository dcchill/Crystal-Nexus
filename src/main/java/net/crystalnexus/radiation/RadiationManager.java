package net.crystalnexus.radiation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RadiationManager {

    private static final Set<IRadiationSource> SOURCES = new HashSet<>();

    private static final int SCAN_XZ = 12;
	private static final int SCAN_Y = 6;


    public static void register(IRadiationSource source) {
        SOURCES.add(source);
    }

    public static void unregister(IRadiationSource source) {
        SOURCES.remove(source);
    }

    public static double getRadiationAt(Level level, BlockPos pos) {

        double total = 0;

        // === Registered radiation sources (reactors etc.)
        Iterator<IRadiationSource> iterator = SOURCES.iterator();

        while (iterator.hasNext()) {
            IRadiationSource source = iterator.next();

            if (source == null || !source.isValid()) {
                iterator.remove();
                continue;
            }

            if (source.getLevel() != level)
                continue;

            BlockPos srcPos = source.getRadiationPos();

            double maxRadius = source.getRadiationRadius();

            if (!srcPos.closerThan(pos, maxRadius))
                continue;

            double dist = Math.sqrt(srcPos.distSqr(pos));
            double falloff = 1.0 / (dist + 1.0);

            total += source.getRadiationStrength() * falloff;
        }

        // === Tag-based radioactive blocks
			BlockPos min = pos.offset(-SCAN_XZ, -SCAN_Y, -SCAN_XZ);
			BlockPos max = pos.offset(SCAN_XZ, SCAN_Y, SCAN_XZ);



        for (BlockPos checkPos : BlockPos.betweenClosed(min, max)) {

            BlockState state = level.getBlockState(checkPos);

            if (!state.is(RadiationTags.RADIOACTIVE_BLOCKS))
                continue;

            double dist = Math.sqrt(checkPos.distSqr(pos));
            double falloff = 1.0 / (dist + 1.0);

            total += 40.0 * falloff; // base strength for tagged blocks
        }

        return total;
    }
}
