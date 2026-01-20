package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.crystalnexus.block.entity.TankBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import org.joml.Matrix4f;

import java.util.*;

public class TankBER implements BlockEntityRenderer<TankBlockEntity> {

    public TankBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(TankBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {

        Level level = be.getLevel();
        if (level == null) return;

        TankBlockEntity controller = be.getController();
        FluidStack fluid = controller.getTank().getFluid();
        int totalAmount = controller.getTank().getFluidAmount();
        if (fluid.isEmpty() || totalAmount <= 0) return;

        // Bottom-up member ordering (client)
        List<BlockPos> members = collectComponent(level, controller.getBlockPos());
        members.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getZ));

        int index = members.indexOf(be.getBlockPos());
        if (index < 0) return;

        int per = TankBlockEntity.PER_BLOCK_CAPACITY;
        int start = index * per;
        int blockAmount = Math.max(0, Math.min(per, totalAmount - start));
        if (blockAmount <= 0) return;

        float fill = blockAmount / (float) per; // 0..1

        // Use STILL texture everywhere (reliable; no random atlas garbage)
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation stillTex = ext.getStillTexture(fluid);
        if (stillTex == null) return;

        TextureAtlasSprite still = Minecraft.getInstance()
                .getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                .apply(stillTex);

        int argb = ext.getTintColor(fluid);
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ((argb      ) & 0xFF) / 255f;

        // Inner bounds
        float inset = 1f / 16f;
        float min = inset;
        float max = 1f - inset;

        float y0 = inset;
        float y1 = inset + (max - inset) * fill;

        // Epsilon so the top plane doesn't z-fight
        float topY = y1 - 0.0005f;
        float topInset = inset + 0.001f;

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();

        VertexConsumer vc = buffers.getBuffer(Sheets.translucentCullBlockSheet());

        // Sides (still texture, cropped V so it fills upward)
        drawNorth(vc, mat, min, y0, min,  max, y1, min,  still, r, g, b, a, packedLight, packedOverlay, fill);
        drawSouth(vc, mat, min, y0, max,  max, y1, max,  still, r, g, b, a, packedLight, packedOverlay, fill);
        drawWest (vc, mat, min, y0, min,  min, y1, max,  still, r, g, b, a, packedLight, packedOverlay, fill);
        drawEast (vc, mat, max, y0, min,  max, y1, max,  still, r, g, b, a, packedLight, packedOverlay, fill);

        // Top surface
        if (fill > 0.001f) {
            drawTop(vc, mat, topInset, topY, topInset,  1f - topInset, topY, 1f - topInset,
                    still, r, g, b, a, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    private static List<BlockPos> collectComponent(Level level, BlockPos start) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        HashSet<BlockPos> out = new HashSet<>();
        q.add(start);
        out.add(start);

        while (!q.isEmpty()) {
            BlockPos p = q.removeFirst();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (out.contains(n)) continue;
                if (level.getBlockEntity(n) instanceof TankBlockEntity) {
                    out.add(n);
                    q.add(n);
                }
            }
        }
        return new ArrayList<>(out);
    }

    // ---- Vertex helper (1.21 format: UV2 + Normal required) ----
    private static void v(VertexConsumer vc, Matrix4f mat,
                          float x, float y, float z,
                          float r, float g, float b, float a,
                          float u, float v,
                          int light, int overlay,
                          float nx, float ny, float nz) {

        vc.addVertex(mat, x, y, z)
          .setColor(r, g, b, a)
          .setUv(u, v)
          .setOverlay(overlay)
          .setUv2(light & 0xFFFF, (light >> 16) & 0xFFFF)
          .setNormal(nx, ny, nz);
    }

    // ---- Faces (cropped V: reveal from bottom upward, no stretching) ----
    private static void drawNorth(VertexConsumer vc, Matrix4f mat,
                                  float x0, float y0, float z,
                                  float x1, float y1, float z1,
                                  TextureAtlasSprite s,
                                  float r, float g, float b, float a,
                                  int light, int overlay,
                                  float fill) {
        float u0 = s.getU0(), u1 = s.getU1();
        float v0 = s.getV0(), v1 = s.getV1();
        float vFill = v1 - (v1 - v0) * fill;

        // normal -Z
        v(vc, mat, x0, y0, z, r,g,b,a, u0, v1,    light, overlay, 0, 0, -1);
        v(vc, mat, x0, y1, z, r,g,b,a, u0, vFill, light, overlay, 0, 0, -1);
        v(vc, mat, x1, y1, z, r,g,b,a, u1, vFill, light, overlay, 0, 0, -1);
        v(vc, mat, x1, y0, z, r,g,b,a, u1, v1,    light, overlay, 0, 0, -1);
    }

