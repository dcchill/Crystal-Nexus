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
		imageWidth = 248;
		imageHeight = 190;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(button("-", leftPos + 14, topPos + 83, 0));
		addRenderableWidget(button("+", leftPos + 65, topPos + 83, 1));
		addRenderableWidget(button("-", leftPos + 89, topPos + 83, 2));
		addRenderableWidget(button("+", leftPos + 120, topPos + 83, 3));
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
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff20242b);
		graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xff343b46);
		graphics.fill(leftPos + 8, topPos + 20, leftPos + 132, topPos + 78, 0xff171a20);
		graphics.fill(leftPos + 136, topPos + 20, leftPos + 204, topPos + 79, 0xff171a20);
		graphics.fill(leftPos + 210, topPos + 20, leftPos + 239, topPos + 50, 0xff171a20);

		for (int z = -3; z <= 3; z++) {
			for (int x = -3; x <= 3; x++) {
				int color = QuarryChunkSelection.containsOffset(x, z, menu.selectionWidth(), menu.selectionDepth()) ? 0xffd17df3 : 0xff4a5260;
				int cellX = leftPos + GRID_X + (x + 3) * CELL;
				int cellY = topPos + GRID_Y + (z + 3) * 6;
				graphics.fill(cellX, cellY, cellX + 8, cellY + 5, color);
			}
		}

		double charge = EnergyDisplayProcedure.execute(menu.entity.level(), menu.x, menu.y, menu.z);
		int energyWidth = Math.max(0, Math.min(120, (int) Math.round(charge * 12)));
		graphics.fill(leftPos + 8, topPos + 102, leftPos + 128, topPos + 106, 0xff171a20);
		graphics.fill(leftPos + 8, topPos + 102, leftPos + 8 + energyWidth, topPos + 106, 0xffe641ff);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, 8, 7, 0xffffff, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.selection",
			menu.selectionWidth(), menu.selectionDepth()), 91, 31, 0xffffff, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.y_level", menu.currentY()), 91, 45, 0xffffff, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.buffer",
			menu.bufferedSlots(), QuarryBlockEntity.HYPER_BUFFER_SLOTS), 91, 59, 0xffffff, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.width"), 33, 87, 0xffffff, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.hyper_laser_quarry.depth"), 107, 87, 0xffffff, false);
		graphics.drawString(font, status(), 136, 86, statusColor(), false);
		graphics.drawString(font, playerInventoryTitle, 43, 98, 0xffffff, false);
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
		return menu.status() == QuarryBlockEntity.STATUS_MINING ? 0xff76ff7a
			: menu.status() == QuarryBlockEntity.STATUS_IDLE ? 0xffdddddd : 0xffff7373;
	}
}
