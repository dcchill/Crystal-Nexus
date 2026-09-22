package net.crystalnexus.client.gui;

import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.crystalnexus.world.inventory.ReactorGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ReactorGUIScreen extends AbstractContainerScreen<ReactorGUIMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.parse("crystalnexus:textures/screens/reactor_gui_54.png");
    private static final ResourceLocation NAME_ADDON = ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png");
    private static final ResourceLocation UPGRADE_ADDON = ResourceLocation.parse("crystalnexus:textures/screens/reactorupgradeslot.png");
    private static final ResourceLocation WASTE_ADDON = ResourceLocation.parse("crystalnexus:textures/screens/waste_slot.png");
    private Button previous;
    private Button next;

    public ReactorGUIScreen(ReactorGUIMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 206;
        imageHeight = 222;
    }

    @Override protected void init() {
        super.init();
        topPos = Math.min(Math.max(15, topPos), Math.max(0, height - imageHeight));
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(menu.page() - 1))
            .bounds(leftPos + 8, topPos + 7, 18, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(menu.page() + 1))
            .bounds(leftPos + 150, topPos + 7, 18, 18).build());
        updateButtons();
    }

    private void changePage(int page) {
        if (page < 0 || page >= menu.pageCount()) return;
        menu.setClientPage(page);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
        updateButtons();
    }

    private void updateButtons() {
        previous.active = menu.page() > 0;
        next.active = menu.page() + 1 < menu.pageCount();
    }

    @Override protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, 176, 222, 256, 256);
        graphics.blit(NAME_ADDON, leftPos + 50, topPos - 15, 0, 0, 126, 18, 126, 18);
        graphics.blit(UPGRADE_ADDON, leftPos + 173, topPos, 0, 0, 32, 32, 32, 32);
        graphics.blit(WASTE_ADDON, leftPos + 173, topPos + 34, 0, 0, 32, 32, 32, 32);
        for (int row = 0; row < 9; row++) {
            if (menu.rodPosition(row) == null) continue;
            for (int cell = 0; cell < 3; cell++) ControllerGuiStyle.inset(graphics,
                leftPos + 7 + (row % 3) * 54 + cell * 18, topPos + 31 + (row / 3) * 18, 18, 18);
        }
        ReactorComputerBlockEntity controller = menu.controller();
        if (controller != null) {
            ControllerGuiStyle.panel(graphics, leftPos + 174, topPos + 74, 30, 62);
            ControllerGuiStyle.inset(graphics, leftPos + 181, topPos + 80, 16, 50);
            FluidTankRenderer.draw(graphics, controller.getFluidTank().getFluid(), 16000,
                leftPos + 182, topPos + 81, 14, 48);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component name = Component.translatable("block.crystalnexus.reactor_computer");
        graphics.drawString(font, name, 50 + (126 - font.width(name)) / 2, -10, 0xff404040, false);
        Component page = Component.literal((menu.page() + 1) + " / " + menu.pageCount());
        graphics.drawString(font, page, 88 - font.width(page) / 2, 12, 0xff404040, false);
        ReactorComputerBlockEntity controller = menu.controller();
        if (controller != null) {
            var data = controller.getPersistentData();
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth("Status: " + data.getString("reactorStatus"), 160)),
                8, 91, 0xff404040, false);
            graphics.drawString(font, Component.literal("FE/t " + compact(data.getDouble("lastFEt"))), 8, 103, 0xff205b78, false);
            graphics.drawString(font, Component.literal("Heat " + (int) data.getDouble("heat") + " C"), 88, 103, 0xffa04520, false);
            graphics.drawString(font, Component.literal("Energy " + compact(controller.getEnergyStorage().getEnergyStored())), 8, 115, 0xff205b78, false);
            graphics.drawString(font, Component.literal("Coolant " + compact(controller.getFluidTank().getFluidAmount()) + " mB"), 88, 115, 0xff205b78, false);
        }
        int first = menu.page() * 9 + 1;
        int last = Math.min(menu.rodCount(), first + 8);
        graphics.drawString(font, Component.literal("Fuel rods " + first + "-" + last + " / " + menu.rodCount()), 8, 127, 0xff404040, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private static String compact(double value) {
        if (value >= 1_000_000) return String.format(java.util.Locale.ROOT, "%.1fM", value / 1_000_000);
        if (value >= 10_000) return Math.round(value / 1_000) + "k";
        return Long.toString(Math.round(value));
    }
}
