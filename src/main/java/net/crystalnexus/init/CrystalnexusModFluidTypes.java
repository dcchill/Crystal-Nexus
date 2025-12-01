/*
 * MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.fluids.FluidType;

import net.crystalnexus.fluid.types.SteamFluidType;
import net.crystalnexus.fluid.types.GasolineFluidType;
import net.crystalnexus.fluid.types.CrystalGloopFluidType;
import net.crystalnexus.fluid.types.CrudeOilFluidType;
import net.crystalnexus.CrystalnexusMod;

public class CrystalnexusModFluidTypes {
	public static final DeferredRegister<FluidType> REGISTRY = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, CrystalnexusMod.MODID);
	public static final DeferredHolder<FluidType, FluidType> CRYSTAL_GLOOP_TYPE = REGISTRY.register("crystal_gloop", () -> new CrystalGloopFluidType());
	public static final DeferredHolder<FluidType, FluidType> CRUDE_OIL_TYPE = REGISTRY.register("crude_oil", () -> new CrudeOilFluidType());
	public static final DeferredHolder<FluidType, FluidType> GASOLINE_TYPE = REGISTRY.register("gasoline", () -> new GasolineFluidType());
	public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = REGISTRY.register("steam", () -> new SteamFluidType());
}