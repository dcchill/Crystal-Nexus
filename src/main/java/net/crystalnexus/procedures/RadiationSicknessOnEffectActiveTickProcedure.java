package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import net.crystalnexus.init.CrystalnexusModMobEffects;
import net.crystalnexus.radiation.RadiationLogic;

public class RadiationSicknessOnEffectActiveTickProcedure {
private static final String TIME_SICK_TAG = "timeSick";
private static final long EXPOSURE_GRACE_TICKS = 25;
private static final double RECOVERY_PER_TICK = 50;

public static void execute(LevelAccessor world, Entity entity) {
    if (entity == null)
        return;

    if (entity instanceof LivingEntity living) {

        ItemStack chest = living.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasHazmat = chest.getEnchantmentLevel(
                world.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(ResourceKey.create(
                                Registries.ENCHANTMENT,
                                ResourceLocation.parse("crystalnexus:hazmat")
                        ))
        ) > 0;

        long currentTick = living.level().getGameTime();
        long lastExposureTick = entity.getPersistentData().getLong(RadiationLogic.LAST_EXPOSURE_TICK_TAG);
        boolean isActivelyExposed = currentTick - lastExposureTick <= EXPOSURE_GRACE_TICKS;

        if (hasHazmat || !isActivelyExposed) {
            recoverTimeSick(living);
            return;
        }

        if (!hasHazmat) {

            // increment once per tick
            double time = entity.getPersistentData().getDouble(TIME_SICK_TAG) + 1;
            entity.getPersistentData().putDouble(TIME_SICK_TAG, time);

            if (!living.level().isClientSide())
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 1, false, false));

            if (time >= 1000 && !living.level().isClientSide())
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 1, false, false));

            if (time >= 1500) {
                if (living.getRandom().nextInt(20) == 0) {
                    living.hurt(
                        new DamageSource(world.holderOrThrow(
                                ResourceKey.create(
                                        Registries.DAMAGE_TYPE,
                                        ResourceLocation.parse("crystalnexus:rad_sickness")
                                ))),
                        2
                    );
                }
            }
        }
    }
}

private static void recoverTimeSick(LivingEntity living) {
    double time = living.getPersistentData().getDouble(TIME_SICK_TAG);
    if (time <= 0) {
        living.getPersistentData().putDouble(TIME_SICK_TAG, 0);
        if (!living.level().isClientSide())
            living.removeEffect(CrystalnexusModMobEffects.RADIATION_SICKNESS);
        return;
    }

    double recoveredTime = Math.max(0, time - RECOVERY_PER_TICK);
    living.getPersistentData().putDouble(TIME_SICK_TAG, recoveredTime);

    if (!living.level().isClientSide()) {
        if (recoveredTime > 0) {
            living.addEffect(new MobEffectInstance(CrystalnexusModMobEffects.RADIATION_SICKNESS, 20, 0, false, false));
        } else {
            living.removeEffect(CrystalnexusModMobEffects.RADIATION_SICKNESS);
        }
    }
}
}
