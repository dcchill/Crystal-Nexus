package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.MRecrystallGuiMenu;
import net.crystalnexus.procedures.RecryNoneProcedure;
import net.crystalnexus.procedures.Recry8Procedure;
import net.crystalnexus.procedures.Recry7Procedure;
import net.crystalnexus.procedures.Recry6Procedure;
import net.crystalnexus.procedures.Recry5Procedure;
import net.crystalnexus.procedures.Recry4Procedure;
import net.crystalnexus.procedures.Recry3Procedure;
import net.crystalnexus.procedures.Recry2Procedure;
import net.crystalnexus.procedures.Recry1Procedure;
import net.crystalnexus.procedures.MetallurgicRecrystallizerOnTickUpdateProcedure;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.init.CrystalnexusModScreens;

import java.util.stream.Collectors;
import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;

public class MRecrystallGuiScreen extends AbstractContainerScreen<MRecrystallGuiMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public MRecrystallGuiScreen(MRecrystallGuiMenu container, Inventory inventory, Component text) {
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
		if (mouseX > leftPos + -21 && mouseX < leftPos + 3 && mouseY > topPos + 8 && mouseY < topPos + 32) {
			String hoverText = MetallurgicRecrystallizerOnTickUpdateProcedure.execute(world, x, y, z);
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
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/m_recrystall_gui_new.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);
		if (RecryNoneProcedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/m_recrystall_gui_overlay.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);
		}
		if (Recry1Procedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/m_recrystall_gui_overlay1.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);
		}
		if (Recry2Procedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/m_recrystall_gui_overlay2.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);
		}
		if (Recry3Procedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/m_recrystall_gui_overlay3.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);
		}
		if (Recry4Procedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/m_recrystall_gui_overlay4.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);
		}
		if (Recry5Procedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/m_recrystall_gui_overlay5.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);
		}
		if (Recry6Procedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/m_recrystall_gui_overlay6.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);
		}
		if (Recry7Procedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/m_recrystall_gui_overlay7.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);
		}
		if (Recry8Procedure.execute(world, x, y, z)) {
			guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/m_recrystall_gui_overlay8.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);
		}
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/upgradeslot.png"), this.leftPos + 173, this.topPos + 0, 0, 0, 32, 32, 32, 32);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/battery_addon.png"), this.leftPos + -33, this.topPos + -1, 0, 0, 48, 48, 48, 48);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), this.leftPos + 50, this.topPos + -15, 0, 0, 126, 18, 126, 18);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.m_recrystall_gui.label_proc_get_block_name_for_gui"), 72, -10, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
	}
}