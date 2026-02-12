package net.crystalnexus.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.crystalnexus.client.render.ParticleAcceleratorControllerRenderer;
import net.crystalnexus.client.render.QuarryBlockEntityRenderer;
import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.crystalnexus.block.entity.TankBlockEntity;
import net.crystalnexus.block.entity.ParticleAcceleratorControllerBlockEntity;
import net.crystalnexus.block.entity.QuarryBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;

import net.crystalnexus.client.render.ConveyerBeltBER;
import net.crystalnexus.init.CrystalnexusModBlockEntities;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {

@SubscribeEvent
public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {

    event.registerBlockEntityRenderer(
        (BlockEntityType<ConveyerBeltBaseBlockEntity>)
            CrystalnexusModBlockEntities.CONVEYER_BELT.get(),
        ConveyerBeltBER::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<ConveyerBeltBaseBlockEntity>)
            CrystalnexusModBlockEntities.CONVEYER_BELT_INPUT.get(),
        ConveyerBeltBER::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<ConveyerBeltBaseBlockEntity>)
            CrystalnexusModBlockEntities.CONVEYER_BELT_OUTPUT.get(),
        ConveyerBeltBER::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<TankBlockEntity>)
            CrystalnexusModBlockEntities.TANK.get(),
        net.crystalnexus.client.renderer.TankBER::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<ParticleAcceleratorControllerBlockEntity>)
            CrystalnexusModBlockEntities.PARTICLE_ACCELERATOR_CONTROLLER.get(),
        ParticleAcceleratorControllerRenderer::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<QuarryBlockEntity>)
            CrystalnexusModBlockEntities.QUARRY.get(),
        QuarryBlockEntityRenderer::new
    );
}
}