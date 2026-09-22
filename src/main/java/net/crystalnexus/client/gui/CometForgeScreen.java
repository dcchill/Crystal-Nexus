package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.world.inventory.CometForgeMenu;
import net.crystalnexus.block.entity.CometForgeControllerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class CometForgeScreen extends AbstractContainerScreen<CometForgeMenu> {
	private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/gravitational_array.png");
	private static final ResourceLocation PROGRESS = ResourceLocation.parse("crystalnexus:textures/screens/progressbar.png");
	private static final int FLUID_X = 7, FLUID_Y = 29, FLUID_WIDTH = 16, FLUID_HEIGHT = 34;
	public CometForgeScreen(CometForgeMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 181; }
	@Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
		CometForgeControllerBlockEntity controller = menu.controller();
		if (controller == null) return;
		int duration = controller.getActiveDuration();
		int frame = duration == 0 ? 0 : Mth.clamp(Mth.ceil(controller.getProgress() * 10.0F / duration), 0, 10);
		graphics.blit(PROGRESS, leftPos + 72, topPos + 30, 0, frame * 32, 32, 32, 32, 352);
		graphics.fill(leftPos + FLUID_X - 1, topPos + FLUID_Y - 1,
			leftPos + FLUID_X + FLUID_WIDTH + 1, topPos + FLUID_Y + FLUID_HEIGHT + 1, 0xff161022);
		FluidTankRenderer.draw(graphics, controller.getTemporalFluidTank().getFluid(),
			CometForgeControllerBlockEntity.TANK_CAPACITY,
			leftPos + FLUID_X, topPos + FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT);
	}
	@Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        var controller = menu.controller();
        if (controller == null) return;
        graphics.drawString(font, controller.isFormed() ? "Formed" : "Incomplete", 5, 3, 0xffeeeeee, false);
        graphics.drawString(font, controller.multiblockEnergyInput().getEnergyStored() + " FE", 5, 82, 0xffeeeeee, false);
        graphics.drawString(font, (controller.getProgress() * 100 / CometForgeControllerBlockEntity.DURATION) + "%", 140, 82, 0xffeeeeee, false);
    }
	@Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		CometForgeControllerBlockEntity controller = menu.controller();
		if (controller != null && isHovering(FLUID_X, FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT, mouseX, mouseY)) {
			FluidStack fluid = controller.getTemporalFluidTank().getFluid();
			graphics.renderComponentTooltip(font, List.of(fluid.isEmpty() ? Component.literal("Empty") : fluid.getHoverName(),
				Component.literal(fluid.getAmount() + " / " + CometForgeControllerBlockEntity.TANK_CAPACITY + " mB")), mouseX, mouseY);
			return;
		}
        if (isHovering(4, 81, 168, 12, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.literal("500,000 FE / 25,000 mB / 10 s"),
                Component.literal("3 high-tier singularities + 1 full material stack")), mouseX, mouseY);
        } else if (isHovering(4, 2, 70, 12, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.literal("Replace meteorite alloy casing with"),
                Component.literal("at least one Energy Input and Fluid Input.")), mouseX, mouseY);
        } else renderTooltip(graphics, mouseX, mouseY);
	}
}
