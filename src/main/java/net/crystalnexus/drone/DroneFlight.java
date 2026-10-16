package net.crystalnexus.drone;

import net.crystalnexus.entity.LUNADroneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Bounded, incremental routes through already loaded chunks using vanilla flight navigation. */
public final class DroneFlight {
    public enum Result { RUNNING, SUCCESS, FAILURE }
    private final LUNADroneEntity drone;
    private Vec3 previous, target;
    private int ticks, stalled;
    public String error = "";
    public DroneFlight(LUNADroneEntity drone) { this.drone = drone; }
    public void reset() { ticks = stalled = 0; previous = target = null; error = ""; drone.hover(); }
    public String destination() { return target == null ? "None" : BlockPos.containing(target).toShortString(); }
    public Result tick(Vec3 destination, double distance, double speed, boolean persistent) {
        target = destination;
        if (!Double.isFinite(destination.lengthSqr()) || !drone.level().isInWorldBounds(BlockPos.containing(destination))) return fail("Destination is outside the world");
        if (!drone.level().hasChunkAt(BlockPos.containing(destination))) return fail("Destination chunk is unloaded");
        double remaining = drone.position().distanceTo(destination);
        if (remaining <= Math.max(.75, distance)) { drone.hover(); stalled = ticks = 0; previous = drone.position(); return Result.SUCCESS; }
        if (drone.battery.getEnergyStored() < 5) return fail("Insufficient battery to navigate; recharge and resume");
        if (!persistent && ++ticks > 2400) return fail("Navigation timed out");
        if (previous != null && drone.position().distanceToSqr(previous) < .0001) stalled++;
        else { stalled = 0; drone.battery.extractEnergy(5, false); }
        previous = drone.position();
        if (stalled > 200) return fail("Destination is unreachable or its route is unloaded");
        if (drone.tickCount % 10 == 0 || drone.getNavigation().isDone()) {
            Vec3 waypoint = remaining > 12 ? drone.position().add(destination.subtract(drone.position()).normalize().scale(12)) : destination;
            if (!drone.level().hasChunkAt(BlockPos.containing(waypoint))) return fail("Route enters an unloaded chunk");
            drone.getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, Math.max(.1, Math.min(2, speed)));
        }
        return Result.RUNNING;
    }
    private Result fail(String message) { error = message; drone.hover(); return Result.FAILURE; }
}
