package net.crystalnexus.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.crystalnexus.init.CrystalnexusModMobEffects;

public class RadiationLogic {

    private static final int MAX_RADIUS = 24;
    private static final int MAX_AMPLIFIER = 4;

    private static final int EFFECT_DURATION_TICKS = 60; // 3 seconds
    private static final int MIN_AMOUNT = 1;

		public static void radiateFrom(ServerLevel level, BlockPos sourcePos, int amount) {
		    if (amount < MIN_AMOUNT) return;
		
		    int radius = calculateRadius(amount);
		    int amplifier = calculateAmplifier(amount);
		
		    AABB area = new AABB(sourcePos).inflate(radius);
		
		    for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
		
		        if (entity instanceof Player p && p.isCreative()) continue;
		
		        // Apply radiation effect
		        entity.addEffect(new MobEffectInstance(
		                CrystalnexusModMobEffects.RADIATION_SICKNESS,
		                EFFECT_DURATION_TICKS,
		                amplifier,
		                true,
		                true
		        ));
		
		        // 🔥 Store radiation value for Geiger
		        if (entity instanceof Player player) {
		        }
		    }
		}


    private static int calculateRadius(int amount) {
        int r = 2 + (int) Math.sqrt(amount) + (amount / 64);
        return Math.min(r, MAX_RADIUS);
    }

    private static int calculateAmplifier(int amount) {
        return Math.min(amount / 64, MAX_AMPLIFIER);
    }
}
