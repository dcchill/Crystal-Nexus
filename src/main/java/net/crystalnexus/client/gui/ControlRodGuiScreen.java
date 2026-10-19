package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.ControlRodGuiMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public final class ControlRodGuiScreen extends AbstractContainerScreen<ControlRodGuiMenu> {
	private InsertionSlider insertionSlider;
	private int lastMenuInsertion;

	public ControlRodGuiScreen(ControlRodGuiMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 92;
	}

	@Override
	protected void init() {
		super.init();
		lastMenuInsertion = menu.getInsertion();
		insertionSlider = addRenderableWidget(new InsertionSlider(leftPos + 18, topPos + 30, 140, 20, lastMenuInsertion));
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		int syncedInsertion = menu.getInsertion();
		if (syncedInsertion != lastMenuInsertion) {
			lastMenuInsertion = syncedInsertion;
			insertionSlider.setInsertion(syncedInsertion);
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff373737);
		graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xffc6c6c6);
		graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + 5, 0xffffffff);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		Component heading = Component.translatable("gui.crystalnexus.control_rod_gui.title");
		graphics.drawString(font, heading, (imageWidth - font.width(heading)) / 2, 10, 0x404040, false);
		String reactivity = Component.translatable("gui.crystalnexus.control_rod_gui.reactivity", 100 - insertionSlider.getInsertion()).getString();
		graphics.drawString(font, reactivity, (imageWidth - font.width(reactivity)) / 2, 62, 0x404040, false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	private final class InsertionSlider extends AbstractSliderButton {
		private InsertionSlider(int x, int y, int width, int height, int insertion) {
			super(x, y, width, height, Component.empty(), insertion / 100.0);
			updateMessage();
		}

		private int getInsertion() {
			return Mth.clamp((int) Math.round(value * 100), 0, 100);
		}

		private void setInsertion(int insertion) {
			value = Mth.clamp(insertion, 0, 100) / 100.0;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("gui.crystalnexus.control_rod_gui.insertion", getInsertion()));
		}

		@Override
		protected void applyValue() {
			int insertion = getInsertion();
			if (insertion == lastMenuInsertion) {
				return;
			}
			lastMenuInsertion = insertion;
			menu.setClientInsertion(insertion);
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, insertion);
		}
	}
}
