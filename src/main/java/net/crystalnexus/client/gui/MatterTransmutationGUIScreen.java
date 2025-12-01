package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.MatterTransmutationGUIMenu;
import net.crystalnexus.procedures.ProgressDisplayProcedure;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.init.CrystalnexusModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class MatterTransmutationGUIScreen extends AbstractContainerScreen<MatterTransmutationGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public MatterTransmutationGUIScreen(MatterTransmutationGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 190;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		menuStateUpdateActive = false;
	}

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
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/matter_transmutation_gui_color.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 190, 176, 190);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/battery_addon.png"), this.leftPos + -33, this.topPos + -1, 0, 0, 48, 48, 48, 48);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/progressbarvert.png"), this.leftPos + 7, this.topPos + 41, 0, Mth.clamp((int) ProgressDisplayProcedure.execute(world, x, y, z) * 32, 0, 320), 32, 32, 32, 352);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/batterylevelsmall.png"), this.leftPos + -25, this.topPos + 5, 0, Mth.clamp((int) EnergyDisplayProcedure.execute(world, x, y, z) * 32, 0, 320), 32, 32, 32, 352);
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
	}

	@Override
	public void init() {
		super.init();
	}
}