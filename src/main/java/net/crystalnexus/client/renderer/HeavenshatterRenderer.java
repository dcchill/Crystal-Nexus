package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.crystalnexus.block.entity.HeavenshatterBeamControllerBlockEntity;
import net.crystalnexus.multiblock.HeavenshatterMeteor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class HeavenshatterRenderer implements BlockEntityRenderer<HeavenshatterBeamControllerBlockEntity> {
    private record Surface(List<HeavenshatterMeteor.Cell> source, List<HeavenshatterMeteor.Cell> visible) {}
    private final Map<HeavenshatterBeamControllerBlockEntity, Surface> surfaces = new WeakHashMap<>();

    public HeavenshatterRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public boolean shouldRenderOffScreen(HeavenshatterBeamControllerBlockEntity controller) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override public void render(HeavenshatterBeamControllerBlockEntity controller, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        boolean escaping = controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.ESCAPING;
        if ((!escaping && controller.stage() != HeavenshatterBeamControllerBlockEntity.Stage.RETRIEVING) || controller.cells().isEmpty()) {
            surfaces.remove(controller);
            return;
        }
        var level = controller.getLevel();
        if (level == null) return;
        double fraction = controller.visualProgress(partialTick) / controller.duration();
        double start = Math.max(level.getMaxBuildHeight() + 16, controller.arrival().getY() + 128);
        double bottom = start + (controller.arrival().getY() - start) * fraction;
        double escape = escaping ? controller.visualEscape(partialTick) : 0;
        bottom += 48 * escape * escape;
        Vec3 base = Vec3.atLowerCornerOf(controller.meteorBase()).subtract(Vec3.atLowerCornerOf(controller.getBlockPos()));
        base = new Vec3(base.x, bottom - controller.getBlockPos().getY(), base.z);
        Surface surface = surfaces.get(controller);
        if (surface == null || surface.source() != controller.cells()) {
            surface = new Surface(controller.cells(), surface(controller.cells()));
            surfaces.put(controller, surface);
        }
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        float alpha = (float) (1 - Math.max(0, (escape - 0.4) / 0.6));
        MultiBufferSource meteorBuffers = escaping
            ? type -> new FadeVertices(buffers.getBuffer(Sheets.translucentCullBlockSheet()), alpha) : buffers;
        for (var cell : surface.visible()) {
            pose.pushPose();
            pose.translate(base.x + cell.x(), base.y + cell.y(), base.z + cell.z());
            // The meteor is in open sky, independent of the controller's ground lighting.
            dispatcher.renderSingleBlock(controller.palette().get(cell.material()), pose, meteorBuffers, LightTexture.pack(0, 15), OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        if (escaping || controller.status() != HeavenshatterBeamControllerBlockEntity.Status.PULLING) return;
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(Vec3.atLowerCornerOf(controller.getBlockPos()));
        double time = level.getGameTime() + partialTick;
        int count = controller.emitters().size();
        for (int i = 0; i < count; i++) {
            Vec3 from = Vec3.atCenterOf(controller.emitters().get(i)).subtract(Vec3.atLowerCornerOf(controller.getBlockPos()));
            Vec3 to = tractorTarget(from, base, controller.diameter());
            double pulse = 0.85 + 0.15 * Math.sin(time * 0.1 + i);
            beam(glow, pose.last().pose(), from, to, camera, 0.75 * pulse, 135, 40, 245, 36);
            beam(glow, pose.last().pose(), from, to, camera, 0.17 * pulse, 225, 155, 255, 210);
        }
    }

    static Vec3 tractorTarget(Vec3 from, Vec3 base, int diameter) {
        double centerX = base.x + diameter / 2.0, centerZ = base.z + diameter / 2.0;
        double dx = from.x - centerX, dz = from.z - centerZ;
        // Keep each beam on its emitter's side, vertical whenever it is already below the target area.
        double scale = Math.min(1, diameter * 0.3 / Math.max(0.0001, Math.hypot(dx, dz)));
        return new Vec3(centerX + dx * scale, base.y + diameter * 0.3, centerZ + dz * scale);
    }

    private static List<HeavenshatterMeteor.Cell> surface(List<HeavenshatterMeteor.Cell> cells) {
        var occupied = new HashSet<Integer>();
        for (var cell : cells) occupied.add(key(cell.x(), cell.y(), cell.z()));
        return cells.stream().filter(cell -> {
            for (Direction direction : Direction.values()) {
                if (!occupied.contains(key(cell.x() + direction.getStepX(), cell.y() + direction.getStepY(), cell.z() + direction.getStepZ()))) return true;
            }
            return false;
        }).toList();
    }

    private static int key(int x, int y, int z) { return (x + 1) + (y + 1) * 32 + (z + 1) * 1024; }

    private static void beam(VertexConsumer vertices, Matrix4f matrix, Vec3 from, Vec3 to, Vec3 camera,
                             double width, int r, int g, int b, int alpha) {
        Vec3 direction = to.subtract(from).normalize();
        Vec3 side = direction.cross(camera.subtract(from)).normalize();
        if (side.lengthSqr() < 0.0001) side = new Vec3(1, 0, 0);
        Vec3 up = direction.cross(side).normalize();
        quad(vertices, matrix, from, to, side.scale(width), r, g, b, alpha);
        quad(vertices, matrix, from, to, up.scale(width), r, g, b, alpha);
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix, Vec3 from, Vec3 to, Vec3 width, int r, int g, int b, int alpha) {
        vertex(vertices, matrix, from.subtract(width), r, g, b, alpha);
        vertex(vertices, matrix, from.add(width), r, g, b, alpha);
        vertex(vertices, matrix, to.add(width), r, g, b, alpha);
        vertex(vertices, matrix, to.subtract(width), r, g, b, alpha);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 pos, int r, int g, int b, int alpha) {
        vertices.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).setColor(r, g, b, alpha);
    }

    private record FadeVertices(VertexConsumer delegate, float alpha) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { delegate.setColor(r, g, b, (int) (a * alpha)); return this; }
        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
    }
}
