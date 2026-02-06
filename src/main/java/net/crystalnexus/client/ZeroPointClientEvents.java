package net.crystalnexus.client;

import net.crystalnexus.client.preview.ZeroPointPreviewRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(
        modid = "crystalnexus",
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME
)
public class ZeroPointClientEvents {

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        ZeroPointPreviewRenderer.onRenderLevelStage(event);
    }
}
