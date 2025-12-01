package net.crystalnexus.potion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

import net.crystalnexus.procedures.RadiationSicknessOnEffectActiveTickProcedure;

public class RadiationSicknessMobEffect extends MobEffect {
	public RadiationSicknessMobEffect() {
		super(MobEffectCategory.HARMFUL, -16750900);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		RadiationSicknessOnEffectActiveTickProcedure.execute(entity.level(), entity);
		return super.applyEffectTick(entity, amplifier);
	}
}