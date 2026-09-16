package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.block.entity.HemolyzerBlockEntity;
import net.crystalnexus.world.inventory.HemolyzerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class HemolyzerScreen extends AbstractContainerScreen<HemolyzerMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.parse("crystalnexus:textures/screens/hemolyzer_gui.png");
    private static final ResourceLocation BATTERY = ResourceLocation.parse("crystalnexus:textures/screens/batterylevelsmall.png");
    private static final ResourceLocation PROGRESS = ResourceLocation.parse("crystalnexus:textures/screens/flesh_progress_bar.png");
    private static final int FLUID_X = 134, FLUID_Y = 26, FLUID_WIDTH = 15, FLUID_HEIGHT = 33;

    public HemolyzerScreen(HemolyzerMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 166; }
    private HemolyzerBlockEntity hemolyzer() { return menu.getSlot(0).container instanceof HemolyzerBlockEntity hemolyzer ? hemolyzer : null; }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        HemolyzerBlockEntity hemolyzer = hemolyzer();
        int energyFrame = hemolyzer == null ? 0 : Mth.clamp(hemolyzer.getEnergyStorage().getEnergyStored() * 10 / HemolyzerBlockEntity.ENERGY_CAPACITY, 0, 10);
        int progressFrame = Mth.clamp(menu.progress() * 10 / menu.maxProgress(), 0, 10);
        graphics.blit(BATTERY, leftPos + 17, topPos + 27, 0, energyFrame * 32, 32, 32, 32, 352);
        graphics.blit(PROGRESS, leftPos + 99, topPos + 26, 0, progressFrame * 32, 32, 32, 32, 352);
        if (hemolyzer != null) FluidTankRenderer.draw(graphics, hemolyzer.getBloodTank().getFluid(), HemolyzerBlockEntity.BLOOD_TANK_CAPACITY,
            leftPos + FLUID_X, topPos + FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        HemolyzerBlockEntity hemolyzer = hemolyzer();
        if (hemolyzer != null && isHovering(FLUID_X, FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT, mouseX, mouseY)) {
            FluidStack fluid = hemolyzer.getBloodTank().getFluid();
            graphics.renderComponentTooltip(font, List.of(fluid.isEmpty() ? Component.literal("Empty") : fluid.getHoverName(),
                Component.literal(fluid.getAmount() + " / " + HemolyzerBlockEntity.BLOOD_TANK_CAPACITY + " mB")), mouseX, mouseY);
        }
        else renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { }
}
