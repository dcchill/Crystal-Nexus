package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.block.entity.SolarEngineControllerBlockEntity;
import net.crystalnexus.world.inventory.SolarEngineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class SolarEngineScreen extends AbstractContainerScreen<SolarEngineMenu> {
	private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/solar_engine.png");
	private static final int FLUID_X = 7, FLUID_Y = 29, FLUID_WIDTH = 16, FLUID_HEIGHT = 34;
	private ExtractionSlider extractionSlider;
	private int lastExtraction;

	public SolarEngineScreen(SolarEngineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 177;
	}

	@Override protected void init() {
		super.init();
		lastExtraction = menu.controller() == null ? 0 : menu.controller().getExtractionPercent();
		extractionSlider = addRenderableWidget(new ExtractionSlider(leftPos + 28, topPos + 69, 120, 20, lastExtraction));
	}

	@Override protected void containerTick() {
		super.containerTick();
		if (menu.controller() == null) return;
		int synced = menu.controller().getExtractionPercent();
		if (synced != lastExtraction) {
			lastExtraction = synced;
			extractionSlider.setExtraction(synced);
		}
	}

	@Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
		if (menu.controller() != null) {
			graphics.fill(leftPos + FLUID_X - 1, topPos + FLUID_Y - 1,
				leftPos + FLUID_X + FLUID_WIDTH + 1, topPos + FLUID_Y + FLUID_HEIGHT + 1, 0xff161022);
			FluidTankRenderer.draw(graphics, menu.controller().getCoolantTank().getFluid(),
				SolarEngineControllerBlockEntity.TANK_CAPACITY,
				leftPos + FLUID_X, topPos + FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT);
		}
	}

	@Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		SolarEngineControllerBlockEntity controller = menu.controller();
		if (controller == null) return;
		graphics.drawString(font, Component.literal(controller.isOperating() ? "ONLINE" : "IDLE"), 28, 7,
			controller.isOperating() ? 0x55ff88 : 0xaaaaaa, false);
		graphics.drawString(font, Component.literal(controller.getOutputPerTick() + " FE/t"), 105, 7, 0xffdd66, false);
		graphics.drawString(font, Component.literal("Heat " + controller.getHeat() * 100 / SolarEngineControllerBlockEntity.MAX_HEAT + "%"), 105, 19, 0xff7755, false);
		graphics.drawString(font, Component.literal("Stress " + controller.getContainmentStress() * 100 / SolarEngineControllerBlockEntity.MAX_CONTAINMENT_STRESS + "%"), 105, 31, 0xff77dd, false);
	}

	@Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		if (menu.controller() != null && isHovering(FLUID_X, FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT, mouseX, mouseY)) {
			FluidStack fluid = menu.controller().getCoolantTank().getFluid();
			graphics.renderComponentTooltip(font, List.of(fluid.isEmpty() ? Component.literal("Water coolant: empty") : fluid.getHoverName(),
				Component.literal(fluid.getAmount() + " / " + SolarEngineControllerBlockEntity.TANK_CAPACITY + " mB")), mouseX, mouseY);
			return;
		}
		renderTooltip(graphics, mouseX, mouseY);
	}

	private final class ExtractionSlider extends AbstractSliderButton {
		private ExtractionSlider(int x, int y, int width, int height, int extraction) {
			super(x, y, width, height, Component.empty(), extraction / 100.0D);
			updateMessage();
		}
		private int getExtraction() { return Mth.clamp((int) Math.round(value * 100), 0, 100); }
		private void setExtraction(int extraction) { value = Mth.clamp(extraction, 0, 100) / 100.0D; updateMessage(); }
		@Override protected void updateMessage() { setMessage(Component.literal("Extraction: " + getExtraction() + "%")); }
		@Override protected void applyValue() {
			int extraction = getExtraction();
			if (extraction == lastExtraction || minecraft == null || minecraft.gameMode == null) return;
			lastExtraction = extraction;
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, extraction);
		}
	}
}
