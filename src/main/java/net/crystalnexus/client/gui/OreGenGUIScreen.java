package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.OreGenGUIMenu;
import net.crystalnexus.procedures.OreTankTwoProcedure;
import net.crystalnexus.procedures.OreTankThreeProcedure;
import net.crystalnexus.procedures.OreTankOneProcedure;
import net.crystalnexus.procedures.OreTankNoneProcedure;
import net.crystalnexus.procedures.OreTankDoneProcedure;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.init.CrystalnexusModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class OreGenGUIScreen extends HolographicMachineScreen<OreGenGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public OreGenGUIScreen(OreGenGUIMenu container, Inventory inventory, Component text) {
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
		if (mouseX > leftPos + 1 && mouseX < leftPos + 25 && mouseY > topPos + 5 && mouseY < topPos + 29) {
			guiGraphics.renderTooltip(font, Component.translatable("gui.crystalnexus.ore_gen_gui.tooltip_fill_with_crystal_gloop_and_powe"), mouseX, mouseY);
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
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tankmt.png"), this.leftPos + 78, this.topPos + 34, 0, 0, 16, 32, 16, 32);
		if (OreTankOneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tank14.png"), this.leftPos + 78, this.topPos + 34, 0, 0, 16, 32, 16, 32);
		}
		if (OreTankTwoProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tank12.png"), this.leftPos + 78, this.topPos + 34, 0, 0, 16, 32, 16, 32);
		}
		if (OreTankThreeProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tank34.png"), this.leftPos + 78, this.topPos + 34, 0, 0, 16, 32, 16, 32);
		}
		if (OreTankDoneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tankfull.png"), this.leftPos + 78, this.topPos + 34, 0, 0, 16, 32, 16, 32);
		}
		if (OreTankNoneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tankmt.png"), this.leftPos + 78, this.topPos + 34, 0, 0, 16, 32, 16, 32);
		}

		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tooltip.png"), this.leftPos + 6, this.topPos + 7, 0, 0, 16, 16, 16, 16);

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
}
