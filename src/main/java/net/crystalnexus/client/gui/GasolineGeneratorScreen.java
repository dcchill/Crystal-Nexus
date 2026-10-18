package net.crystalnexus.client.gui;

import net.crystalnexus.block.entity.GasolineGeneratorControllerBlockEntity;
import net.crystalnexus.world.inventory.GasolineGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GasolineGeneratorScreen extends HolographicMachineScreen<GasolineGeneratorMenu> {
    public GasolineGeneratorScreen(GasolineGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 250;
        imageHeight = 136;
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        machineBackground(graphics);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        GasolineGeneratorControllerBlockEntity controller = menu.controller();
        if (controller == null) return;
        int statusColor = controller.isOperating() ? 0xFF6EECA1 : controller.isFormed() ? 0xFFFFCE79 : 0xFFFF8080;
        graphics.drawString(font, "Status: " + controller.getStatus(), 12, 34, statusColor, false);
        graphics.drawString(font, "Driveshafts: " + controller.getShaftCount() + " / " + GasolineGeneratorControllerBlockEntity.MAX_SHAFTS, 12, 50, 0xE3F3FF, false);
        graphics.drawString(font, "Fuel: " + controller.getFuelName() + " " + controller.getFuelTank().getFluidAmount() + " / " + GasolineGeneratorControllerBlockEntity.FUEL_CAPACITY + " mB", 12, 66, 0xFFFFCE79, false);
        graphics.drawString(font, "Output: " + controller.getOutputPerTick() + " FE/t", 12, 82, 0xFF8BD8FF, false);
        graphics.drawString(font, "Fuel cycle: " + controller.getFuelPerCycle() + " mB / " + GasolineGeneratorControllerBlockEntity.FUEL_CYCLE_TICKS + " t", 12, 98, 0xE3F3FF, false);
        graphics.drawString(font, "Energy: " + controller.getEnergyStored() + " / " + GasolineGeneratorControllerBlockEntity.ENERGY_CAPACITY + " FE", 12, 114, 0xFF6EECA1, false);
    }
    @Override protected net.minecraft.core.BlockPos machinePosition() { return menu.controller() == null ? null : menu.controller().getBlockPos(); }
    @Override protected String machineFallbackId() { return "diesel_generator_controller"; }
}
