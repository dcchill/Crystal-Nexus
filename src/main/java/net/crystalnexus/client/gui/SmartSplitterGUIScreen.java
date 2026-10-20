package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.SmartSplitterGUIMenu;
import net.crystalnexus.init.CrystalnexusModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class SmartSplitterGUIScreen extends HolographicMachineScreen<SmartSplitterGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
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


	@Override
	protected void renderMachine(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.renderMachine(guiGraphics, mouseX, mouseY, partialTicks);
        boolean customTooltipShown = false;
		if (mouseX > leftPos + 0 && mouseX < leftPos + 24 && mouseY > topPos + 0 && mouseY < topPos + 24) {
			guiGraphics.renderTooltip(font, Component.translatable("gui.crystalnexus.smart_splitter_gui.tooltip_empty_slots_no_filter"), mouseX, mouseY);
			customTooltipShown = true;
		}
		if (MachineGuiWidgets.slotFrame(menu, SmartSplitterGUIMenu.OVERFLOW_SLOT, leftPos, topPos).contains(mouseX, mouseY)) {
			guiGraphics.renderTooltip(font, Component.translatable("gui.crystalnexus.smart_splitter_gui.tooltip_overflow"), mouseX, mouseY);
			customTooltipShown = true;
		}
		if (MachineGuiWidgets.slotFrame(menu, SmartSplitterGUIMenu.INPUT_SLOT, leftPos, topPos).contains(mouseX, mouseY)) {
			guiGraphics.renderTooltip(font, Component.translatable("gui.crystalnexus.smart_splitter_gui.tooltip_input"), mouseX, mouseY);
			customTooltipShown = true;
		}
			if (!customTooltipShown) this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		machineBackground(guiGraphics);
		// Slot 0 is the input (green); slots 1-3 are the left/forward/right filters with their direction icons.
		String[] colors = {"green.png", "red.png", "blue.png", "yellow.png"};
		String[] directions = {null, "arrow_left.png", "arrow_forward.png", "arrow_right.png"};
		for (int i = 0; i < colors.length; i++) {
			var frame = MachineGuiWidgets.slotFrame(menu, i, leftPos, topPos);
			MachineGuiWidgets.overlay(guiGraphics, ResourceLocation.parse("crystalnexus:textures/screens/" + colors[i]), frame);
			if (directions[i] != null)
				MachineGuiWidgets.overlay(guiGraphics, ResourceLocation.parse("crystalnexus:textures/screens/" + directions[i]), frame);
		}

		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tooltip.png"), this.leftPos + 4, this.topPos + 4, 0, 0, 16, 16, 16, 16);
		RenderSystem.disableBlend();

        MachineGuiWidgets.finishBackground(guiGraphics);
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
	}

	@Override
	public void init() {
		super.init();
	}
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "smart_splitter"; }
}
