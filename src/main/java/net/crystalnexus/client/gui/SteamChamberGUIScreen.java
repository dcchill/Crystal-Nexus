package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

import net.crystalnexus.world.inventory.SteamChamberGUIMenu;
import net.crystalnexus.procedures.ProgressDisplayProcedure;
import net.crystalnexus.init.CrystalnexusModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class SteamChamberGUIScreen extends HolographicMachineScreen<SteamChamberGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public SteamChamberGUIScreen(SteamChamberGUIMenu container, Inventory inventory, Component text) {
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
		if (mouseX > leftPos + 22 && mouseX < leftPos + 46 && mouseY > topPos + 36 && mouseY < topPos + 60) {
			guiGraphics.renderTooltip(font, Component.translatable("gui.crystalnexus.steam_chamber_gui.tooltip_water_bucket"), mouseX, mouseY);
			customTooltipShown = true;
		}
		if (mouseX > leftPos + 1 && mouseX < leftPos + 25 && mouseY > topPos + 1 && mouseY < topPos + 25) {
			guiGraphics.renderTooltip(font, Component.translatable("gui.crystalnexus.steam_chamber_gui.tooltip_input_coal_or_charcoal_for_fuel"), mouseX, mouseY);
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

		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tooltip.png"), this.leftPos + 4, this.topPos + 4, 0, 0, 16, 16, 16, 16);
		machineProgress(guiGraphics, (Mth.clamp((int) ProgressDisplayProcedure.execute(world, x, y, z) * 32, 0, 320)) / 320.0);
		machineBlockTank(guiGraphics, world, BlockPos.containing(x, y, z).above(), 0,
			leftPos + 122, topPos + 14, 32, 56);
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
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "steam_chamber"; }
}
