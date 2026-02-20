package net.crystalnexus.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.crystalnexus.init.CrystalnexusModMobEffects;

@EventBusSubscriber
public class TimeSickDegradeProcedure {

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

        // Only recover if NOT irradiated
        if (!living.hasEffect(CrystalnexusModMobEffects.RADIATION_SICKNESS)
                && currentTimeSick > 0) {

            entity.getPersistentData().putDouble(
                "timeSick",
                Math.max(0, currentTimeSick - 1)
            );
        }
    }
}