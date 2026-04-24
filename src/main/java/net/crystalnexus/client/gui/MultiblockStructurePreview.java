package net.crystalnexus.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.crystalnexus.jei.CrystalnexusJeiRuntimePlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiblockStructurePreview {
    private static final int PREVIEW_X = 6;
    private static final int PREVIEW_Y = 7;
    private static final int PREVIEW_SIZE = 128;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final Map<String, StructurePreviewData> CACHE = new HashMap<>();

    private final String structureId;
    private float yaw = -35.0f;
    private float pitch = 25.0f;
    private int visibleLayer = Integer.MAX_VALUE;
    private boolean dragging;
    private double lastMouseX;
    private double lastMouseY;
    private double clickStartX;
    private double clickStartY;
    private boolean movedSinceClick;
    private PreviewBlock clickedBlock;

    public MultiblockStructurePreview(String structureId) {
        this.structureId = structureId;
    }

    public void render(GuiGraphics guiGraphics, Font font, RegistryAccess registryAccess, int leftPos, int topPos, int mouseX, int mouseY) {
        StructurePreviewData data = getData(registryAccess);
        int x = leftPos + PREVIEW_X;
        int y = topPos + PREVIEW_Y;

        if (data.error != null) {
            guiGraphics.drawCenteredString(font, Component.literal("Preview unavailable"), x + PREVIEW_SIZE / 2, y + 54, 0xD06060);
            guiGraphics.drawCenteredString(font, Component.literal(data.error), x + PREVIEW_SIZE / 2, y + 66, 0x909090);
            return;
        }

        this.visibleLayer = Mth.clamp(this.visibleLayer, 0, data.layerCount - 1);

        PoseStack pose = guiGraphics.pose();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        pose.pushPose();
        PreviewTransform transform = getPreviewTransform(data);
        pose.translate(x + PREVIEW_SIZE / 2.0f, y + transform.originY(), 200.0f);
        Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(this.yaw)).rotateX((float) Math.toRadians(this.pitch));
        pose.scale(transform.scale(), -transform.scale(), transform.scale());
        pose.mulPose(rotation);
        pose.translate(-data.centerX, -data.centerY, -data.centerZ);

        HoveredBlock hoveredBlock = this.dragging ? null : getHoveredBlock(data, leftPos, topPos, mouseX, mouseY);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        for (PreviewBlock block : data.blocks) {
            if (block.pos.getY() > data.minY + this.visibleLayer) {
                continue;
            }

            pose.pushPose();
            pose.translate(block.pos.getX(), block.pos.getY(), block.pos.getZ());
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.state, pose, bufferSource, FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        if (hoveredBlock != null) {
            renderHighlight(pose, bufferSource, hoveredBlock.block.pos);
        }
        bufferSource.endBatch();
        pose.popPose();
        Lighting.setupForFlatItems();
        RenderSystem.disableDepthTest();

        guiGraphics.drawCenteredString(font, Component.literal("Drag rotate"), x + PREVIEW_SIZE / 2, y + 6, 0xB8B8B8);
        guiGraphics.drawCenteredString(font, Component.literal("Scroll layers"), x + PREVIEW_SIZE / 2, y + 16, 0xB8B8B8);
    }

    public void renderHoverTooltip(GuiGraphics guiGraphics, Font font, RegistryAccess registryAccess, int leftPos, int topPos, int mouseX, int mouseY) {
        if (this.dragging || !isInside(mouseX, mouseY, leftPos, topPos)) {
            return;
        }

        HoveredBlock hoveredBlock = getHoveredBlock(getData(registryAccess), leftPos, topPos, mouseX, mouseY);
        if (hoveredBlock == null) {
            return;
        }

        guiGraphics.renderTooltip(font, hoveredBlock.block.state.getBlock().getName(), mouseX + 12, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, RegistryAccess registryAccess, int leftPos, int topPos) {
        if (button != 0 || !isInside(mouseX, mouseY, leftPos, topPos)) {
            return false;
        }

        StructurePreviewData data = getData(registryAccess);
        if (data.error != null) {
            return false;
        }

        HoveredBlock hoveredBlock = getHoveredBlock(data, leftPos, topPos, mouseX, mouseY);
        this.dragging = true;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.clickStartX = mouseX;
        this.clickStartY = mouseY;
        this.movedSinceClick = false;
        this.clickedBlock = hoveredBlock == null ? null : hoveredBlock.block;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!this.dragging || button != 0) {
            return false;
        }

        double deltaX = mouseX - this.lastMouseX;
        double deltaY = mouseY - this.lastMouseY;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        if (!this.movedSinceClick && (Math.abs(mouseX - this.clickStartX) > 3.0 || Math.abs(mouseY - this.clickStartY) > 3.0)) {
            this.movedSinceClick = true;
        }
        this.yaw += (float) deltaX;
        this.pitch = Mth.clamp(this.pitch + (float) deltaY, -70.0f, 70.0f);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (button == 0 && this.dragging && !this.movedSinceClick && this.clickedBlock != null) {
            handled = CrystalnexusJeiRuntimePlugin.showRecipesFor(new ItemStack(this.clickedBlock.state.getBlock()));
        }
        this.dragging = false;
        this.clickedBlock = null;
        this.movedSinceClick = false;
        return handled;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY, RegistryAccess registryAccess, int leftPos, int topPos) {
        if (!isInside(mouseX, mouseY, leftPos, topPos)) {
            return false;
        }

        StructurePreviewData data = getData(registryAccess);
        if (data.error != null) {
            return false;
        }

        int direction = deltaY > 0 ? 1 : -1;
        this.visibleLayer = Mth.clamp(this.visibleLayer + direction, 0, data.layerCount - 1);
        return true;
    }

    private boolean isInside(double mouseX, double mouseY, int leftPos, int topPos) {
        return mouseX >= leftPos + PREVIEW_X && mouseX < leftPos + PREVIEW_X + PREVIEW_SIZE
                && mouseY >= topPos + PREVIEW_Y && mouseY < topPos + PREVIEW_Y + PREVIEW_SIZE;
    }

    private StructurePreviewData getData(RegistryAccess registryAccess) {
        return CACHE.computeIfAbsent(this.structureId, id -> load(id, registryAccess.lookupOrThrow(Registries.BLOCK)));
    }

    private HoveredBlock getHoveredBlock(StructurePreviewData data, int leftPos, int topPos, double mouseX, double mouseY) {
        if (data.error != null) {
            return null;
        }

        PreviewTransform transform = getPreviewTransform(data);
        float scale = transform.scale();
        float originX = leftPos + PREVIEW_X + PREVIEW_SIZE / 2.0f;
        float originY = topPos + PREVIEW_Y + transform.originY();
        Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(this.yaw)).rotateX((float) Math.toRadians(this.pitch));

        HoveredBlock best = null;
        for (PreviewBlock block : data.blocks) {
            if (block.pos.getY() > data.minY + this.visibleLayer) {
                continue;
            }

            float minScreenX = Float.POSITIVE_INFINITY;
            float minScreenY = Float.POSITIVE_INFINITY;
            float maxScreenX = Float.NEGATIVE_INFINITY;
            float maxScreenY = Float.NEGATIVE_INFINITY;
            float frontDepth = Float.NEGATIVE_INFINITY;

            for (int ox = 0; ox <= 1; ox++) {
                for (int oy = 0; oy <= 1; oy++) {
                    for (int oz = 0; oz <= 1; oz++) {
                        Vector3f point = new Vector3f(
                                block.pos.getX() - data.centerX + ox,
                                block.pos.getY() - data.centerY + oy,
                                block.pos.getZ() - data.centerZ + oz
                        );
                        point.rotate(rotation);

                        float screenX = originX + point.x * scale;
                        float screenY = originY - point.y * scale;
                        minScreenX = Math.min(minScreenX, screenX);
                        minScreenY = Math.min(minScreenY, screenY);
                        maxScreenX = Math.max(maxScreenX, screenX);
                        maxScreenY = Math.max(maxScreenY, screenY);
                        frontDepth = Math.max(frontDepth, point.z);
                    }
                }
            }

            if (mouseX < minScreenX || mouseX > maxScreenX || mouseY < minScreenY || mouseY > maxScreenY) {
                continue;
            }

            double centerX = (minScreenX + maxScreenX) * 0.5;
            double centerY = (minScreenY + maxScreenY) * 0.5;
            double distanceSq = Mth.square(mouseX - centerX) + Mth.square(mouseY - centerY);
            if (best == null || frontDepth > best.depth || (frontDepth == best.depth && distanceSq < best.distanceSq)) {
                best = new HoveredBlock(block, frontDepth, distanceSq);
            }
        }
        return best;
    }

    private PreviewTransform getPreviewTransform(StructurePreviewData data) {
        float baseScale = 72.0f / Math.max(Math.max(data.sizeX, data.sizeY), data.sizeZ);
        if ("zero_point".equals(this.structureId)) {
            return new PreviewTransform(baseScale * 1.18f, 96.0f);
        }
        return new PreviewTransform(baseScale, 104.0f);
    }

    private static void renderHighlight(PoseStack pose, MultiBufferSource bufferSource, BlockPos pos) {
        pose.pushPose();
        pose.translate(pos.getX(), pos.getY(), pos.getZ());

        VertexConsumer fill = bufferSource.getBuffer(RenderType.debugFilledBox());
        PoseStack.Pose last = pose.last();
        addFace(fill, last, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.2f, 0.75f, 1.0f, 0.18f, 0, 0, -1);
        addFace(fill, last, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.2f, 0.75f, 1.0f, 0.18f, 0, 0, 1);
        addFace(fill, last, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.2f, 0.75f, 1.0f, 0.18f, -1, 0, 0);
        addFace(fill, last, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.2f, 0.75f, 1.0f, 0.18f, 1, 0, 0);
        addFace(fill, last, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.2f, 0.75f, 1.0f, 0.18f, 0, 1, 0);
        addFace(fill, last, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.2f, 0.75f, 1.0f, 0.18f, 0, -1, 0);

        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        addLineBox(lines, last, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.35f, 0.9f, 1.0f, 0.9f);
        pose.popPose();
    }

    private static void addFace(VertexConsumer vc, PoseStack.Pose pose,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float r, float g, float b, float a,
                                int nx, int ny, int nz) {
        vc.addVertex(pose.pose(), x1, y1, z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose.pose(), x2, y2, z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose.pose(), x3, y3, z3).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose.pose(), x4, y4, z4).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
    }

    private static void addLineBox(VertexConsumer vc, PoseStack.Pose pose,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ,
                                   float r, float g, float b, float a) {
        addLine(vc, pose, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        addLine(vc, pose, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(vc, pose, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
        addLine(vc, pose, minX, maxY, minZ, minX, minY, minZ, r, g, b, a);
        addLine(vc, pose, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, a);
        addLine(vc, pose, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(vc, pose, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addLine(vc, pose, minX, maxY, maxZ, minX, minY, maxZ, r, g, b, a);
        addLine(vc, pose, minX, minY, minZ, minX, minY, maxZ, r, g, b, a);
        addLine(vc, pose, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        addLine(vc, pose, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(vc, pose, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void addLine(VertexConsumer vc, PoseStack.Pose pose,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.0f) {
            nx /= length;
            ny /= length;
            nz /= length;
        }
        vc.addVertex(pose.pose(), x1, y1, z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose.pose(), x2, y2, z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
    }

    private static StructurePreviewData load(String structureId, HolderGetter<Block> blockLookup) {
        String path = "data/crystalnexus/structures/" + structureId + ".nbt";
        try (InputStream input = openPreviewStream(path, structureId)) {
            if (input == null) {
                return StructurePreviewData.error("Missing file");
            }

            CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            ListTag paletteTag = getPaletteTag(root);
            List<BlockState> palette = new ArrayList<>(paletteTag.size());
            for (int i = 0; i < paletteTag.size(); i++) {
                palette.add(NbtUtils.readBlockState(blockLookup, paletteTag.getCompound(i)));
            }

            ListTag blocksTag = root.getList("blocks", Tag.TAG_COMPOUND);
            List<PreviewBlock> blocks = new ArrayList<>(blocksTag.size());
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (int i = 0; i < blocksTag.size(); i++) {
                CompoundTag blockTag = blocksTag.getCompound(i);
                ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
                BlockPos pos = new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2));
                BlockState state = palette.get(blockTag.getInt("state"));
                if (state.isAir() || state.is(Blocks.STRUCTURE_VOID)) {
                    continue;
                }

                blocks.add(new PreviewBlock(pos, state));
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }

            if (blocks.isEmpty()) {
                return StructurePreviewData.error("No blocks");
            }

            blocks.sort(Comparator
                    .comparingInt((PreviewBlock block) -> block.pos.getY())
                    .thenComparingInt(block -> block.pos.getZ())
                    .thenComparingInt(block -> block.pos.getX()));

            return new StructurePreviewData(
                    blocks,
                    maxX - minX + 1,
                    maxY - minY + 1,
                    maxZ - minZ + 1,
                    minY,
                    maxY,
                    maxY - minY + 1,
                    (minX + maxX + 1) / 2.0f,
                    (minY + maxY + 1) / 2.0f,
                    (minZ + maxZ + 1) / 2.0f,
                    null
            );
        } catch (IOException | RuntimeException exception) {
            return StructurePreviewData.error("Load failed");
        }
    }

    private static InputStream openPreviewStream(String resourcePath, String structureId) throws IOException {
        InputStream resource = MultiblockStructurePreview.class.getClassLoader().getResourceAsStream(resourcePath);
        if (resource != null) {
            return resource;
        }

        Path schematicsPath = Path.of("schematics", structureId + ".nbt");
        if (Files.exists(schematicsPath)) {
            return Files.newInputStream(schematicsPath);
        }

        Path runSchematicsPath = Path.of("run", "schematics", structureId + ".nbt");
        if (Files.exists(runSchematicsPath)) {
            return Files.newInputStream(runSchematicsPath);
        }

        return null;
    }

    private static ListTag getPaletteTag(CompoundTag root) {
        if (root.contains("palette", Tag.TAG_LIST)) {
            return root.getList("palette", Tag.TAG_COMPOUND);
        }
        if (root.contains("palettes", Tag.TAG_LIST)) {
            ListTag palettes = root.getList("palettes", Tag.TAG_LIST);
            return palettes.isEmpty() ? new ListTag() : palettes.getList(0);
        }
        return new ListTag();
    }

    private record PreviewBlock(BlockPos pos, BlockState state) {
    }

    private record HoveredBlock(PreviewBlock block, float depth, double distanceSq) {
    }

    private record PreviewTransform(float scale, float originY) {
    }

    private static class StructurePreviewData {
        private final List<PreviewBlock> blocks;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final int minY;
        private final int maxY;
        private final int layerCount;
        private final float centerX;
        private final float centerY;
        private final float centerZ;
        private final String error;

        private StructurePreviewData(List<PreviewBlock> blocks, int sizeX, int sizeY, int sizeZ, int minY, int maxY, int layerCount, float centerX, float centerY, float centerZ, String error) {
            this.blocks = blocks;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.minY = minY;
            this.maxY = maxY;
            this.layerCount = layerCount;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.error = error;
        }

        private static StructurePreviewData error(String message) {
            return new StructurePreviewData(List.of(), 1, 1, 1, 0, 0, 1, 0.0f, 0.0f, 0.0f, message);
        }
    }
}
