/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import net.crystalnexus.CrystalnexusMod;

public class CrystalnexusModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, CrystalnexusMod.MODID);
	public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_FAILURE = REGISTRY.register("reactor_failure", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crystalnexus", "reactor_failure")));
	public static final DeferredHolder<SoundEvent, SoundEvent> ZERO_POINT_ACTIVE = REGISTRY.register("zero_point_active", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crystalnexus", "zero_point_active")));
	public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_CLICK = REGISTRY.register("geiger_click", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crystalnexus", "geiger_click")));
}