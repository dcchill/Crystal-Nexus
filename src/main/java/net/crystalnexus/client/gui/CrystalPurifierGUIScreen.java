package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.CrystalPurifierGUIMenu;
import net.crystalnexus.procedures.PurifierTwoProcedure;
import net.crystalnexus.procedures.PurifierThreeProcedure;
import net.crystalnexus.procedures.PurifierOneProcedure;
import net.crystalnexus.procedures.PurifierNoneProcedure;
import net.crystalnexus.procedures.PurifierDoneProcedure;
import net.crystalnexus.procedures.ProgressDisplayProcedure;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.procedures.CrystalPurifierOnTickUpdateProcedure;
import net.crystalnexus.init.CrystalnexusModScreens;

import java.util.stream.Collectors;
import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;

public class CrystalPurifierGUIScreen extends AbstractContainerScreen<CrystalPurifierGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public CrystalPurifierGUIScreen(CrystalPurifierGUIMenu container, Inventory inventory, Component text) {
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

	private static final ResourceLocation texture = ResourceLocation.parse("crystalnexus:textures/screens/crystal_purifier_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		boolean customTooltipShown = false;
		if (mouseX > leftPos + -22 && mouseX < leftPos + 2 && mouseY > topPos + 9 && mouseY < topPos + 33) {
			String hoverText = CrystalPurifierOnTickUpdateProcedure.execute(world, x, y, z);
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
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/crystal_purifier_gui_addon.png"), this.leftPos + 65, this.topPos + 33, 0, 0, 44, 33, 44, 33);
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
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/battery_addon.png"), this.leftPos + -33, this.topPos + -1, 0, 0, 48, 48, 48, 48);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), this.leftPos + 50, this.topPos + -15, 0, 0, 126, 18, 126, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/upgradeslot.png"), this.leftPos + 173, this.topPos + 0, 0, 0, 32, 32, 32, 32);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/batterylevelsmall.png"), this.leftPos + -25, this.topPos + 5, 0, Mth.clamp((int) EnergyDisplayProcedure.execute(world, x, y, z) * 32, 0, 320), 32, 32, 32, 352);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/progressbarinvert.png"), this.leftPos + 71, this.topPos + 14, 0, Mth.clamp((int) ProgressDisplayProcedure.execute(world, x, y, z) * 32, 0, 320), 32, 32, 32, 352);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.crystal_purifier_gui.label_proc_get_block_name_for_gui"), 67, -9, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
	}
}