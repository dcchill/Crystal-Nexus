package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.procedures.ProgressDisplayProcedure;
import net.crystalnexus.world.inventory.ArcFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public final class ArcFurnaceScreen extends AbstractContainerScreen<ArcFurnaceMenu> {
	private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/arc_smelter_gui.png");

	public ArcFurnaceScreen(ArcFurnaceMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
		graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), leftPos + 50, topPos - 15, 0, 0, 126, 18, 126, 18);
		graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/upgradeslot.png"), leftPos + 173, topPos, 0, 0, 32, 32, 32, 32);
		graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/battery_addon.png"), leftPos - 33, topPos - 1, 0, 0, 48, 48, 48, 48);
		graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/progressbar.png"), leftPos + 70, topPos + 27, 0,
			Mth.clamp((int) ProgressDisplayProcedure.execute(menu.entity.level(), menu.x, menu.y, menu.z) * 32, 0, 320), 32, 32, 32, 352);
		graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/batterylevelsmall.png"), leftPos - 25, topPos + 5, 0,
			Mth.clamp((int) EnergyDisplayProcedure.execute(menu.entity.level(), menu.x, menu.y, menu.z) * 32, 0, 320), 32, 32, 32, 352);
		RenderSystem.disableBlend();
	}

	@Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, Component.literal("Arc Blast Furnace"), 58, -11, -12829636, false);
	}
}
