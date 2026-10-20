package net.crystalnexus.client.gui;

import net.crystalnexus.jei_recipes.PartsAssemblingRecipe;
import net.crystalnexus.world.inventory.PartsAssemblerMenu;
import net.crystalnexus.world.inventory.MachineGuiLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class PartsAssemblerScreen extends HolographicMachineScreen<PartsAssemblerMenu> {
    private static final ResourceLocation[] MODE_TEXTURES = {
        ResourceLocation.parse("crystalnexus:textures/item/placeholdersheet.png"),
        ResourceLocation.parse("crystalnexus:textures/item/placeholderrod.png"),
        ResourceLocation.parse("crystalnexus:textures/item/placeholderbolt.png")
    };

    public PartsAssemblerScreen(PartsAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        machineBackground(graphics);
        machineProgress(graphics, (double) menu.progress() / menu.maxProgress());
        for (int mode = 0; mode < MODE_TEXTURES.length; mode++) {
            var r = MachineGuiLayout.assemblerMode(mode);
            MachineGuiTheme.frame(graphics, leftPos + r.x(), topPos + r.y(), r.width(), r.height(),
                menu.selectedMode() == mode ? MachineGuiTheme.accent(machineTier()) : 0x52788F);
            graphics.blit(MODE_TEXTURES[mode], leftPos + r.x() + 1, topPos + r.y() + 1, 0, 0, 16, 16, 16, 16);
        }

        MachineGuiWidgets.finishBackground(graphics);
    }

    @Override protected boolean machineMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) for (int mode = 0; mode < MODE_TEXTURES.length; mode++) {
            if (MachineGuiLayout.assemblerMode(mode).contains(mouseX - leftPos, mouseY - topPos)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, mode);
                return true;
            }
        }
        return super.machineMouseClicked(mouseX, mouseY, button);
    }

    @Override protected void renderMachine(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderMachine(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (int mode = 0; mode < MODE_TEXTURES.length; mode++) if (MachineGuiLayout.assemblerMode(mode).contains(mouseX - leftPos, mouseY - topPos)) {
            var selected = PartsAssemblingRecipe.Mode.fromId(mode);
            graphics.renderTooltip(font, Component.translatable("gui.crystalnexus.parts_assembler.mode." + selected.serializedName()), mouseX, mouseY);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
    @Override protected BlockPos machinePosition() { return menu.guiPosition(); }
    @Override protected String machineFallbackId() { return "parts_assembler"; }
}
