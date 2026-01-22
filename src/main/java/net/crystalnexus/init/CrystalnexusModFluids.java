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
import net.crystalnexus.fluid.GasolineFluid;
import net.crystalnexus.fluid.CrudeOilFluid;
import net.crystalnexus.CrystalnexusMod;

public class CrystalnexusModFluids {
	public static final DeferredRegister<Fluid> REGISTRY = DeferredRegister.create(BuiltInRegistries.FLUID, CrystalnexusMod.MODID);
	public static final DeferredHolder<Fluid, FlowingFluid> CRUDE_OIL = REGISTRY.register("crude_oil", () -> new CrudeOilFluid.Source());
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_CRUDE_OIL = REGISTRY.register("flowing_crude_oil", () -> new CrudeOilFluid.Flowing());
	public static final DeferredHolder<Fluid, FlowingFluid> GASOLINE = REGISTRY.register("gasoline", () -> new GasolineFluid.Source());
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_GASOLINE = REGISTRY.register("flowing_gasoline", () -> new GasolineFluid.Flowing());
	public static final DeferredHolder<Fluid, FlowingFluid> STEAM = REGISTRY.register("steam", () -> new SteamFluid.Source());
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_STEAM = REGISTRY.register("flowing_steam", () -> new SteamFluid.Flowing());

	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class FluidsClientSideHandler {
		@SubscribeEvent
		public static void clientSetup(FMLClientSetupEvent event) {
			ItemBlockRenderTypes.setRenderLayer(CRUDE_OIL.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_CRUDE_OIL.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(GASOLINE.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_GASOLINE.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(STEAM.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_STEAM.get(), RenderType.translucent());
		}
	}
}