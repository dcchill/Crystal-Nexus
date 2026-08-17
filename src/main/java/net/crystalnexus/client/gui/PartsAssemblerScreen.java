package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.jei_recipes.PartsAssemblingRecipe;
import net.crystalnexus.world.inventory.PartsAssemblerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public final class PartsAssemblerScreen extends AbstractContainerScreen<PartsAssemblerMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.parse("crystalnexus:textures/screens/iron_smelter_gui.png");
    private static final ResourceLocation BATTERY = ResourceLocation.parse("crystalnexus:textures/screens/batterylevelsmall.png");
    private static final ResourceLocation PROGRESS = ResourceLocation.parse("crystalnexus:textures/screens/progressbarsmelt.png");
    private static final ResourceLocation[] MODE_TEXTURES = {
        ResourceLocation.parse("crystalnexus:textures/item/placeholdersheet.png"),
        ResourceLocation.parse("crystalnexus:textures/item/placeholderrod.png"),
        ResourceLocation.parse("crystalnexus:textures/item/placeholderbolt.png")
    };
    private static final int SELECTOR_X = 78;
    private static final int SELECTOR_Y = 57;
    private boolean dropdownOpen;

    public PartsAssemblerScreen(PartsAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/upgradeslot.png"), leftPos + 173, topPos, 0, 0, 32, 32, 32, 32);
        graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/battery_addon.png"), leftPos - 33, topPos - 1, 0, 0, 48, 48, 48, 48);
        graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), leftPos + 50, topPos - 15, 0, 0, 126, 18, 126, 18);

        int energyFrame = Mth.clamp((int) ((double) menu.energy() / menu.maxEnergy() * 10), 0, 10) * 32;
        int progressFrame = Mth.clamp((int) ((double) menu.progress() / menu.maxProgress() * 10), 0, 10) * 32;
        graphics.blit(BATTERY, leftPos - 25, topPos + 5, 0, energyFrame, 32, 32, 32, 352);
        graphics.blit(PROGRESS, leftPos + 72, topPos + 27, 0, progressFrame, 32, 32, 32, 352);

        drawModeOption(graphics, SELECTOR_X, SELECTOR_Y, menu.selectedMode(), true);
    }

    private void drawModeOption(GuiGraphics graphics, int x, int y, int mode, boolean arrow) {
        int screenX = leftPos + x;
        int screenY = topPos + y;
        graphics.fill(screenX, screenY, screenX + 38, screenY + 18, 0xff8b8b8b);
        graphics.fill(screenX + 1, screenY + 1, screenX + 37, screenY + 17, 0xffc6c6c6);
        graphics.blit(MODE_TEXTURES[Mth.clamp(mode, 0, 2)], screenX + 2, screenY + 1, 0, 0, 16, 16, 16, 16);
        if (arrow) graphics.drawString(font, dropdownOpen ? "▲" : "▼", screenX + 25, screenY + 5, 0xff303030, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (inside(x, y, SELECTOR_X, SELECTOR_Y, 38, 18)) {
            dropdownOpen = !dropdownOpen;
            return true;
        }
        if (dropdownOpen) {
            for (int mode = 0; mode < 3; mode++) {
                if (inside(x, y, SELECTOR_X, SELECTOR_Y + 19 + mode * 18, 38, 18)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, mode);
                    dropdownOpen = false;
                    return true;
                }
            }
            dropdownOpen = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (dropdownOpen) {
            for (int mode = 0; mode < 3; mode++) drawModeOption(graphics, SELECTOR_X, SELECTOR_Y + 19 + mode * 18, mode, false);
        }
        renderTooltip(graphics, mouseX, mouseY);
        if (inside(mouseX - leftPos, mouseY - topPos, SELECTOR_X, SELECTOR_Y, 38, 18)) {
            PartsAssemblingRecipe.Mode mode = PartsAssemblingRecipe.Mode.fromId(menu.selectedMode());
            graphics.renderTooltip(font, Component.translatable("gui.crystalnexus.parts_assembler.mode." + mode.serializedName()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("block.crystalnexus.parts_assembler"), 83, -11, 0xff3c3c3c, false);
    }
}
