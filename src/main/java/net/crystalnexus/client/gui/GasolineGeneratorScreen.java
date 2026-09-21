package net.crystalnexus.client.gui;

import net.crystalnexus.block.entity.GasolineGeneratorControllerBlockEntity;
import net.crystalnexus.world.inventory.GasolineGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GasolineGeneratorScreen extends AbstractContainerScreen<GasolineGeneratorMenu> {
    public GasolineGeneratorScreen(GasolineGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 250;
        imageHeight = 136;
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ControllerGuiStyle.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 9, 9, 0xff404040, false);
        GasolineGeneratorControllerBlockEntity controller = menu.controller();
        if (controller == null) return;
        int statusColor = controller.isOperating() ? 0xff216b35 : controller.isFormed() ? 0xff805500 : 0xffa02020;
        graphics.drawString(font, "Status: " + controller.getStatus(), 12, 34, statusColor, false);
        graphics.drawString(font, "Driveshafts: " + controller.getShaftCount() + " / " + GasolineGeneratorControllerBlockEntity.MAX_SHAFTS, 12, 50, 0xff404040, false);
        graphics.drawString(font, "Fuel: " + controller.getFuelName() + " " + controller.getFuelTank().getFluidAmount() + " / " + GasolineGeneratorControllerBlockEntity.FUEL_CAPACITY + " mB", 12, 66, 0xff805500, false);
        graphics.drawString(font, "Output: " + controller.getOutputPerTick() + " FE/t", 12, 82, 0xff205b78, false);
        graphics.drawString(font, "Fuel cycle: " + controller.getFuelPerCycle() + " mB / " + GasolineGeneratorControllerBlockEntity.FUEL_CYCLE_TICKS + " t", 12, 98, 0xff404040, false);
        graphics.drawString(font, "Energy: " + controller.getEnergyStored() + " / " + GasolineGeneratorControllerBlockEntity.ENERGY_CAPACITY + " FE", 12, 114, 0xff216b35, false);
    }
}
