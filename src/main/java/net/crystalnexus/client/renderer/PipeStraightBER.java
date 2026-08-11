package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.crystalnexus.block.PipeStraightBlock;
import net.crystalnexus.block.entity.PipeStraightBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public class PipeStraightBER implements BlockEntityRenderer<PipeStraightBlockEntity> {
    public PipeStraightBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PipeStraightBlockEntity pipe, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        VertexConsumer vertices = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        VertexConsumer highlights = buffers.getBuffer(RenderType.debugStructureQuads());
        Matrix4f matrix = poses.last().pose();
        BlockState state = pipe.getBlockState();
        for (Direction direction : Direction.values()) {
            if (!state.getValue(PipeStraightBlock.property(direction))) continue;
            if (pipe.isInputSide(direction)) {
                drawArm(highlights, matrix, direction, 0.15f, 0.45f, 1.0f);
            } else if (pipe.isOutputSide(direction)) {
                drawArm(highlights, matrix, direction, 1.0f, 0.15f, 0.15f);
            }
        }

        FluidStack fluid = pipe.getFluidTank().getFluid();
        if (fluid.isEmpty()) return;

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation texture = extensions.getStillTexture(fluid);
        if (texture == null) return;
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        int color = extensions.getTintColor(fluid);
        float alpha = ((color >>> 24) & 255) / 255.0f;
        float red = ((color >>> 16) & 255) / 255.0f;
        float green = ((color >>> 8) & 255) / 255.0f;
        float blue = (color & 255) / 255.0f;
        if (alpha == 0) alpha = 1.0f;

        float innerMin = 5.1f / 16.0f;
        float innerMax = 10.9f / 16.0f;
        drawBox(vertices, matrix, innerMin, innerMin, innerMin, innerMax, innerMax, innerMax,
            sprite, red, green, blue, alpha * 0.82f, packedLight, packedOverlay);

        for (Direction direction : Direction.values()) {
            if (!state.getValue(PipeStraightBlock.property(direction))) continue;
            switch (direction) {
                case NORTH -> drawBox(vertices, matrix, innerMin, innerMin, 0.0f, innerMax, innerMax, innerMin,
                    sprite, red, green, blue, alpha * 0.82f, packedLight, packedOverlay);
                case SOUTH -> drawBox(vertices, matrix, innerMin, innerMin, innerMax, innerMax, innerMax, 1.0f,
                    sprite, red, green, blue, alpha * 0.82f, packedLight, packedOverlay);
                case WEST -> drawBox(vertices, matrix, 0.0f, innerMin, innerMin, innerMin, innerMax, innerMax,
                    sprite, red, green, blue, alpha * 0.82f, packedLight, packedOverlay);
                case EAST -> drawBox(vertices, matrix, innerMax, innerMin, innerMin, 1.0f, innerMax, innerMax,
                    sprite, red, green, blue, alpha * 0.82f, packedLight, packedOverlay);
                case DOWN -> drawBox(vertices, matrix, innerMin, 0.0f, innerMin, innerMax, innerMin, innerMax,
                    sprite, red, green, blue, alpha * 0.82f, packedLight, packedOverlay);
                case UP -> drawBox(vertices, matrix, innerMin, innerMax, innerMin, innerMax, 1.0f, innerMax,
                    sprite, red, green, blue, alpha * 0.82f, packedLight, packedOverlay);
            }
        }
    }

    private static void drawArm(VertexConsumer vertices, Matrix4f matrix, Direction direction,
                                float red, float green, float blue) {
        float min = 4.0f / 16.0f;
        float max = 12.0f / 16.0f;
        float jointMin = 5.0f / 16.0f;
        float jointMax = 11.0f / 16.0f;
        switch (direction) {
            case NORTH -> drawColoredBox(vertices, matrix, min, min, -0.01f, max, max, jointMin, red, green, blue);
            case SOUTH -> drawColoredBox(vertices, matrix, min, min, jointMax, max, max, 1.01f, red, green, blue);
            case WEST -> drawColoredBox(vertices, matrix, -0.01f, min, min, jointMin, max, max, red, green, blue);
            case EAST -> drawColoredBox(vertices, matrix, jointMax, min, min, 1.01f, max, max, red, green, blue);
            case DOWN -> drawColoredBox(vertices, matrix, min, -0.01f, min, max, jointMin, max, red, green, blue);
            case UP -> drawColoredBox(vertices, matrix, min, jointMax, min, max, 1.01f, max, red, green, blue);
        }
    }

    private static void drawColoredBox(VertexConsumer vertex, Matrix4f matrix,
                                       float x0, float y0, float z0, float x1, float y1, float z1,
                                       float red, float green, float blue) {
        coloredQuad(vertex, matrix, red, green, blue, x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0);
        coloredQuad(vertex, matrix, red, green, blue, x1,y0,z1, x1,y1,z1, x0,y1,z1, x0,y0,z1);
        coloredQuad(vertex, matrix, red, green, blue, x0,y0,z1, x0,y1,z1, x0,y1,z0, x0,y0,z0);
        coloredQuad(vertex, matrix, red, green, blue, x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1);
        coloredQuad(vertex, matrix, red, green, blue, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1);
        coloredQuad(vertex, matrix, red, green, blue, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0);
    }

    private static void coloredQuad(VertexConsumer vertex, Matrix4f matrix,
                                    float red, float green, float blue,
                                    float x0,float y0,float z0, float x1,float y1,float z1,
                                    float x2,float y2,float z2, float x3,float y3,float z3) {
        vertex.addVertex(matrix, x0, y0, z0).setColor(red, green, blue, 0.3f);
        vertex.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, 0.3f);
        vertex.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, 0.3f);
        vertex.addVertex(matrix, x3, y3, z3).setColor(red, green, blue, 0.3f);
    }

    private static void drawBox(VertexConsumer vertex, Matrix4f matrix,
                                float x0, float y0, float z0, float x1, float y1, float z1,
                                TextureAtlasSprite sprite, float red, float green, float blue, float alpha,
                                int light, int overlay) {
        quad(vertex, matrix, sprite, red, green, blue, alpha, light, overlay,
            x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0, 0,0,-1);
        quad(vertex, matrix, sprite, red, green, blue, alpha, light, overlay,
            x1,y0,z1, x1,y1,z1, x0,y1,z1, x0,y0,z1, 0,0,1);
        quad(vertex, matrix, sprite, red, green, blue, alpha, light, overlay,
            x0,y0,z1, x0,y1,z1, x0,y1,z0, x0,y0,z0, -1,0,0);
        quad(vertex, matrix, sprite, red, green, blue, alpha, light, overlay,
            x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1, 1,0,0);
        quad(vertex, matrix, sprite, red, green, blue, alpha, light, overlay,
            x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, 0,-1,0);
        quad(vertex, matrix, sprite, red, green, blue, alpha, light, overlay,
            x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, 0,1,0);
    }

    private static void quad(VertexConsumer vertex, Matrix4f matrix, TextureAtlasSprite sprite,
                             float red, float green, float blue, float alpha, int light, int overlay,
                             float x0,float y0,float z0, float x1,float y1,float z1,
                             float x2,float y2,float z2, float x3,float y3,float z3,
                             float nx,float ny,float nz) {
        float u0 = sprite == null ? 0.0f : sprite.getU0();
        float v0 = sprite == null ? 0.0f : sprite.getV0();
        float u1 = sprite == null ? 0.0f : sprite.getU1();
        float v1 = sprite == null ? 0.0f : sprite.getV1();
        point(vertex,matrix,x0,y0,z0,red,green,blue,alpha,u0,v1,light,overlay,nx,ny,nz);
        point(vertex,matrix,x1,y1,z1,red,green,blue,alpha,u0,v0,light,overlay,nx,ny,nz);
        point(vertex,matrix,x2,y2,z2,red,green,blue,alpha,u1,v0,light,overlay,nx,ny,nz);
        point(vertex,matrix,x3,y3,z3,red,green,blue,alpha,u1,v1,light,overlay,nx,ny,nz);
    }

    private static void point(VertexConsumer vertex, Matrix4f matrix, float x,float y,float z,
                              float red,float green,float blue,float alpha, float u,float v,
                              int light,int overlay, float nx,float ny,float nz) {
        vertex.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha).setUv(u, v)
            .setOverlay(overlay).setUv2(light & 65535, light >>> 16 & 65535).setNormal(nx, ny, nz);
    }
}
