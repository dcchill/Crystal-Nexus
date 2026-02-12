/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.crystalnexus.client.model.Modellaser_beam;
import net.crystalnexus.client.model.Modeljet_pack;
import net.crystalnexus.client.model.Modelhover_pack;
import net.crystalnexus.client.model.Modeldrone;
import net.crystalnexus.client.model.Modelbomb;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class CrystalnexusModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modeldrone.LAYER_LOCATION, Modeldrone::createBodyLayer);
		event.registerLayerDefinition(Modellaser_beam.LAYER_LOCATION, Modellaser_beam::createBodyLayer);
		event.registerLayerDefinition(Modeljet_pack.LAYER_LOCATION, Modeljet_pack::createBodyLayer);
		event.registerLayerDefinition(Modelbomb.LAYER_LOCATION, Modelbomb::createBodyLayer);
		event.registerLayerDefinition(Modelhover_pack.LAYER_LOCATION, Modelhover_pack::createBodyLayer);
	}
}