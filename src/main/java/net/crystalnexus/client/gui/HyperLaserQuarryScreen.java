package net.crystalnexus.client.gui;

import net.crystalnexus.block.entity.QuarryBlockEntity;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.util.QuarryChunkSelection;
import net.crystalnexus.world.inventory.HyperLaserQuarryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class HyperLaserQuarryScreen extends AbstractContainerScreen<HyperLaserQuarryMenu> {
	private static final int GRID_X = 16;
	private static final int GRID_Y = 30;
	private static final int CELL = 10;

	public HyperLaserQuarryScreen(HyperLaserQuarryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 300;
		imageHeight = 238;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(button("-", leftPos + 16, topPos + 106, 0));
		addRenderableWidget(button("+", leftPos + 80, topPos + 106, 1));
		addRenderableWidget(button("-", leftPos + 112, topPos + 106, 2));
		addRenderableWidget(button("+", leftPos + 176, topPos + 106, 3));
	}

	private Button button(String label, int x, int y, int id) {
		return Button.builder(Component.literal(label), ignored -> {
			if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}).bounds(x, y, 18, 16).build();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		ControllerGuiStyle.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		ControllerGuiStyle.inset(graphics, leftPos + GRID_X - 2, topPos + GRID_Y - 2, 72, 72);
		for (var slot : menu.slots) {
			ControllerGuiStyle.inset(graphics, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
		}

		for (int z = -3; z <= 3; z++) {
			for (int x = -3; x <= 3; x++) {
				int color = QuarryChunkSelection.containsOffset(x, z, menu.selectionWidth(), menu.selectionDepth()) ? 0xffa34ac6 : 0xffaaaaaa;
				int cellX = leftPos + GRID_X + (x + 3) * CELL;
				int cellY = topPos + GRID_Y + (z + 3) * CELL;
				graphics.fill(cellX, cellY, cellX + 8, cellY + 8, color);
			}
		}

		double charge = EnergyDisplayProcedure.execute(menu.entity.level(), menu.x, menu.y, menu.z);
		int energyWidth = Math.max(0, Math.min(268, (int) Math.round(charge * 26.8)));
		ControllerGuiStyle.inset(graphics, leftPos + 15, topPos + 132, 270, 7);
		graphics.fill(leftPos + 16, topPos + 133, leftPos + 16 + energyWidth, topPos + 138, 0xffa34ac6);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, 12, 10, 0xff404040, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.selection",
			menu.selectionWidth(), menu.selectionDepth()), 100, 32, 0xff404040, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.y_level", menu.currentY()), 100, 50, 0xff404040, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.buffer",
			menu.bufferedSlots(), QuarryBlockEntity.HYPER_BUFFER_SLOTS), 100, 68, 0xff404040, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.width"), 40, 110, 0xff404040, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.depth"), 136, 110, 0xff404040, false);
		graphics.drawString(font, status(), 100, 88, statusColor(), false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.output"), 226, 142, 0xff404040, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.upgrade"), 226, 102, 0xff404040, false);
		graphics.drawString(font, playerInventoryTitle, 16, 142, 0xff404040, false);
	}

	private Component status() {
		String key = switch (menu.status()) {
			case QuarryBlockEntity.STATUS_MINING -> "mining";
			case QuarryBlockEntity.STATUS_NO_POWER -> "no_power";
			case QuarryBlockEntity.STATUS_BUFFER_FULL -> "buffer_full";
			case QuarryBlockEntity.STATUS_REDSTONE_STOPPED -> "redstone";
			default -> "idle";
		};
		return Component.translatable("gui.crystalnexus.hyper_laser_quarry.status." + key);
	}

	private int statusColor() {
		return menu.status() == QuarryBlockEntity.STATUS_MINING ? 0xff216b35
			: menu.status() == QuarryBlockEntity.STATUS_IDLE ? 0xff404040 : 0xffa02020;
	}
}
