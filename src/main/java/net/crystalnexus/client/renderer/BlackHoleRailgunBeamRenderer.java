package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.client.BlackHoleRailgunClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, value = Dist.CLIENT)
public final class BlackHoleRailgunBeamRenderer {
    private static final int DURATION_TICKS = 8;
    private static final int SIDES = 12;
    private static RenderType beamType;
    private static final List<Beam> BEAMS = new ArrayList<>();
    private static ClientLevel visualLevel;

    public static void add(int shooterId, Vec3 start, Vec3 end, float charge) {
        var level = Minecraft.getInstance().level;
        if (level == null || start.distanceToSqr(end) < 1.0e-6) return;
        if (visualLevel != level) BEAMS.clear();
        visualLevel = level;
        var player = Minecraft.getInstance().player;
        boolean centered = player != null && player.getId() == shooterId && BlackHoleRailgunClientEvents.isScoped();
        BEAMS.add(new Beam(start, end, level.getGameTime(), centered, Math.clamp(charge, 0, 1)));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level != visualLevel) {
            BEAMS.clear();
            visualLevel = level;
        }
        if (level != null) BEAMS.removeIf(beam -> level.getGameTime() - beam.startTick() >= DURATION_TICKS + beam.charge() * 8);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || BEAMS.isEmpty()) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level != visualLevel) return;
        double time = minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        if (beamType == null) {
            beamType = RenderType.create("crystalnexus_railgun_beam",
                    DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536, false, false,
                    RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setOutputState(RenderStateShard.WEATHER_TARGET)
                    .createCompositeState(false));
        }
        var buffers = minecraft.renderBuffers().bufferSource();
        var consumer = buffers.getBuffer(beamType);
        var matrix = event.getPoseStack().last().pose();
        for (var beam : BEAMS) {
            float fade = (float) Math.max(0, 1 - (time - beam.startTick()) / (DURATION_TICKS + beam.charge() * 8));
            drawBeam(consumer, matrix, beam.start().subtract(camera), beam.end().subtract(camera), fade, beam.centered(), beam.charge());
        }
        buffers.endBatch(beamType);
    }

    static void drawBeam(VertexConsumer consumer, Matrix4f matrix, Vec3 start, Vec3 end, float fade, boolean centered, float charge) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0e-6 || fade <= 0) return;
        direction = direction.normalize();
        Vec3 axis = Math.abs(direction.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = direction.cross(axis).normalize();
        Vec3 up = side.cross(direction).normalize();
        start = start.add(direction.scale(Math.min(0.8, start.distanceTo(end) * 0.5)));
        start = start.add(side.scale(centered ? 0.06 : 0.35));
        if (!centered) start = start.add(0, -0.1, 0);
        double width = (0.6 + 0.4 * fade) * (1 + 2 * charge);
        drawTube(consumer, matrix, start, end, side, up, 0.24 * width, 125, 30, 255, (int) (55 * fade));
        drawTube(consumer, matrix, start, end, side, up, 0.10 * width, 195, 95, 255, (int) (150 * fade));
        drawTube(consumer, matrix, start, end, side, up, 0.035 * width, 255, 245, 255, (int) (255 * fade));
    }

    private static void drawTube(VertexConsumer consumer, Matrix4f matrix, Vec3 start, Vec3 end,
            Vec3 side, Vec3 up, double radius, int r, int g, int b, int alpha) {
        for (int i = 0; i < SIDES; i++) {
            double a = Math.PI * 2 * i / SIDES, next = Math.PI * 2 * (i + 1) / SIDES;
            Vec3 offset = side.scale(Math.cos(a) * radius).add(up.scale(Math.sin(a) * radius));
            Vec3 nextOffset = side.scale(Math.cos(next) * radius).add(up.scale(Math.sin(next) * radius));
            vertex(consumer, matrix, start.add(offset), r, g, b, alpha);
            vertex(consumer, matrix, start.add(nextOffset), r, g, b, alpha);
            vertex(consumer, matrix, end.add(nextOffset), r, g, b, alpha);
            vertex(consumer, matrix, end.add(offset), r, g, b, alpha);
            // Cap the impact end so the beam's core remains visible when looking along it through the scope.
            vertex(consumer, matrix, end, r, g, b, alpha);
            vertex(consumer, matrix, end.add(offset), r, g, b, alpha);
            vertex(consumer, matrix, end.add(nextOffset), r, g, b, alpha);
            vertex(consumer, matrix, end, r, g, b, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 point, int r, int g, int b, int alpha) {
        consumer.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z).setColor(r, g, b, alpha);
    }

    private record Beam(Vec3 start, Vec3 end, long startTick, boolean centered, float charge) {}
}
