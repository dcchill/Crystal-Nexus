    package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.DepotCableConnectionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class DepotCableConnectionScreen extends AbstractContainerScreen<DepotCableConnectionMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.parse(
            "crystalnexus:textures/screens/depot_cable_gui.png");
    private Button filterButton;

    public DepotCableConnectionScreen(DepotCableConnectionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 212;
        imageHeight = 236;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(button("<", 8, 24, 0));
        addRenderableWidget(button(">", 186, 24, 1));
        filterButton = addRenderableWidget(button(filterButtonText(), 8, 72, 2));
        addRenderableWidget(button("-10", 104, 72, 5));
        addRenderableWidget(button("-", 134, 72, 3));
        addRenderableWidget(button("+", 154, 72, 4));
        addRenderableWidget(button("+10", 174, 72, 6));
    }

    private Button button(String label, int x, int y, int id) {
        int width = id == 2 ? 72 : label.length() > 1 ? 28 : 18;
        return Button.builder(Component.literal(label), ignored -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }).bounds(leftPos + x, topPos + y, width, 18).build();
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "DEPOT CABLE CONNECTION", 8, 7, 0xff7de8ff, false);
        graphics.drawCenteredString(font, "Connected Side: " + menu.side().getName(), imageWidth / 2, 29, 0xffffffff);
        graphics.drawString(font, Component.literal("Target: ").append(menu.targetName()), 8, 46, 0xffd7e6ed, false);
        graphics.drawString(font, "Cable Mode: " + menu.connectionMode(), 8, 58, 0xff91aab7, false);
        String priority = "Priority: " + menu.priority();
        graphics.drawString(font, priority, imageWidth - 8 - font.width(priority), 58, 0xff91aab7, false);
        graphics.drawString(font, "Items (right-click to clear)", DepotCableConnectionMenu.SLOT_X, 96,
                0xff91aab7, false);
        graphics.drawString(font, playerInventoryTitle, DepotCableConnectionMenu.INVENTORY_X, 142, 0xff91aab7, false);
    }

    private String filterButtonText() {
        if (menu.connectionMode().equals("EXPORT")) return "Whitelist";
        return menu.filterMode() == net.crystalnexus.block.entity.DepotCableConnectionConfig.FilterMode.ALLOW_LISTED
                ? "Allow Listed" : "Block Listed";
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (filterButton != null) {
            filterButton.setMessage(Component.literal(filterButtonText()));
            filterButton.active = !menu.connectionMode().equals("EXPORT");
        }
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
