package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.SmartSplitterGUIMenu;
import net.crystalnexus.init.CrystalnexusModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class SmartSplitterGUIScreen extends AbstractContainerScreen<SmartSplitterGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public SmartSplitterGUIScreen(SmartSplitterGUIMenu container, Inventory inventory, Component text) {
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

	private static final ResourceLocation texture = ResourceLocation.parse("crystalnexus:textures/screens/smart_splitter_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		boolean customTooltipShown = false;
		if (mouseX > leftPos + 0 && mouseX < leftPos + 24 && mouseY > topPos + 0 && mouseY < topPos + 24) {
			guiGraphics.renderTooltip(font, Component.translatable("gui.crystalnexus.smart_splitter_gui.tooltip_empty_slots_no_filter"), mouseX, mouseY);
			customTooltipShown = true;
		}
		if (mouseX > leftPos + 101 && mouseX < leftPos + 125 && mouseY > topPos + 49 && mouseY < topPos + 73) {
			guiGraphics.renderTooltip(font, Component.translatable("gui.crystalnexus.smart_splitter_gui.tooltip_overflow"), mouseX, mouseY);
			customTooltipShown = true;
		}
		if (mouseX > leftPos + 48 && mouseX < leftPos + 72 && mouseY > topPos + 50 && mouseY < topPos + 74) {
			guiGraphics.renderTooltip(font, Component.translatable("gui.crystalnexus.smart_splitter_gui.tooltip_input"), mouseX, mouseY);
			customTooltipShown = true;
		}
		if (!customTooltipShown)
			this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/blue.png"), this.leftPos + 78, this.topPos + 25, 0, 0, 18, 18, 18, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/red.png"), this.leftPos + 24, this.topPos + 25, 0, 0, 18, 18, 18, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/yellow.png"), this.leftPos + 132, this.topPos + 25, 0, 0, 18, 18, 18, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/green.png"), this.leftPos + 51, this.topPos + 52, 0, 0, 18, 18, 18, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/arrow_left.png"), this.leftPos + 24, this.topPos + 25, 0, 0, 18, 18, 18, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/arrow_right.png"), this.leftPos + 132, this.topPos + 25, 0, 0, 18, 18, 18, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/arrow_forward.png"), this.leftPos + 78, this.topPos + 25, 0, 0, 18, 18, 18, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), this.leftPos + 50, this.topPos + -15, 0, 0, 126, 18, 126, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tooltip.png"), this.leftPos + 4, this.topPos + 4, 0, 0, 16, 16, 16, 16);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.smart_splitter_gui.label_smart_splitte"), 72, -11, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
	}
}