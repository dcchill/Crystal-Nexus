package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.CelestialGearForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public final class CelestialGearForgeScreen extends AbstractContainerScreen<CelestialGearForgeMenu> {
	public CelestialGearForgeScreen(CelestialGearForgeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 226;
	}

	@Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		graphics.fillGradient(x, y, x + imageWidth, y + imageHeight, 0xff292532, 0xff17151d);
		graphics.fill(x, y, x + imageWidth, y + 1, 0xff88734f);
		graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xff88734f);
		graphics.fill(x, y, x + 1, y + imageHeight, 0xff88734f);
		graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xff88734f);
		slot(graphics, 80, 28, false);
		slot(graphics, 53, 55, false);
		slot(graphics, 80, 55, true);
		slot(graphics, 107, 55, false);
		slot(graphics, 80, 82, false);
		slot(graphics, 151, 55, false);
		graphics.fill(x + 128, y + 62, x + 143, y + 67, 0xff08080b);
		int width = Mth.clamp(menu.progress() * 15 / Math.max(1, menu.duration()), 0, 15);
		if (width > 0) graphics.fill(x + 128, y + 62, x + 128 + width, y + 67, 0xffffc647);
	}

	private void slot(GuiGraphics graphics, int x, int y, boolean star) {
		int left = leftPos + x, top = topPos + y;
		graphics.fill(left, top, left + 18, top + 18, 0xff08080b);
		graphics.fill(left + 1, top + 1, left + 17, top + 17, star ? 0xffffb51e : 0xff8c8992);
		graphics.fill(left + 2, top + 2, left + 16, top + 16, star ? 0xff503712 : 0xff37343e);
		graphics.fill(left + 2, top + 2, left + 16, top + 3, star ? 0xffffd469 : 0xffaaa7ae);
		graphics.fill(left + 2, top + 2, left + 3, top + 16, star ? 0xffffd469 : 0xffaaa7ae);
	}

	@Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, 8, 5, 0xfff2e8d5, false);
		graphics.drawString(font, Component.translatable(menu.formed() ? "gui.crystalnexus.celestial_gear_forge.formed" : "gui.crystalnexus.celestial_gear_forge.incomplete"), 8, 106,
			menu.formed() ? 0xff83e08d : 0xffff8278, false);
		graphics.drawString(font, Component.translatable("container.inventory"), 8, 118, 0xffc9c4ce, false);
	}
}
