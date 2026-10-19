package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.CraftingProcessorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CraftingProcessorScreen extends AbstractContainerScreen<CraftingProcessorMenu> {
    public CraftingProcessorScreen(CraftingProcessorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 126;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
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
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 2, 0xFFFFFFFF);
        graphics.fill(leftPos, topPos + imageHeight - 2, leftPos + imageWidth, topPos + imageHeight, 0xFF555555);
        graphics.fill(leftPos, topPos, leftPos + 2, topPos + imageHeight, 0xFFFFFFFF);
        graphics.fill(leftPos + imageWidth - 2, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF555555);
        if (!menu.hasJob()) return;
        graphics.renderItem(menu.getTargetStack(), leftPos + 10, topPos + 27);
        graphics.renderItem(menu.getStepStack(), leftPos + 10, topPos + 51);
        progressBar(graphics, topPos + 76, menu.getStepPercent());
        progressBar(graphics, topPos + 103, menu.getOverallPercent());
    }

    private void progressBar(GuiGraphics graphics, int y, int percent) {
        graphics.fill(leftPos + 10, y, leftPos + 166, y + 10, 0xFF373737);
        graphics.fill(leftPos + 12, y + 2, leftPos + 12 + 152 * percent / 100, y + 8, 0xFF35BFB4);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 8, 0xFF404040, false);
        if (!menu.hasJob()) {
            graphics.drawString(font, "Idle", 10, 31, 0xFF555555, false);
            return;
        }
        String target = shortened(menu.getTargetStack().getHoverName().getString());
        String step = shortened(menu.getStepStack().getHoverName().getString());
        graphics.drawString(font, menu.getTargetAmount() + " x " + target, 32, 31, 0xFF404040, false);
        graphics.drawString(font, menu.getStepAmount() + " x " + step, 32, 55, 0xFF404040, false);
        graphics.drawString(font, "Craft " + (menu.getStepIndex() + 1) + " / " + menu.getStepCount(), 10, 66,
                0xFF404040, false);
        String current = menu.isProcessing() ? "Machine processing..." : "Current craft: " + menu.getStepPercent() + "%";
        graphics.drawString(font, current, 10, 88, 0xFF404040, false);
        graphics.drawString(font, "Whole job: " + menu.getOverallPercent() + "%", 10, 115, 0xFF404040, false);
    }

    private String shortened(String value) {
        return font.plainSubstrByWidth(value, 130);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }
}
