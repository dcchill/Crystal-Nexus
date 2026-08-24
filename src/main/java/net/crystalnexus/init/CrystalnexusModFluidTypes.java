/*
 * MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.fluids.FluidType;

import net.crystalnexus.fluid.types.SteamFluidType;
import net.crystalnexus.fluid.types.OverfuelFluidType;
import net.crystalnexus.fluid.types.TemporalEssenceFluidType;
import net.crystalnexus.fluid.types.GasolineFluidType;
import net.crystalnexus.fluid.types.CrudeOilFluidType;
import net.crystalnexus.fluid.types.SulfuricAcidFluidType;
import net.crystalnexus.fluid.types.AcidicSlurryFluidType;
import net.crystalnexus.fluid.types.ResinFluidType;
import net.crystalnexus.fluid.types.InversionSolutionFluidType;
import net.crystalnexus.fluid.types.MineralSlurryFluidType;
import net.crystalnexus.fluid.types.ArgonFluidType;
import net.crystalnexus.fluid.types.OxygenFluidType;
import net.crystalnexus.CrystalnexusMod;

public class CrystalnexusModFluidTypes {
	public static final DeferredRegister<FluidType> REGISTRY = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, CrystalnexusMod.MODID);
	public static final DeferredHolder<FluidType, FluidType> CRUDE_OIL_TYPE = REGISTRY.register("crude_oil", () -> new CrudeOilFluidType());
	public static final DeferredHolder<FluidType, FluidType> SULFURIC_ACID_TYPE = REGISTRY.register("sulfuric_acid", SulfuricAcidFluidType::new);
	public static final DeferredHolder<FluidType, FluidType> ACIDIC_SLURRY_TYPE = REGISTRY.register("acidic_slurry", AcidicSlurryFluidType::new);
	public static final DeferredHolder<FluidType, FluidType> RESIN_TYPE = REGISTRY.register("resin", ResinFluidType::new);
	public static final DeferredHolder<FluidType, FluidType> INVERSION_SOLUTION_TYPE = REGISTRY.register("inversion_solution", InversionSolutionFluidType::new);
	public static final DeferredHolder<FluidType, FluidType> GASOLINE_TYPE = REGISTRY.register("gasoline", () -> new GasolineFluidType());
	public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = REGISTRY.register("steam", () -> new SteamFluidType());
	public static final DeferredHolder<FluidType, FluidType> OVERFUEL_TYPE = REGISTRY.register("overfuel", () -> new OverfuelFluidType());
	public static final DeferredHolder<FluidType, FluidType> TEMPORAL_ESSENCE_TYPE = REGISTRY.register("temporal_essence", () -> new TemporalEssenceFluidType());
	public static final DeferredHolder<FluidType, FluidType> MINERAL_SLURRY_TYPE = REGISTRY.register("mineral_slurry", MineralSlurryFluidType::new);
	public static final DeferredHolder<FluidType, FluidType> ARGON_TYPE = REGISTRY.register("argon", ArgonFluidType::new);
	public static final DeferredHolder<FluidType, FluidType> OXYGEN_TYPE = REGISTRY.register("oxygen", OxygenFluidType::new);
}
