package net.crystalnexus.network;

import net.crystalnexus.network.payload.C2S_RequestPage;
import net.crystalnexus.network.payload.C2S_Withdraw;
import net.crystalnexus.network.payload.C2S_DepotCliRequest;
import net.crystalnexus.network.payload.C2S_DepotJeiRecipes;
import net.crystalnexus.network.payload.C2S_DepotCraftingRequest;
import net.crystalnexus.network.payload.S2C_SendPage;
import net.crystalnexus.network.payload.S2C_DepotCliResponse;
import net.crystalnexus.network.payload.S2C_DepotCraftingResponse;
import net.crystalnexus.network.payload.S2C_BlackHoleVisual;
import net.crystalnexus.network.payload.S2C_OreScanResult;
import net.crystalnexus.network.payload.S2C_OrbitalStrikeBeam;
import net.crystalnexus.network.payload.S2C_ZeroPointPreview;
import net.crystalnexus.network.payload.S2C_MaterialProfiles;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = DepotNetIds.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

	@SubscribeEvent
	public static void register(final RegisterPayloadHandlersEvent event) {
		PayloadRegistrar r = event.registrar(DepotNetIds.NETWORK_VERSION);

		r.playToServer(C2S_RequestPage.TYPE, C2S_RequestPage.STREAM_CODEC, ServerHandlers::onRequestPage);
		r.playToServer(C2S_Withdraw.TYPE,     C2S_Withdraw.STREAM_CODEC,     ServerHandlers::onWithdraw);
		r.playToServer(C2S_DepotCliRequest.TYPE, C2S_DepotCliRequest.STREAM_CODEC, ServerHandlers::onDepotCliRequest);
		r.playToServer(C2S_DepotJeiRecipes.TYPE, C2S_DepotJeiRecipes.STREAM_CODEC, ServerHandlers::onDepotJeiRecipes);
		r.playToServer(C2S_DepotCraftingRequest.TYPE, C2S_DepotCraftingRequest.STREAM_CODEC, ServerHandlers::onDepotCraftingRequest);

		r.playToClient(S2C_SendPage.TYPE, S2C_SendPage.STREAM_CODEC, ClientHandlers::onSendPage);
		r.playToClient(S2C_DepotCliResponse.TYPE, S2C_DepotCliResponse.STREAM_CODEC, ClientHandlers::onDepotCliResponse);
		r.playToClient(S2C_DepotCraftingResponse.TYPE, S2C_DepotCraftingResponse.STREAM_CODEC, ClientHandlers::onDepotCraftingResponse);

		// Ore scanner results
		r.playToClient(S2C_OreScanResult.TYPE, S2C_OreScanResult.STREAM_CODEC, ClientHandlers::onOreScanResult);

		r.playToClient(S2C_ZeroPointPreview.TYPE, S2C_ZeroPointPreview.STREAM_CODEC, ClientHandlers::onZeroPointPreview);
		r.playToClient(S2C_BlackHoleVisual.TYPE, S2C_BlackHoleVisual.STREAM_CODEC, ClientHandlers::onBlackHoleVisual);
		r.playToClient(S2C_OrbitalStrikeBeam.TYPE, S2C_OrbitalStrikeBeam.STREAM_CODEC, ClientHandlers::onOrbitalStrikeBeam);
		r.playToClient(S2C_MaterialProfiles.TYPE, S2C_MaterialProfiles.STREAM_CODEC, ClientHandlers::onMaterialProfiles);

	}
}
