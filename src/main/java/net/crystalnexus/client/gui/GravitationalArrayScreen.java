package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.world.inventory.GravitationalArrayMenu;
import net.crystalnexus.block.entity.GravitationalArrayControllerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class GravitationalArrayScreen extends AbstractContainerScreen<GravitationalArrayMenu> {
	private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/gravitational_array.png");
	private static final ResourceLocation PROGRESS = ResourceLocation.parse("crystalnexus:textures/screens/progressbar.png");
	private static final int FLUID_X = 7, FLUID_Y = 29, FLUID_WIDTH = 16, FLUID_HEIGHT = 34;
	public GravitationalArrayScreen(GravitationalArrayMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 181; }
	@Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
		GravitationalArrayControllerBlockEntity controller = menu.controller();
		if (controller == null) return;
		int duration = controller.getActiveDuration();
		int frame = duration == 0 ? 0 : Mth.clamp(Mth.ceil(controller.getProgress() * 10.0F / duration), 0, 10);
		graphics.blit(PROGRESS, leftPos + 72, topPos + 30, 0, frame * 32, 32, 32, 32, 352);
		graphics.fill(leftPos + FLUID_X - 1, topPos + FLUID_Y - 1,
			leftPos + FLUID_X + FLUID_WIDTH + 1, topPos + FLUID_Y + FLUID_HEIGHT + 1, 0xff161022);
		FluidTankRenderer.draw(graphics, controller.getTemporalFluidTank().getFluid(),
			GravitationalArrayControllerBlockEntity.TANK_CAPACITY,
			leftPos + FLUID_X, topPos + FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT);
	}
	@Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { }
	@Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		GravitationalArrayControllerBlockEntity controller = menu.controller();
		if (controller != null && isHovering(FLUID_X, FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT, mouseX, mouseY)) {
			FluidStack fluid = controller.getTemporalFluidTank().getFluid();
			graphics.renderComponentTooltip(font, List.of(fluid.isEmpty() ? Component.literal("Empty") : fluid.getHoverName(),
				Component.literal(fluid.getAmount() + " / " + GravitationalArrayControllerBlockEntity.TANK_CAPACITY + " mB")), mouseX, mouseY);
			return;
		}
		renderTooltip(graphics, mouseX, mouseY);
	}
}
