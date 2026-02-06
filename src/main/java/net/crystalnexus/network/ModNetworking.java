package net.crystalnexus.network;

import net.crystalnexus.network.payload.C2S_RequestPage;
import net.crystalnexus.network.payload.C2S_Withdraw;
import net.crystalnexus.network.payload.S2C_SendPage;
import net.crystalnexus.network.payload.S2C_OreScanResult;
import net.crystalnexus.network.payload.S2C_ZeroPointPreview;

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

		r.playToClient(S2C_SendPage.TYPE, S2C_SendPage.STREAM_CODEC, ClientHandlers::onSendPage);

		// Ore scanner results
		r.playToClient(S2C_OreScanResult.TYPE, S2C_OreScanResult.STREAM_CODEC, ClientHandlers::onOreScanResult);

		r.playToClient(S2C_ZeroPointPreview.TYPE, S2C_ZeroPointPreview.STREAM_CODEC, ClientHandlers::onZeroPointPreview);

	}
}
