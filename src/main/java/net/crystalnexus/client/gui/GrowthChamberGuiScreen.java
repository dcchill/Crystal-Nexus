package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.GrowthChamberGuiMenu;
import net.crystalnexus.procedures.TankTwoProcedure;
import net.crystalnexus.procedures.TankThreeProcedure;
import net.crystalnexus.procedures.TankOneProcedure;
import net.crystalnexus.procedures.TankNoneProcedure;
import net.crystalnexus.procedures.TankDoneProcedure;
import net.crystalnexus.procedures.PurifierTwoProcedure;
import net.crystalnexus.procedures.PurifierThreeProcedure;
import net.crystalnexus.procedures.PurifierOneProcedure;
import net.crystalnexus.procedures.PurifierNoneProcedure;
import net.crystalnexus.procedures.PurifierDoneProcedure;
import net.crystalnexus.procedures.GrowthChamberOffOnTickUpdateProcedure;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.init.CrystalnexusModScreens;

import java.util.stream.Collectors;
import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;

public class GrowthChamberGuiScreen extends AbstractContainerScreen<GrowthChamberGuiMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public GrowthChamberGuiScreen(GrowthChamberGuiMenu container, Inventory inventory, Component text) {
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
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		boolean customTooltipShown = false;
		if (mouseX > leftPos + 114 && mouseX < leftPos + 138 && mouseY > topPos + 28 && mouseY < topPos + 52) {
			String hoverText = GrowthChamberOffOnTickUpdateProcedure.execute(world, x, y, z);
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
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/growth_chamber_guibg.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tankmt.png"), this.leftPos + 42, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		if (TankDoneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tankfull.png"), this.leftPos + 42, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		}
		if (TankThreeProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tank34.png"), this.leftPos + 42, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		}
		if (TankTwoProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tank12.png"), this.leftPos + 42, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		}
		if (TankOneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tank14.png"), this.leftPos + 42, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		}
		if (TankNoneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tankmt.png"), this.leftPos + 42, this.topPos + 25, 0, 0, 16, 32, 16, 32);
		}
		if (PurifierNoneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/0progress.png"), this.leftPos + 71, this.topPos + 45, 0, 0, 32, 32, 32, 32);
		}
		if (PurifierOneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/1progress.png"), this.leftPos + 71, this.topPos + 45, 0, 0, 32, 32, 32, 32);
		}
		if (PurifierTwoProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/2progress.png"), this.leftPos + 71, this.topPos + 45, 0, 0, 32, 32, 32, 32);
		}
		if (PurifierThreeProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/3progress.png"), this.leftPos + 71, this.topPos + 45, 0, 0, 32, 32, 32, 32);
		}
		if (PurifierDoneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/4progress.png"), this.leftPos + 71, this.topPos + 45, 0, 0, 32, 32, 32, 32);
		}
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), this.leftPos + 50, this.topPos + -15, 0, 0, 126, 18, 126, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/batterylevelsmall.png"), this.leftPos + 110, this.topPos + 24, 0, Mth.clamp((int) EnergyDisplayProcedure.execute(world, x, y, z) * 32, 0, 320), 32, 32, 32, 352);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.growth_chamber_gui.label_proc_get_block_name_for_gui"), 71, -10, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
	}
}