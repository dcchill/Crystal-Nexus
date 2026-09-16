package net.crystalnexus.client.gui;

import net.crystalnexus.block.entity.EngineeredHeartBlockEntity;
import net.crystalnexus.world.inventory.EngineeredHeartMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class EngineeredHeartScreen extends AbstractContainerScreen<EngineeredHeartMenu> {
    private static final int TANK_X = 20, TANK_Y = 37, TANK_WIDTH = 24, TANK_HEIGHT = 52;
    public EngineeredHeartScreen(EngineeredHeartMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 310; imageHeight = 130; }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff13080a);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xff3b171c);
        graphics.fill(leftPos + TANK_X - 2, topPos + TANK_Y - 2, leftPos + TANK_X + TANK_WIDTH + 2, topPos + TANK_Y + TANK_HEIGHT + 2, 0xffc04651);
        graphics.fill(leftPos + TANK_X, topPos + TANK_Y, leftPos + TANK_X + TANK_WIDTH, topPos + TANK_Y + TANK_HEIGHT, 0xff080304);
        EngineeredHeartBlockEntity heart = menu.heart();
        if (heart != null) FluidTankRenderer.draw(graphics, heart.getBloodTank().getFluid(), EngineeredHeartBlockEntity.BLOOD_TANK_CAPACITY,
            leftPos + TANK_X, topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 9, 9, 0xffffd3d7, false);
        EngineeredHeartBlockEntity heart = menu.heart();
        if (heart == null) return;
        graphics.drawString(font, Component.literal("Blood: " + heart.getBloodTank().getFluidAmount() + " / " + EngineeredHeartBlockEntity.BLOOD_TANK_CAPACITY + " mB"), 56, 38, 0xffffb1bb, false);
        graphics.drawString(font, Component.literal("Energy: " + heart.getEnergyStorage().getEnergyStored() + " / " + EngineeredHeartBlockEntity.ENERGY_CAPACITY + " FE"), 56, 56, 0xffffff8a, false);
        graphics.drawString(font, Component.literal("500 mB → 100,000 FE / heartbeat"), 56, 74, 0xfff6d36f, false);
        graphics.drawString(font, Component.literal(heart.getStatus()), 9, 23, heart.isFormed() ? 0xffa8e090 : 0xffff8888, false);
        graphics.drawString(font, Component.literal("1 beat / second · 5,000 FE/t average"), 56, 92, 0xffffd3d7, false);
        graphics.drawString(font, Component.literal("5×5×5 structure · See JEI for layout"), 9, 112, 0xffffd3d7, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        EngineeredHeartBlockEntity heart = menu.heart();
        if (heart != null && isHovering(TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) graphics.renderComponentTooltip(font,
            List.of(Component.literal("Blood"), Component.literal(heart.getBloodTank().getFluidAmount() + " / " + EngineeredHeartBlockEntity.BLOOD_TANK_CAPACITY + " mB")), mouseX, mouseY);
        else renderTooltip(graphics, mouseX, mouseY);
    }
}
