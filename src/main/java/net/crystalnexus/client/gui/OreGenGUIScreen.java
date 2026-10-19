package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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

public class OreGenGUIScreen extends AbstractContainerScreen<OreGenGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
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

	private static final ResourceLocation texture = ResourceLocation.parse("crystalnexus:textures/screens/ore_gen_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		boolean customTooltipShown = false;
		if (mouseX > leftPos + 1 && mouseX < leftPos + 25 && mouseY > topPos + 5 && mouseY < topPos + 29) {
			guiGraphics.renderTooltip(font, Component.translatable("gui.crystalnexus.ore_gen_gui.tooltip_fill_with_crystal_gloop_and_powe"), mouseX, mouseY);
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
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/upgradeslot.png"), this.leftPos + 173, this.topPos + 0, 0, 0, 32, 32, 32, 32);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tooltip.png"), this.leftPos + 6, this.topPos + 7, 0, 0, 16, 16, 16, 16);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/battery_addon.png"), this.leftPos + -33, this.topPos + -1, 0, 0, 48, 48, 48, 48);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.ore_gen_gui.label_proc_get_block_name_for_gui"), 50, 6, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
	}
}