    private static void drawSouth(VertexConsumer vc, Matrix4f mat,
                                  float x0, float y0, float z,
                                  float x1, float y1, float z1,
                                  TextureAtlasSprite s,
                                  float r, float g, float b, float a,
                                  int light, int overlay,
                                  float fill) {
        float u0 = s.getU0(), u1 = s.getU1();
        float v0 = s.getV0(), v1 = s.getV1();
        float vFill = v1 - (v1 - v0) * fill;

        // normal +Z
        v(vc, mat, x1, y0, z, r,g,b,a, u0, v1,    light, overlay, 0, 0,  1);
        v(vc, mat, x1, y1, z, r,g,b,a, u0, vFill, light, overlay, 0, 0,  1);
        v(vc, mat, x0, y1, z, r,g,b,a, u1, vFill, light, overlay, 0, 0,  1);
        v(vc, mat, x0, y0, z, r,g,b,a, u1, v1,    light, overlay, 0, 0,  1);
    }

    private static void drawWest(VertexConsumer vc, Matrix4f mat,
                                 float x, float y0, float z0,
                                 float x1, float y1, float z1,
                                 TextureAtlasSprite s,
                                 float r, float g, float b, float a,
                                 int light, int overlay,
                                 float fill) {
        float u0 = s.getU0(), u1 = s.getU1();
        float v0 = s.getV0(), v1 = s.getV1();
        float vFill = v1 - (v1 - v0) * fill;

        // normal -X
        v(vc, mat, x, y0, z1, r,g,b,a, u0, v1,    light, overlay, -1, 0, 0);
        v(vc, mat, x, y1, z1, r,g,b,a, u0, vFill, light, overlay, -1, 0, 0);
        v(vc, mat, x, y1, z0, r,g,b,a, u1, vFill, light, overlay, -1, 0, 0);
        v(vc, mat, x, y0, z0, r,g,b,a, u1, v1,    light, overlay, -1, 0, 0);
    }

    private static void drawEast(VertexConsumer vc, Matrix4f mat,
                                 float x, float y0, float z0,
                                 float x1, float y1, float z1,
                                 TextureAtlasSprite s,
                                 float r, float g, float b, float a,
                                 int light, int overlay,
                                 float fill) {
        float u0 = s.getU0(), u1 = s.getU1();
        float v0 = s.getV0(), v1 = s.getV1();
        float vFill = v1 - (v1 - v0) * fill;

        // normal +X
        v(vc, mat, x, y0, z0, r,g,b,a, u0, v1,    light, overlay,  1, 0, 0);
        v(vc, mat, x, y1, z0, r,g,b,a, u0, vFill, light, overlay,  1, 0, 0);
        v(vc, mat, x, y1, z1, r,g,b,a, u1, vFill, light, overlay,  1, 0, 0);
        v(vc, mat, x, y0, z1, r,g,b,a, u1, v1,    light, overlay,  1, 0, 0);
    }

private static void drawTop(VertexConsumer vc, Matrix4f mat,
                            float x0, float y, float z0,
                            float x1, float y1, float z1,
                            TextureAtlasSprite s,
                            float r, float g, float b, float a,
                            int light, int overlay) {
    float u0 = s.getU0(), u1 = s.getU1();
    float v0 = s.getV0(), v1 = s.getV1();

    // CCW when viewed from above (+Y) so it won't get culled
    v(vc, mat, x0, y, z0, r,g,b,a, u0, v0, light, overlay, 0, 1, 0);
    v(vc, mat, x0, y, z1, r,g,b,a, u0, v1, light, overlay, 0, 1, 0);
    v(vc, mat, x1, y, z1, r,g,b,a, u1, v1, light, overlay, 0, 1, 0);
    v(vc, mat, x1, y, z0, r,g,b,a, u1, v0, light, overlay, 0, 1, 0);
}
}
