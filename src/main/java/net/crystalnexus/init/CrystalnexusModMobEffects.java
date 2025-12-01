/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.Registries;

import net.crystalnexus.potion.RadiationSicknessMobEffect;
import net.crystalnexus.CrystalnexusMod;

public class CrystalnexusModMobEffects {
	public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(Registries.MOB_EFFECT, CrystalnexusMod.MODID);
	public static final DeferredHolder<MobEffect, MobEffect> RADIATION_SICKNESS = REGISTRY.register("radiation_sickness", () -> new RadiationSicknessMobEffect());
}