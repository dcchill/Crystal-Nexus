package net.crystalnexus.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import net.crystalnexus.world.inventory.BlueprintGUIMenu;
import net.crystalnexus.init.CrystalnexusModScreens;
import net.crystalnexus.network.BlueprintClearMessage;
import net.crystalnexus.network.BlueprintSaveMessage;
import net.crystalnexus.schematic.BlueprintSchematicManager;
import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BlueprintGUIScreen extends AbstractContainerScreen<BlueprintGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private static final int PREVIEW_X = 8;
	private static final int PREVIEW_Y = 18;
	private static final int PREVIEW_SIZE = 60;
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final float PREVIEW_PAD_X = 4.0f;
	private static final float PREVIEW_PAD_TOP = 4.0f;
	private static final float PREVIEW_PAD_BOTTOM = 4.0f;

	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	EditBox name;
	Button button_save;
	Button button_clear;
	private BlueprintPreviewData previewData = BlueprintPreviewData.error("No blueprint volume");
	private long lastPreviewRefreshGameTime = Long.MIN_VALUE;

	public BlueprintGUIScreen(BlueprintGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		if (elementType == 0 && elementState instanceof String stringState) {
			if (name.equals("name"))
				this.name.setValue(stringState);
		}
		menuStateUpdateActive = false;
	}

	private static final ResourceLocation texture = ResourceLocation.parse("crystalnexus:textures/screens/blueprint_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		name.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), this.leftPos + 50, this.topPos + -15, 0, 0, 126, 18, 126, 18);
		renderBlueprintPreview(guiGraphics, partialTicks);
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		if (name.isFocused())
			return name.keyPressed(key, b, c);
		return super.keyPressed(key, b, c);
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		String nameValue = name.getValue();
		super.resize(minecraft, width, height);
		name.setValue(nameValue);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.blueprint_gui.label_blueprint_controller"), 55, -11, -12829636, false);
		guiGraphics.drawString(this.font, Component.literal("Preview"), 10, 8, 0x7FE7FF, false);
	}

	@Override
	public void init() {
		super.init();
		name = new EditBox(this.font, this.leftPos + 78, this.topPos + 30, 92, 18, Component.translatable("gui.crystalnexus.blueprint_gui.name"));
		name.setMaxLength(8192);
		name.setResponder(content -> {
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 0, "name", content, false);
		});
		name.setHint(Component.translatable("gui.crystalnexus.blueprint_gui.name"));
		this.addWidget(this.name);
		button_save = Button.builder(Component.translatable("gui.crystalnexus.blueprint_gui.button_save"), e -> {
			PacketDistributor.sendToServer(new BlueprintSaveMessage(new BlockPos(this.x, this.y, this.z), name.getValue()));
		}).bounds(this.leftPos + 79, this.topPos + 55, 40, 20).build();
		this.addRenderableWidget(button_save);
		button_clear = Button.builder(Component.translatable("gui.crystalnexus.blueprint_gui.button_clear"), e -> {
			PacketDistributor.sendToServer(new BlueprintClearMessage(new BlockPos(this.x, this.y, this.z)));
		}).bounds(this.leftPos + 124, this.topPos + 55, 46, 20).build();
		this.addRenderableWidget(button_clear);
	}

	private void renderBlueprintPreview(GuiGraphics guiGraphics, float partialTicks) {
		int panelX = this.leftPos + PREVIEW_X;
		int panelY = this.topPos + PREVIEW_Y;
		guiGraphics.fill(panelX - 1, panelY - 1, panelX + PREVIEW_SIZE + 1, panelY + PREVIEW_SIZE + 1, 0xFF335D6E);
		guiGraphics.fill(panelX, panelY, panelX + PREVIEW_SIZE, panelY + PREVIEW_SIZE, 0xCC0A1117);

		refreshPreviewData();
		if (!previewData.valid()) {
			guiGraphics.drawCenteredString(this.font, Component.literal("Preview unavailable"), panelX + PREVIEW_SIZE / 2, panelY + 24, 0xE46A6A);
			guiGraphics.drawCenteredString(this.font, Component.literal(previewData.error()), panelX + PREVIEW_SIZE / 2, panelY + 36, 0xA0A0A0);
			return;
		}
		if (previewData.blocks().isEmpty()) {
			guiGraphics.drawCenteredString(this.font, Component.literal("Interior empty"), panelX + PREVIEW_SIZE / 2, panelY + 30, 0xB8B8B8);
			return;
		}

		PoseStack pose = guiGraphics.pose();
		RenderSystem.enableDepthTest();
		Lighting.setupFor3DItems();
		pose.pushPose();
		float yaw = ((this.entity == null ? this.world.getGameTime() : this.entity.tickCount) + partialTicks) * 1.2f - 35.0f;
		Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(17.0f)).rotateY((float) Math.toRadians(yaw));
		PreviewTransform transform = computePreviewTransform(previewData, rotation);
		pose.translate(panelX + PREVIEW_SIZE / 2.0f + transform.offsetX(), panelY + PREVIEW_SIZE / 2.0f + transform.offsetY(), 200.0f);
		pose.scale(transform.scale(), -transform.scale(), transform.scale());
		pose.mulPose(rotation);
		pose.translate(-previewData.centerX(), -previewData.centerY(), -previewData.centerZ());

		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		for (PreviewBlock block : previewData.blocks()) {
			pose.pushPose();
			pose.translate(block.pos().getX(), block.pos().getY(), block.pos().getZ());
			Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.state(), pose, bufferSource, FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			pose.popPose();
		}
		renderBounds(pose, bufferSource, previewData.sizeX(), previewData.sizeY(), previewData.sizeZ());
		bufferSource.endBatch();
		pose.popPose();
		Lighting.setupForFlatItems();
		RenderSystem.disableDepthTest();
	}

	private static PreviewTransform computePreviewTransform(BlueprintPreviewData previewData, Quaternionf rotation) {
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;

		for (int ox = 0; ox <= 1; ox++) {
			for (int oy = 0; oy <= 1; oy++) {
				for (int oz = 0; oz <= 1; oz++) {
					Vector3f point = new Vector3f(
							ox * previewData.sizeX() - previewData.centerX(),
							oy * previewData.sizeY() - previewData.centerY(),
							oz * previewData.sizeZ() - previewData.centerZ()
					);
					point.rotate(rotation);
					minX = Math.min(minX, point.x);
					maxX = Math.max(maxX, point.x);
					minY = Math.min(minY, point.y);
					maxY = Math.max(maxY, point.y);
				}
			}
		}

		float availableWidth = PREVIEW_SIZE - PREVIEW_PAD_X * 2.0f;
		float availableHeight = PREVIEW_SIZE - PREVIEW_PAD_TOP - PREVIEW_PAD_BOTTOM;
		float spanX = Math.max(maxX - minX, 0.001f);
		float spanY = Math.max(maxY - minY, 0.001f);
		float scale = Math.min(availableWidth / spanX, availableHeight / spanY);
		float offsetX = -((minX + maxX) * 0.5f) * scale;
		float offsetY = ((minY + maxY) * 0.5f) * scale;
		return new PreviewTransform(scale, offsetX, offsetY);
	}

	private void refreshPreviewData() {
		if (this.world == null) {
			this.previewData = BlueprintPreviewData.error("No world");
			return;
		}

		long gameTime = this.world.getGameTime();
		if (gameTime == this.lastPreviewRefreshGameTime) {
			return;
		}
		this.lastPreviewRefreshGameTime = gameTime;

		BlueprintSchematicManager.BlueprintVolume volume = BlueprintSchematicManager.findVolume(this.world, new BlockPos(this.x, this.y, this.z));
		if (!volume.isValid()) {
			this.previewData = BlueprintPreviewData.error(volume.error());
			return;
		}

		List<PreviewBlock> blocks = new ArrayList<>();
		for (BlockPos worldPos : BlockPos.betweenClosed(volume.minX(), volume.minY(), volume.minZ(), volume.maxX(), volume.maxY(), volume.maxZ())) {
			BlockState state = this.world.getBlockState(worldPos);
			if (state.isAir() || state.is(Blocks.STRUCTURE_VOID)) {
				continue;
			}
			blocks.add(new PreviewBlock(new BlockPos(worldPos.getX() - volume.minX(), worldPos.getY() - volume.minY(), worldPos.getZ() - volume.minZ()), state));
		}

		blocks.sort(Comparator
				.comparingInt((PreviewBlock block) -> block.pos().getY())
				.thenComparingInt(block -> block.pos().getZ())
				.thenComparingInt(block -> block.pos().getX()));

		int sizeX = volume.maxX() - volume.minX() + 1;
		int sizeY = volume.maxY() - volume.minY() + 1;
		int sizeZ = volume.maxZ() - volume.minZ() + 1;
		this.previewData = new BlueprintPreviewData(
				blocks,
				sizeX,
				sizeY,
				sizeZ,
				sizeX / 2.0f,
				sizeY / 2.0f,
				sizeZ / 2.0f,
				null
		);
	}

	private static void renderBounds(PoseStack pose, MultiBufferSource bufferSource, int sizeX, int sizeY, int sizeZ) {
		VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
		addLineBox(lines, pose.last(), 0.0f, 0.0f, 0.0f, sizeX, sizeY, sizeZ, 0.24f, 0.84f, 1.0f, 0.9f);
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

	private record PreviewBlock(BlockPos pos, BlockState state) {
	}

	private record PreviewTransform(float scale, float offsetX, float offsetY) {
	}

	private record BlueprintPreviewData(List<PreviewBlock> blocks, int sizeX, int sizeY, int sizeZ, float centerX, float centerY, float centerZ, String error) {
		private boolean valid() {
			return error == null;
		}

		private static BlueprintPreviewData error(String message) {
			return new BlueprintPreviewData(List.of(), 1, 1, 1, 0.5f, 0.5f, 0.5f, message);
		}
	}
}
