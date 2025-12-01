/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;

import net.crystalnexus.CrystalnexusMod;

public class CrystalnexusModParticleTypes {
	public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(Registries.PARTICLE_TYPE, CrystalnexusMod.MODID);
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LASER_PART = REGISTRY.register("laser_part", () -> new SimpleParticleType(false));
}