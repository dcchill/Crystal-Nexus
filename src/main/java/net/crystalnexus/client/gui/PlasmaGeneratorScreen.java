package net.crystalnexus.client.gui;

import net.crystalnexus.block.entity.PlasmaGeneratorControllerBlockEntity;
import net.crystalnexus.world.inventory.PlasmaGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class PlasmaGeneratorScreen extends AbstractContainerScreen<PlasmaGeneratorMenu> {
    private static final int FLUID_X = 20, FLUID_Y = 37, FLUID_WIDTH = 24, FLUID_HEIGHT = 52;

    public PlasmaGeneratorScreen(PlasmaGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 250;
        imageHeight = 112;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ControllerGuiStyle.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        PlasmaGeneratorControllerBlockEntity controller = menu.controller();
        if (controller != null) {
            int x = leftPos + FLUID_X, y = topPos + FLUID_Y;
            ControllerGuiStyle.inset(graphics, x - 1, y - 1, FLUID_WIDTH + 2, FLUID_HEIGHT + 2);
            FluidTankRenderer.draw(graphics, controller.getArgonTank().getFluid(),
                PlasmaGeneratorControllerBlockEntity.TANK_CAPACITY, x, y, FLUID_WIDTH, FLUID_HEIGHT);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        PlasmaGeneratorControllerBlockEntity controller = menu.controller();
        graphics.drawString(font, title, 9, 9, 0xff404040, false);
        if (controller == null) return;
        int statusColor = controller.isOperating() ? 0xff216b35
            : controller.isFormed() ? 0xff805500 : 0xffa02020;
        graphics.drawString(font, Component.literal("Status: " + controller.getStatus()), 56, 37, statusColor, false);
        graphics.drawString(font, Component.literal("Output: " + controller.getOutputPerTick() + " FE/t"), 56, 55, 0xff205b78, false);
        graphics.drawString(font, Component.literal("Argon: " + controller.getArgonTank().getFluidAmount()
            + " / " + PlasmaGeneratorControllerBlockEntity.TANK_CAPACITY + " mB"), 56, 73, 0xff713c8e, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (menu.controller() != null && isHovering(FLUID_X, FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.literal("Argon"),
                Component.literal(menu.controller().getArgonTank().getFluidAmount() + " / "
                    + PlasmaGeneratorControllerBlockEntity.TANK_CAPACITY + " mB")), mouseX, mouseY);
        } else renderTooltip(graphics, mouseX, mouseY);
    }
}
