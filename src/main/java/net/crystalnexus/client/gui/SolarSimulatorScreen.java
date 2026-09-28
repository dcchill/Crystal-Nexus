package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.crystalnexus.world.inventory.SolarSimulatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class SolarSimulatorScreen extends AbstractContainerScreen<SolarSimulatorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/solar_sim_gui.png");
    private static final ResourceLocation DYSON_TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/solar_sim_dyson_gui.png");
    private static final ResourceLocation COMET = ResourceLocation.parse("crystalnexus:textures/screens/comet.png");
    public SolarSimulatorScreen(SolarSimulatorMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 181; }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.crystalnexus.solar_simulator.mode"), button -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 5, topPos + 5, 72, 18).build());
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        ResourceLocation texture = menu.controller() != null && menu.controller().isDysonMode() ? DYSON_TEXTURE : TEXTURE;
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        if (menu.controller() != null && !menu.controller().isDysonMode() && menu.controller().isRenderActive() && minecraft != null && minecraft.level != null) {
            float rotation = (minecraft.level.getGameTime() + partialTick) * 4.0F % 360.0F;
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos + 88.0F, topPos + 46.0F, 0.0F);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
            graphics.blit(COMET, -37, -37, 0, 0, 74, 74, 74, 74);
            graphics.pose().popPose();
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.controller() == null) return;
        boolean dyson = menu.controller().isDysonMode();
        if (dyson) {
            graphics.drawString(font, Component.literal(formatRate(menu.controller().getDysonPotentialFePerTick())), 104, 11, 0xFFFFFF, false);
            graphics.drawString(font, Component.translatable("gui.crystalnexus.solar_simulator.integrity",
                menu.controller().getDysonIntegrity(), net.crystalnexus.recipe.DysonOutput.MAX_INTEGRITY), 5, 81, 0xFFFFFF, false);
        }
    }
    private static String formatRate(int rate) {
        if (rate >= 1_000_000) return String.format(java.util.Locale.ROOT, "%.1fM FE/t", rate / 1_000_000.0);
        if (rate >= 1_000) return String.format(java.util.Locale.ROOT, "%.1fK FE/t", rate / 1_000.0);
        return rate + " FE/t";
    }
    @Override protected void renderSlotContents(GuiGraphics graphics, ItemStack stack, Slot slot, String countString) {
        if (menu.controller() != null && menu.controller().isDysonMode() && slot.index < net.crystalnexus.block.entity.SolarSimulatorControllerBlockEntity.DYSON_SUPPLY_SLOTS && !stack.isEmpty()) {
            graphics.renderItem(stack, slot.x, slot.y);
            graphics.renderItemDecorations(font, stack, slot.x, slot.y, Integer.toString(menu.controller().getDysonCount(slot.index)));
            return;
        }
        super.renderSlotContents(graphics, stack, slot, countString);
    }
    @Override protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromContainerItem(stack);
        if (menu.controller() != null && menu.controller().isDysonMode() && hoveredSlot != null && hoveredSlot.index < net.crystalnexus.block.entity.SolarSimulatorControllerBlockEntity.DYSON_SUPPLY_SLOTS)
            tooltip.add(Component.literal(menu.controller().getDysonCount(hoveredSlot.index) + " / 1024"));
        return tooltip;
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY); }
}
