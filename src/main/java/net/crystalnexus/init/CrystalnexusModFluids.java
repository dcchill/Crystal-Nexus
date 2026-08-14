/*
 * MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

import net.crystalnexus.fluid.SteamFluid;
import net.crystalnexus.fluid.OverfuelFluid;
import net.crystalnexus.fluid.TemporalEssenceFluid;
import net.crystalnexus.fluid.GasolineFluid;
import net.crystalnexus.fluid.CrudeOilFluid;
import net.crystalnexus.fluid.SulfuricAcidFluid;
import net.crystalnexus.fluid.AcidicSlurryFluid;
import net.crystalnexus.fluid.ResinFluid;
import net.crystalnexus.fluid.InversionSolutionFluid;
import net.crystalnexus.fluid.MineralSlurryFluid;
import net.crystalnexus.CrystalnexusMod;

public class CrystalnexusModFluids {
	public static final DeferredRegister<Fluid> REGISTRY = DeferredRegister.create(BuiltInRegistries.FLUID, CrystalnexusMod.MODID);
	public static final DeferredHolder<Fluid, FlowingFluid> CRUDE_OIL = REGISTRY.register("crude_oil", () -> new CrudeOilFluid.Source());
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_CRUDE_OIL = REGISTRY.register("flowing_crude_oil", () -> new CrudeOilFluid.Flowing());
	public static final DeferredHolder<Fluid, FlowingFluid> SULFURIC_ACID = REGISTRY.register("sulfuric_acid", SulfuricAcidFluid.Source::new);
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_SULFURIC_ACID = REGISTRY.register("flowing_sulfuric_acid", SulfuricAcidFluid.Flowing::new);
	public static final DeferredHolder<Fluid, FlowingFluid> ACIDIC_SLURRY = REGISTRY.register("acidic_slurry", AcidicSlurryFluid.Source::new);
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_ACIDIC_SLURRY = REGISTRY.register("flowing_acidic_slurry", AcidicSlurryFluid.Flowing::new);
	public static final DeferredHolder<Fluid, FlowingFluid> RESIN = REGISTRY.register("resin", ResinFluid.Source::new);
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_RESIN = REGISTRY.register("flowing_resin", ResinFluid.Flowing::new);
	public static final DeferredHolder<Fluid, FlowingFluid> INVERSION_SOLUTION = REGISTRY.register("inversion_solution", InversionSolutionFluid.Source::new);
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_INVERSION_SOLUTION = REGISTRY.register("flowing_inversion_solution", InversionSolutionFluid.Flowing::new);
	public static final DeferredHolder<Fluid, FlowingFluid> GASOLINE = REGISTRY.register("gasoline", () -> new GasolineFluid.Source());
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_GASOLINE = REGISTRY.register("flowing_gasoline", () -> new GasolineFluid.Flowing());
	public static final DeferredHolder<Fluid, FlowingFluid> STEAM = REGISTRY.register("steam", () -> new SteamFluid.Source());
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_STEAM = REGISTRY.register("flowing_steam", () -> new SteamFluid.Flowing());
	public static final DeferredHolder<Fluid, FlowingFluid> OVERFUEL = REGISTRY.register("overfuel", () -> new OverfuelFluid.Source());
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_OVERFUEL = REGISTRY.register("flowing_overfuel", () -> new OverfuelFluid.Flowing());
	public static final DeferredHolder<Fluid, FlowingFluid> TEMPORAL_ESSENCE = REGISTRY.register("temporal_essence", () -> new TemporalEssenceFluid.Source());
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_TEMPORAL_ESSENCE = REGISTRY.register("flowing_temporal_essence", () -> new TemporalEssenceFluid.Flowing());
	public static final DeferredHolder<Fluid, FlowingFluid> MINERAL_SLURRY = REGISTRY.register("mineral_slurry", MineralSlurryFluid.Source::new);
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_MINERAL_SLURRY = REGISTRY.register("flowing_mineral_slurry", MineralSlurryFluid.Flowing::new);

	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class FluidsClientSideHandler {
		@SubscribeEvent
		public static void clientSetup(FMLClientSetupEvent event) {
			ItemBlockRenderTypes.setRenderLayer(CrystalnexusModBlocks.TEMPORAL_EXPLOITER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(CRUDE_OIL.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_CRUDE_OIL.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(SULFURIC_ACID.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_SULFURIC_ACID.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(ACIDIC_SLURRY.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_ACIDIC_SLURRY.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(RESIN.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_RESIN.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(INVERSION_SOLUTION.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_INVERSION_SOLUTION.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(GASOLINE.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_GASOLINE.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(STEAM.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_STEAM.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(OVERFUEL.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_OVERFUEL.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(TEMPORAL_ESSENCE.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_TEMPORAL_ESSENCE.get(), RenderType.translucent());
		}
	}
}
