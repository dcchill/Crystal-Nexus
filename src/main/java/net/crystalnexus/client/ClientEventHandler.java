package net.crystalnexus.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.crystalnexus.client.render.ParticleAcceleratorControllerRenderer;

import net.crystalnexus.client.render.ConveyerBeltBER;
import net.crystalnexus.init.CrystalnexusModBlockEntities;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        // register for all three belt BE types
        event.registerBlockEntityRenderer(CrystalnexusModBlockEntities.CONVEYER_BELT.get(), ConveyerBeltBER::new);
        event.registerBlockEntityRenderer(CrystalnexusModBlockEntities.CONVEYER_BELT_INPUT.get(), ConveyerBeltBER::new);
        event.registerBlockEntityRenderer(CrystalnexusModBlockEntities.CONVEYER_BELT_OUTPUT.get(), ConveyerBeltBER::new);
        event.registerBlockEntityRenderer(CrystalnexusModBlockEntities.TANK.get(),net.crystalnexus.client.renderer.TankBER::new);
        event.registerBlockEntityRenderer(CrystalnexusModBlockEntities.PARTICLE_ACCELERATOR_CONTROLLER.get(),ParticleAcceleratorControllerRenderer::new);
}}