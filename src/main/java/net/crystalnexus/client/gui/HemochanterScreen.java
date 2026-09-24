package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.block.entity.HemochanterBlockEntity;
import net.crystalnexus.world.inventory.HemochanterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;

/** Reuses the Hemolyzer's compact energy, progress, and blood layout. */
public final class HemochanterScreen extends AbstractContainerScreen<HemochanterMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.parse("crystalnexus:textures/screens/hemolyzer_gui.png");
    private static final ResourceLocation BATTERY = ResourceLocation.parse("crystalnexus:textures/screens/batterylevelsmall.png");
    private static final ResourceLocation PROGRESS = ResourceLocation.parse("crystalnexus:textures/screens/flesh_progress_bar.png");
    private static final int FLUID_X = 134, FLUID_Y = 26, FLUID_WIDTH = 15, FLUID_HEIGHT = 33;
    public HemochanterScreen(HemochanterMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 166; }
    private HemochanterBlockEntity hemochanter() { return menu.getSlot(0).container instanceof HemochanterBlockEntity hemochanter ? hemochanter : null; }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F); graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        HemochanterBlockEntity hemochanter = hemochanter();
        int energy = hemochanter == null ? 0 : Mth.clamp(hemochanter.getEnergyStorage().getEnergyStored() * 10 / HemochanterBlockEntity.ENERGY_CAPACITY, 0, 10);
        graphics.blit(BATTERY, leftPos + 17, topPos + 27, 0, energy * 32, 32, 32, 32, 352);
        graphics.blit(PROGRESS, leftPos + 99, topPos + 26, 0, Mth.clamp(menu.progress() * 10 / menu.maxProgress(), 0, 10) * 32, 32, 32, 32, 352);
        if (hemochanter != null) FluidTankRenderer.draw(graphics, hemochanter.getBloodTank().getFluid(), HemochanterBlockEntity.BLOOD_TANK_CAPACITY, leftPos + FLUID_X, topPos + FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick); HemochanterBlockEntity hemochanter = hemochanter();
        if (hemochanter != null && isHovering(FLUID_X, FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT, mouseX, mouseY)) { FluidStack fluid = hemochanter.getBloodTank().getFluid(); graphics.renderComponentTooltip(font, List.of(fluid.isEmpty() ? Component.literal("Empty") : fluid.getHoverName(), Component.literal(fluid.getAmount() + " / " + HemochanterBlockEntity.BLOOD_TANK_CAPACITY + " mB")), mouseX, mouseY); } else renderTooltip(graphics, mouseX, mouseY);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { }
}
