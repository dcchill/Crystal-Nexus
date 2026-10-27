package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.ProcessingModuleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ProcessingModuleScreen extends HolographicMachineScreen<ProcessingModuleMenu> {
    public ProcessingModuleScreen(ProcessingModuleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        machineBackground(graphics);
        machineProgress(graphics, (double) menu.progress() / menu.maxProgress());
        MachineGuiTheme.slotFrame(graphics, leftPos + menu.beltItemX() - 1,
            topPos + ProcessingModuleMenu.WORK_SLOT_Y - 1, MachineGuiTheme.accent(machineTier()));
        MachineGuiWidgets.finishBackground(graphics);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.hasMaterialSlot()) label(graphics, "material", ProcessingModuleMenu.MATERIAL_X);
        label(graphics, "held_item", menu.beltItemX());
        var stack = menu.heldItem();
        graphics.renderItem(stack, menu.beltItemX(), ProcessingModuleMenu.WORK_SLOT_Y);
        graphics.renderItemDecorations(font, stack, menu.beltItemX(), ProcessingModuleMenu.WORK_SLOT_Y);
    }

    private void label(GuiGraphics graphics, String key, int slotX) {
        var label = Component.translatable("gui.crystalnexus.processing_module." + key);
        graphics.drawString(font, label, slotX + 8 - font.width(label) / 2,
            ProcessingModuleMenu.WORK_SLOT_Y - 18, 0x88E8FF, false);
    }

    @Override protected void renderMachine(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderMachine(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + menu.beltItemX() && mouseX < leftPos + menu.beltItemX() + 16
                && mouseY >= topPos + ProcessingModuleMenu.WORK_SLOT_Y
                && mouseY < topPos + ProcessingModuleMenu.WORK_SLOT_Y + 16 && !menu.heldItem().isEmpty())
            graphics.renderTooltip(font, menu.heldItem(), mouseX, mouseY);
    }

    @Override protected BlockPos machinePosition() { return menu.guiPosition(); }
    @Override protected String machineFallbackId() { return menu.machineFallbackId(); }
}
