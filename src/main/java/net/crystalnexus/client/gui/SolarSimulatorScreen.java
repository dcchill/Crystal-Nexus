package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.crystalnexus.world.inventory.SolarSimulatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class SolarSimulatorScreen extends AbstractContainerScreen<SolarSimulatorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/solar_sim_gui.png");
    private static final ResourceLocation COMET = ResourceLocation.parse("crystalnexus:textures/screens/comet.png");
    public SolarSimulatorScreen(SolarSimulatorMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 181; }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        if (menu.controller().isRenderActive() && minecraft != null && minecraft.level != null) {
            float rotation = (minecraft.level.getGameTime() + partialTick) * 4.0F % 360.0F;
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos + 88.0F, topPos + 46.0F, 0.0F);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
            graphics.blit(COMET, -37, -37, 0, 0, 74, 74, 74, 74);
            graphics.pose().popPose();
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY); }
}
