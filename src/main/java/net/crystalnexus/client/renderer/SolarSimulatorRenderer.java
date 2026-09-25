package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.crystalnexus.block.entity.SolarSimulatorControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SolarSimulatorRenderer implements BlockEntityRenderer<SolarSimulatorControllerBlockEntity> {
    private static final int STACKS = 32;
    private static final int SLICES = 64;
    private static final int HALO_SEGMENTS = 32;
    private static final int DYSON_FRAME_COLOR = 0xFF9DDFFF;
    private static final int DYSON_FRAME_SIDE_COLOR = 0xFF6BBDEB;
    private static final float[][] BACKGROUND_STARS = createBackgroundStars();
    private static final List<Vec3[]> DYSON_CELLS = createDysonCells();
    private static final RenderType VOID_RENDER_TYPE = RenderType.create(
        "crystalnexus_solar_simulator_void",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        8192,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
            .createCompositeState(false)
    );
    private static final RenderType GLOW_RENDER_TYPE = RenderType.create(
        "crystalnexus_solar_simulator_glow",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.TRIANGLES,
        2048,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(false)
    );
    private static final RenderType DYSON_RENDER_TYPE = RenderType.create(
        "crystalnexus_solar_simulator_dyson", DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.TRIANGLES, 8192, false, false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(false));
    
    private final ItemRenderer itemRenderer;

    public SolarSimulatorRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override public int getViewDistance() { return 192; }

    @Override public AABB getRenderBoundingBox(SolarSimulatorControllerBlockEntity controller) {
        Vec3 center = controller.getFormationCenter();
        return center == null ? new AABB(controller.getBlockPos()) : new AABB(center, center).inflate(11.0D);
    }

    @Override
    public void render(SolarSimulatorControllerBlockEntity controller, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Vec3 center = controller.getFormationCenter();
        if (!controller.isFormed() || center == null || controller.getLevel() == null
            || !controller.isDysonMode() && !controller.isRenderActive()) return;

        double time = controller.getLevel().getGameTime() + partialTick;
        poseStack.pushPose();
        poseStack.translate(center.x - controller.getBlockPos().getX(),
            center.y - controller.getBlockPos().getY(), center.z - controller.getBlockPos().getZ());

        Vec3 cameraOffset = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
            .subtract(center);
        if (!controller.isDysonMode()) {
            float voidRadius = 9.00F + (float) Math.sin(time * 0.04D) * 0.08F;
            VertexConsumer backdrop = buffers.getBuffer(VOID_RENDER_TYPE);
            drawVoidSphere(poseStack.last().pose(), backdrop, voidRadius, cameraOffset);
            drawBackgroundStars(poseStack.last().pose(), backdrop, voidRadius - 0.06F, cameraOffset);
        }

        ItemStack star = controller.getItem(4);
        renderSunGlow(star, poseStack, buffers, (float) time);
        renderBody(star, poseStack, buffers, (float) (time * 1.8D % 360.0D), 2.30F, controller);

        if (controller.isDysonMode()) {
            if (!star.isEmpty() && controller.getDysonStructures() > 0)
                drawDysonShell(poseStack.last().pose(), buffers.getBuffer(DYSON_RENDER_TYPE), controller);
            poseStack.popPose();
            return;
        }

        for (int slot = 0; slot < 4; slot++) {
            ItemStack planet = controller.getItem(slot);
            if (planet.isEmpty()) continue;
            float radius = 3.25F + slot * 1.25F;
            double speed = 0.025D + slot * 0.004D;
            float angle = (float) (time * speed % Mth.TWO_PI) + slot * Mth.HALF_PI;
            float verticalAngle = (float) (time * speed * 0.73D % Mth.TWO_PI) + slot;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * radius,
                Mth.sin(verticalAngle) * 0.56F, Mth.sin(angle) * radius);
            renderBody(planet, poseStack, buffers,
                (float) (-time * (1.2D + slot * 0.2D) % 360.0D), 0.87F, controller);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void drawDysonShell(Matrix4f matrix, VertexConsumer consumer, SolarSimulatorControllerBlockEntity controller) {
        int built = Math.min(DYSON_CELLS.size(), (int) Math.ceil(DYSON_CELLS.size()
            * Math.min(1.0, controller.getDysonStructures() / 512.0)));
        int carbon = Math.min(built, (int) Math.ceil(DYSON_CELLS.size()
            * controller.getDysonCarbonSheets() / (6.0 * 512.0)));
        int solar = Math.min(built - carbon, (int) Math.ceil(DYSON_CELLS.size()
            * controller.getDysonSolarSheets() / (6.0 * 512.0)));
        for (int i = 0; i < built; i++) {
            Vec3[] cell = DYSON_CELLS.get(i);
            Vec3 center = Vec3.ZERO;
            for (Vec3 point : cell) center = center.add(point);
            center = center.scale(1.0 / cell.length).normalize().scale(5.5);
            int panel = i < carbon ? 0x66313131 : i < carbon + solar ? 0x665BBEFF : 0;
            for (int edge = 0; edge < cell.length; edge++) {
                Vec3 start = cell[edge].scale(5.5);
                Vec3 end = cell[(edge + 1) % cell.length].scale(5.5);
                if (panel != 0) triangle(consumer, matrix, center, start, end, panel);
                // A wide band facing into the cell, with an outward-facing side for visible depth.
                Vec3 innerStart = start.lerp(center, 0.15).normalize().scale(5.57);
                Vec3 innerEnd = end.lerp(center, 0.15).normalize().scale(5.57);
                Vec3 outerStart = start.normalize().scale(5.57);
                Vec3 outerEnd = end.normalize().scale(5.57);
                triangle(consumer, matrix, outerStart, outerEnd, innerEnd, DYSON_FRAME_COLOR);
                triangle(consumer, matrix, outerStart, innerEnd, innerStart, DYSON_FRAME_COLOR);
                triangle(consumer, matrix, outerStart, start.normalize().scale(5.42), end.normalize().scale(5.42), DYSON_FRAME_SIDE_COLOR);
                triangle(consumer, matrix, outerStart, end.normalize().scale(5.42), outerEnd, DYSON_FRAME_SIDE_COLOR);
            }
        }
    }

    private static void triangle(VertexConsumer consumer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, int argb) {
        for (Vec3 point : new Vec3[] { a, b, c }) consumer.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24);
    }

    /** Dual of a frequency-three icosahedron: hexagonal faces plus twelve pentagons. */
    private static List<Vec3[]> createDysonCells() {
        double t = (1.0 + Math.sqrt(5.0)) / 2.0;
        Vec3[] vertices = {
            new Vec3(-1,t,0), new Vec3(1,t,0), new Vec3(-1,-t,0), new Vec3(1,-t,0),
            new Vec3(0,-1,t), new Vec3(0,1,t), new Vec3(0,-1,-t), new Vec3(0,1,-t),
            new Vec3(t,0,-1), new Vec3(t,0,1), new Vec3(-t,0,-1), new Vec3(-t,0,1)
        };
        int[][] faces = {
            {0,11,5},{0,5,1},{0,1,7},{0,7,10},{0,10,11},
            {1,5,9},{5,11,4},{11,10,2},{10,7,6},{7,1,8},
            {3,9,4},{3,4,2},{3,2,6},{3,6,8},{3,8,9},
            {4,9,5},{2,4,11},{6,2,10},{8,6,7},{9,8,1}
        };
        List<Vec3> nodes = new ArrayList<>();
        List<List<Vec3>> adjacent = new ArrayList<>();
        for (int[] face : faces) {
            int[][] indices = new int[4][4];
            for (int a = 0; a <= 3; a++) for (int b = 0; b <= 3 - a; b++) {
                Vec3 point = vertices[face[0]].scale((3 - a - b) / 3.0)
                    .add(vertices[face[1]].scale(a / 3.0)).add(vertices[face[2]].scale(b / 3.0)).normalize();
                int index = -1;
                for (int i = 0; i < nodes.size(); i++) if (nodes.get(i).distanceToSqr(point) < 1.0e-8) { index = i; break; }
                if (index < 0) { index = nodes.size(); nodes.add(point); adjacent.add(new ArrayList<>()); }
                indices[a][b] = index;
            }
            for (int a = 0; a < 3; a++) for (int b = 0; b < 3 - a; b++) {
                addDysonTriangle(indices[a][b], indices[a+1][b], indices[a][b+1], nodes, adjacent);
                if (a + b < 2) addDysonTriangle(indices[a+1][b], indices[a+1][b+1], indices[a][b+1], nodes, adjacent);
            }
        }
        List<Vec3[]> cells = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Vec3 normal = nodes.get(i);
            Vec3 tangent = normal.cross(new Vec3(0, 1, 0));
            if (tangent.lengthSqr() < 0.01) tangent = normal.cross(new Vec3(1, 0, 0));
            tangent = tangent.normalize();
            Vec3 bitangent = normal.cross(tangent);
            Vec3 x = tangent, y = bitangent;
            adjacent.get(i).sort(Comparator.comparingDouble(p -> Math.atan2(p.dot(y), p.dot(x))));
            cells.add(adjacent.get(i).toArray(Vec3[]::new));
        }
        return cells;
    }

    private static void addDysonTriangle(int a, int b, int c, List<Vec3> nodes, List<List<Vec3>> adjacent) {
        Vec3 center = nodes.get(a).add(nodes.get(b)).add(nodes.get(c)).normalize();
        adjacent.get(a).add(center);
        adjacent.get(b).add(center);
        adjacent.get(c).add(center);
    }

    private void renderBody(ItemStack stack, PoseStack poseStack, MultiBufferSource buffers, float rotation,
                            float scale, SolarSimulatorControllerBlockEntity controller) {
        if (stack.isEmpty()) return;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY, poseStack, buffers, controller.getLevel(),
            (int) (controller.getBlockPos().asLong() + stack.getItem().hashCode()));
        poseStack.popPose();
    }

    private static void renderSunGlow(ItemStack star, PoseStack poseStack, MultiBufferSource buffers, float time) {
        if (star.isEmpty()) return;
        int color = star.is(CrystalnexusModItems.BLUE_STAR.get()) ? 0x62C8FF
            : star.is(CrystalnexusModItems.PINK_STAR.get()) ? 0xFF62D8
            : star.is(CrystalnexusModItems.ORANGE_STAR.get()) ? 0xFF8A32 : 0xFFE06A;
        float pulse = 1.0F + Mth.sin(time * 0.16F) * 0.08F;
        float outer = 2.96F * pulse;

        poseStack.pushPose();
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        VertexConsumer consumer = buffers.getBuffer(GLOW_RENDER_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        for (int segment = 0; segment < HALO_SEGMENTS; segment++) {
            float angle0 = Mth.TWO_PI * segment / HALO_SEGMENTS;
            float angle1 = Mth.TWO_PI * (segment + 1) / HALO_SEGMENTS;
            haloVertex(consumer, matrix, 0.0F, 0.0F, color, 185);
            haloVertex(consumer, matrix, outer, angle0, color, 0);
            haloVertex(consumer, matrix, outer, angle1, color, 0);
        }
        poseStack.popPose();
    }

    private static void haloVertex(VertexConsumer consumer, Matrix4f matrix, float radius, float angle,
                                   int color, int alpha) {
        consumer.addVertex(matrix, Mth.cos(angle) * radius, Mth.sin(angle) * radius, 0.02F)
            .setColor(color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
    }

    private static void drawVoidSphere(Matrix4f matrix, VertexConsumer consumer, float radius, Vec3 cameraOffset) {
        for (int stack = 0; stack < STACKS; stack++) {
            float phi0 = -Mth.HALF_PI + Mth.PI * stack / STACKS;
            float phi1 = -Mth.HALF_PI + Mth.PI * (stack + 1) / STACKS;
            for (int slice = 0; slice < SLICES; slice++) {
                float theta0 = Mth.TWO_PI * slice / SLICES;
                float theta1 = Mth.TWO_PI * (slice + 1) / SLICES;
                float middlePhi = (phi0 + phi1) * 0.5F;
                float middleTheta = (theta0 + theta1) * 0.5F;
                float middleCosPhi = Mth.cos(middlePhi);
                double facing = Mth.cos(middleTheta) * middleCosPhi * cameraOffset.x
                    + Mth.sin(middlePhi) * cameraOffset.y
                    + Mth.sin(middleTheta) * middleCosPhi * cameraOffset.z - radius;
                if (facing >= 0.0D) continue;

                vertex(consumer, matrix, radius, phi0, theta0);
                vertex(consumer, matrix, radius, phi1, theta0);
                vertex(consumer, matrix, radius, phi1, theta1);
                vertex(consumer, matrix, radius, phi0, theta1);
            }
        }
    }

    private static void drawBackgroundStars(Matrix4f matrix, VertexConsumer consumer, float radius,
                                            Vec3 cameraOffset) {
        for (float[] star : BACKGROUND_STARS) {
            float phi = star[0];
            float theta = star[1];
            float cosPhi = Mth.cos(phi);
            double facing = Mth.cos(theta) * cosPhi * cameraOffset.x + Mth.sin(phi) * cameraOffset.y
                + Mth.sin(theta) * cosPhi * cameraOffset.z - radius;
            if (facing >= 0.0D) continue;

            float size = star[2];
            int brightness = (int) star[3];
            starVertex(consumer, matrix, radius, phi - size, theta - size, brightness);
            starVertex(consumer, matrix, radius, phi + size, theta - size, brightness);
            starVertex(consumer, matrix, radius, phi + size, theta + size, brightness);
            starVertex(consumer, matrix, radius, phi - size, theta + size, brightness);
        }
    }

    private static float[][] createBackgroundStars() {
        Random random = new Random(0x534F4C41524CL);
        float[][] stars = new float[110][4];
        for (float[] star : stars) {
            star[0] = (float) Math.asin(random.nextDouble() * 2.0D - 1.0D);
            star[1] = random.nextFloat() * Mth.TWO_PI;
            star[2] = 0.014F + random.nextFloat() * 0.024F;
            star[3] = 175 + random.nextInt(81);
        }
        return stars;
    }

    private static void starVertex(VertexConsumer consumer, Matrix4f matrix, float radius, float phi,
                                   float theta, int brightness) {
        float cosPhi = Mth.cos(phi);
        consumer.addVertex(matrix, Mth.cos(theta) * cosPhi * radius, Mth.sin(phi) * radius,
            Mth.sin(theta) * cosPhi * radius).setColor(brightness, brightness, 255, 255);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float radius, float phi, float theta) {
        float cosPhi = Mth.cos(phi);
        consumer.addVertex(matrix, Mth.cos(theta) * cosPhi * radius, Mth.sin(phi) * radius,
            Mth.sin(theta) * cosPhi * radius).setColor(0, 0, 2, 255);
    }
}
