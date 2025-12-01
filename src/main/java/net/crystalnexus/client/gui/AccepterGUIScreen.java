package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.AccepterGUIMenu;
import net.crystalnexus.procedures.TankTwoProcedure;
import net.crystalnexus.procedures.TankThreeProcedure;
import net.crystalnexus.procedures.TankOneProcedure;
import net.crystalnexus.procedures.TankNoneProcedure;
import net.crystalnexus.procedures.TankDoneProcedure;
import net.crystalnexus.procedures.CrystalAccepterOnTickUpdateProcedure;
import net.crystalnexus.init.CrystalnexusModScreens;

import java.util.stream.Collectors;
import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;

public class AccepterGUIScreen extends AbstractContainerScreen<AccepterGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public AccepterGUIScreen(AccepterGUIMenu container, Inventory inventory, Component text) {
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

	private static final ResourceLocation texture = ResourceLocation.parse("crystalnexus:textures/screens/accepter_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		boolean customTooltipShown = false;
		if (mouseX > leftPos + 2 && mouseX < leftPos + 26 && mouseY > topPos + 2 && mouseY < topPos + 26) {
			guiGraphics.renderTooltip(font, Component.translatable("gui.crystalnexus.accepter_gui.tooltip_crystal_here"), mouseX, mouseY);
			customTooltipShown = true;
		}
		if (mouseX > leftPos + 128 && mouseX < leftPos + 152 && mouseY > topPos + 29 && mouseY < topPos + 53) {
			String hoverText = CrystalAccepterOnTickUpdateProcedure.execute(world, x, y, z);
			if (hoverText != null) {
				guiGraphics.renderComponentTooltip(font, Arrays.stream(hoverText.split("\n")).map(Component::literal).collect(Collectors.toList()), mouseX, mouseY);
			}
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
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tooltip.png"), this.leftPos + 6, this.topPos + 7, 0, 0, 16, 16, 16, 16);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/upgradeslot.png"), this.leftPos + 173, this.topPos + 0, 0, 0, 32, 32, 32, 32);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tankmt.png"), this.leftPos + 132, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		if (TankDoneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tankfull.png"), this.leftPos + 132, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		}
		if (TankThreeProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tank34.png"), this.leftPos + 132, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		}
		if (TankTwoProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tank12.png"), this.leftPos + 132, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		}
		if (TankOneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tank14.png"), this.leftPos + 132, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		}
		if (TankNoneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tankmt.png"), this.leftPos + 132, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		}
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), this.leftPos + 50, this.topPos + -15, 0, 0, 126, 18, 126, 18);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.accepter_gui.label_proc_get_block_name_for_gui"), 65, -10, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
	}
}