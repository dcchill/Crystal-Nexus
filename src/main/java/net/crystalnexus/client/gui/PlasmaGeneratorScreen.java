package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.block.entity.PlasmaGeneratorControllerBlockEntity;
import net.crystalnexus.world.inventory.PlasmaGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class PlasmaGeneratorScreen extends AbstractContainerScreen<PlasmaGeneratorMenu> {
    private static final int FLUID_X = 20, FLUID_Y = 37, FLUID_WIDTH = 24, FLUID_HEIGHT = 52;

    public PlasmaGeneratorScreen(PlasmaGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 112;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff0b0714);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xff211436);
        graphics.fill(leftPos + 8, topPos + 27, leftPos + imageWidth - 8, topPos + imageHeight - 10, 0xff120b20);
        PlasmaGeneratorControllerBlockEntity controller = menu.controller();
        if (controller != null) drawFluid(graphics, controller.getArgonTank().getFluid());
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        PlasmaGeneratorControllerBlockEntity controller = menu.controller();
        graphics.drawString(font, title, 9, 9, 0xffd8b7ff, false);
        if (controller == null) return;
        int statusColor = controller.isOperating() ? 0xff8cffd5
            : controller.isFormed() ? 0xffffc96b : 0xffff6b82;
        graphics.drawString(font, Component.literal("Status: " + controller.getStatus()), 56, 37, statusColor, false);
        graphics.drawString(font, Component.literal("Output: " + controller.getOutputPerTick() + " FE/t"), 56, 55, 0xff80dfff, false);
        graphics.drawString(font, Component.literal("Argon: " + controller.getArgonTank().getFluidAmount()
            + " / " + PlasmaGeneratorControllerBlockEntity.TANK_CAPACITY + " mB"), 56, 73, 0xffd89cff, false);
    }

    private void drawFluid(GuiGraphics graphics, FluidStack fluid) {
        int x = leftPos + FLUID_X;
        int y = topPos + FLUID_Y;
        graphics.fill(x - 2, y - 2, x + FLUID_WIDTH + 2, y + FLUID_HEIGHT + 2, 0xff6b3f91);
        graphics.fill(x, y, x + FLUID_WIDTH, y + FLUID_HEIGHT, 0xff08050d);
        if (fluid.isEmpty()) return;
        int height = Math.max(1, fluid.getAmount() * FLUID_HEIGHT / PlasmaGeneratorControllerBlockEntity.TANK_CAPACITY);
        IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation still = extension.getStillTexture(fluid);
        if (still == null) return;
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(still.getNamespace(), "textures/" + still.getPath() + ".png");
        int tint = extension.getTintColor(fluid);
        RenderSystem.setShaderColor(((tint >> 16) & 255) / 255F, ((tint >> 8) & 255) / 255F,
            (tint & 255) / 255F, 1F);
        int bottom = y + FLUID_HEIGHT;
        graphics.enableScissor(x, bottom - height, x + FLUID_WIDTH, bottom);
        for (int drawY = bottom - height; drawY < bottom; drawY += 16)
            for (int drawX = x; drawX < x + FLUID_WIDTH; drawX += 16)
                graphics.blit(texture, drawX, drawY, 0, 0, 16, 16, 16, 16);
        graphics.disableScissor();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (menu.controller() != null && isHovering(FLUID_X, FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.literal("Argon"),
                Component.literal(menu.controller().getArgonTank().getFluidAmount() + " / "
                    + PlasmaGeneratorControllerBlockEntity.TANK_CAPACITY + " mB")), mouseX, mouseY);
        } else renderTooltip(graphics, mouseX, mouseY);
    }
}
