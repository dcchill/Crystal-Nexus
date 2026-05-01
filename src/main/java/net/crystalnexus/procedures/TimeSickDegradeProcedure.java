package net.crystalnexus.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.crystalnexus.radiation.RadiationLogic;

@EventBusSubscriber
public class TimeSickDegradeProcedure {
    private static final long EXPOSURE_GRACE_TICKS = 25;
    private static final double RECOVERY_PER_SECOND = 1000;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (entity.level().isClientSide())
            return;

        if (!(entity instanceof LivingEntity living))
            return;

        double currentTimeSick = entity.getPersistentData().getDouble("timeSick");

        // Only degrade once per second
        if (entity.tickCount % 20 != 0)
            return;

        long currentTick = living.level().getGameTime();
        long lastExposureTick = entity.getPersistentData().getLong(RadiationLogic.LAST_EXPOSURE_TICK_TAG);
        boolean isActivelyExposed = currentTick - lastExposureTick <= EXPOSURE_GRACE_TICKS;

        // Recover rapidly once no radiation source has exposed the player recently.
        if (!isActivelyExposed && currentTimeSick > 0) {

            entity.getPersistentData().putDouble(
                "timeSick",
                Math.max(0, currentTimeSick - RECOVERY_PER_SECOND)
            );
        }
    }
}
