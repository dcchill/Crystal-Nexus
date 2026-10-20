package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.CraftingProcessorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CraftingProcessorScreen extends HolographicMachineScreen<CraftingProcessorMenu> {
    public CraftingProcessorScreen(CraftingProcessorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 126;
    }

    @Override
    protected void renderMachine(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.renderMachine(graphics, mouseX, mouseY, partialTick);
        ItemStack target = menu.getTargetStack();
        ItemStack step = menu.getStepStack();
        if (!target.isEmpty() && inside(mouseX, mouseY, leftPos + 10, topPos + 27)) {
            graphics.renderTooltip(font, target, mouseX, mouseY);
        } else if (!step.isEmpty() && inside(mouseX, mouseY, leftPos + 10, topPos + 51)) {
            graphics.renderTooltip(font, step, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        machineBackground(graphics);
        machineProgress(graphics, menu.hasJob() ? menu.getOverallPercent() / 100.0 : 0);
        if (!menu.hasJob()) return;
        graphics.renderItem(menu.getTargetStack(), leftPos + 10, topPos + 27);
        graphics.renderItem(menu.getStepStack(), leftPos + 10, topPos + 51);

        MachineGuiWidgets.finishBackground(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!menu.hasJob()) {
            graphics.drawString(font, "Idle", 10, 31, 0xFF555555, false);
            return;
        }
        String target = shortened(menu.getTargetStack().getHoverName().getString());
        String step = shortened(menu.getStepStack().getHoverName().getString());
        graphics.drawString(font, menu.getTargetAmount() + " x " + target, 32, 31, 0xE3F3FF, false);
        graphics.drawString(font, menu.getStepAmount() + " x " + step, 32, 55, 0xE3F3FF, false);
        graphics.drawString(font, "Craft " + (menu.getStepIndex() + 1) + " / " + menu.getStepCount(), 10, 66,
                0xE3F3FF, false);
        String current = menu.isProcessing() ? "Machine processing..." : "Current craft: " + menu.getStepPercent() + "%";
        graphics.drawString(font, current, 10, 88, 0xE3F3FF, false);
        graphics.drawString(font, "Whole job: " + menu.getOverallPercent() + "%", 10, 115, 0xE3F3FF, false);
    }

    private String shortened(String value) {
        return font.plainSubstrByWidth(value, 130);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }
    @Override protected net.minecraft.core.BlockPos machinePosition() { return menu.guiPosition(); }
    @Override protected String machineFallbackId() { return "crafting_upgrade"; }
}
