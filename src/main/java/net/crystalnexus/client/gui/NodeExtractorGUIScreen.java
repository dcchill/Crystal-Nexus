package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.NodeExtractorGUIMenu;
import net.crystalnexus.procedures.ProgressDisplayProcedure;
import net.crystalnexus.procedures.FluidDisplayProcedure;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.init.CrystalnexusModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class NodeExtractorGUIScreen extends AbstractContainerScreen<NodeExtractorGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public NodeExtractorGUIScreen(NodeExtractorGUIMenu container, Inventory inventory, Component text) {
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
		menuStateUpdateActive = false;
	}

	private static final ResourceLocation texture = ResourceLocation.parse("crystalnexus:textures/screens/node_extractor_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/upgradeslot.png"), this.leftPos + 173, this.topPos + 0, 0, 0, 32, 32, 32, 32);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), this.leftPos + 50, this.topPos + -15, 0, 0, 126, 18, 126, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/batterylevels.png"), this.leftPos + 6, this.topPos + 12, 0, Mth.clamp((int) EnergyDisplayProcedure.execute(world, x, y, z) * 64, 0, 640), 64, 64, 64, 704);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/progressbar.png"), this.leftPos + 72, this.topPos + 27, 0, Mth.clamp((int) ProgressDisplayProcedure.execute(world, x, y, z) * 32, 0, 320), 32, 32, 32, 352);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/fluidlevels.png"), this.leftPos + 105, this.topPos + 12, 0, Mth.clamp((int) FluidDisplayProcedure.execute(world, x, y, z) * 64, 0, 640), 64, 64, 64, 704);
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.node_extractor_gui.label_quantum_miner"), 72, -10, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
	}
}