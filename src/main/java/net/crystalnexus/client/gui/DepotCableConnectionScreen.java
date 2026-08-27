    package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.DepotCableConnectionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class DepotCableConnectionScreen extends AbstractContainerScreen<DepotCableConnectionMenu> {
    public DepotCableConnectionScreen(DepotCableConnectionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 201;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(button("<", 8, 24, 0));
        addRenderableWidget(button(">", 148, 24, 1));
        addRenderableWidget(button("Mode", 8, 48, 2));
        addRenderableWidget(button("-10", 82, 48, 5));
        addRenderableWidget(button("-", 112, 48, 3));
        addRenderableWidget(button("+", 130, 48, 4));
        addRenderableWidget(button("+10", 148, 48, 6));
    }

    private Button button(String label, int x, int y, int id) {
        int width = label.equals("Mode") ? 48 : label.length() > 1 ? 28 : 18;
        return Button.builder(Component.literal(label), ignored -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }).bounds(leftPos + x, topPos + y, width, 18).build();
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff111821);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + 112, 0xff1c2a36);
        for (int i = 0; i < DepotCableConnectionMenu.FILTER_SLOTS; i++) {
            int x = leftPos + 7 + i * 18;
            graphics.fill(x, topPos + 79, x + 18, topPos + 97, 0xff526271);
            graphics.fill(x + 1, topPos + 80, x + 17, topPos + 96, 0xff17232d);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "DEPOT CABLE CONNECTION", 8, 7, 0xff7de8ff, false);
        graphics.drawCenteredString(font, "Connected Side: " + menu.side().getName(), imageWidth / 2, 29, 0xffffffff);
        graphics.drawString(font, Component.literal("Target: ").append(menu.targetName())
                .append("  Mode: " + menu.connectionMode()), 8, 40, 0xffd7e6ed, false);
        graphics.drawString(font, (menu.filterMode() == net.crystalnexus.block.entity.DepotCableConnectionConfig.FilterMode.ALLOW_LISTED
                ? "Allow Listed" : "Block Listed") + "  |  Priority " + menu.priority(), 8, 68, 0xffd7e6ed, false);
        graphics.drawString(font, "Item Filters (right-click to clear)", 8, 99, 0xff91aab7, false);
        graphics.drawString(font, playerInventoryTitle, 8, 109, 0xff91aab7, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
