package net.crystalnexus.client.gui;

import net.crystalnexus.block.entity.PlasmaGeneratorControllerBlockEntity;
import net.crystalnexus.energy.PlasmaGrid;
import net.crystalnexus.world.inventory.PlasmaGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlasmaGeneratorScreen extends AbstractContainerScreen<PlasmaGeneratorMenu> {
    private static final int FLUID_X = 185, FLUID_Y = 126, FLUID_WIDTH = 18, FLUID_HEIGHT = 82;

    public PlasmaGeneratorScreen(PlasmaGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 356;
        imageHeight = 225;
        inventoryLabelX = 8;
        inventoryLabelY = 132;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ControllerGuiStyle.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        PlasmaGeneratorControllerBlockEntity controller = menu.controller();
        for (var slot : menu.slots) {
            ControllerGuiStyle.inset(graphics, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
            if (controller != null && slot.index < PlasmaGrid.SIZE) {
                double fraction = controller.getHeat(slot.index) / PlasmaGrid.HEAT_LIMIT;
                if (fraction > 0) graphics.fill(leftPos + slot.x, topPos + slot.y + 14,
                    leftPos + slot.x + Math.max(1, (int) (16 * fraction)), topPos + slot.y + 16,
                    fraction >= 0.75 ? 0xffff3030 : 0xffffa030);
            }
        }
        if (controller != null) {
            int x = leftPos + FLUID_X, y = topPos + FLUID_Y;
            ControllerGuiStyle.inset(graphics, x - 1, y - 1, FLUID_WIDTH + 2, FLUID_HEIGHT + 2);
            FluidTankRenderer.draw(graphics, controller.getArgonTank().getFluid(),
                PlasmaGeneratorControllerBlockEntity.TANK_CAPACITY, x, y, FLUID_WIDTH, FLUID_HEIGHT);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 9, 0xff404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xff404040, false);
        PlasmaGeneratorControllerBlockEntity controller = menu.controller();
        if (controller == null) return;
        PlasmaGrid.Result grid = controller.getGrid();
        int color = controller.isOperating() ? 0xff216b35 : controller.isFormed() ? 0xff805500 : 0xffa02020;
        graphics.drawWordWrap(font, Component.literal(controller.getStatus()), 181, 29, 166, color);
        graphics.drawString(font, "Output: " + controller.getOutputPerTick() + " FE/t", 181, 60, 0xff205b78, false);
        graphics.drawString(font, "Demand: " + grid.argonPerTick() + " mB/t", 181, 76, 0xff713c8e, false);
        graphics.drawString(font, "Hottest #" + (controller.getHottestSlot() + 1) + ": " + number(controller.getHottestHeat()) + " / " + number(PlasmaGrid.HEAT_LIMIT), 181, 92, 0xffa02020, false);
        graphics.drawWordWrap(font, Component.literal("Argon: " + controller.getArgonTank().getFluidAmount()
            + " / " + PlasmaGeneratorControllerBlockEntity.TANK_CAPACITY + " mB"), 212, 128, 133, 0xff713c8e);
        graphics.drawWordWrap(font, Component.literal("Heatsinks reach 8 neighbors; other components reach 4. Hover to see affected slots."),
            212, 158, 133, 0xff404040);
    }

    private static String number(double value) { return String.format(Locale.ROOT, "%.1f", value); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredSlot != null && hoveredSlot.index < PlasmaGrid.SIZE) {
            var stack = menu.getCarried().isEmpty() ? hoveredSlot.getItem() : menu.getCarried();
            int component = PlasmaGeneratorControllerBlockEntity.componentType(stack);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            for (int affected : PlasmaGrid.affectedSlots(hoveredSlot.index, component)) {
                var target = menu.slots.get(affected);
                graphics.fill(leftPos + target.x, topPos + target.y,
                    leftPos + target.x + 16, topPos + target.y + 16, 0x6633ff66);
            }
            graphics.pose().popPose();
        }
        PlasmaGeneratorControllerBlockEntity controller = menu.controller();
        if (controller != null && hoveredSlot != null && hoveredSlot.index < PlasmaGrid.SIZE) {
            int slot = hoveredSlot.index;
            PlasmaGrid.Result grid = controller.getGrid();
            PlasmaGrid.Cell cell = grid.cells().get(slot);
            List<Component> lines = new ArrayList<>();
            lines.add(hoveredSlot.hasItem() ? hoveredSlot.getItem().getHoverName() : Component.literal("Component slot"));
            lines.add(Component.literal("Heat: " + number(controller.getHeat(slot)) + " / " + number(PlasmaGrid.HEAT_LIMIT)));
            if (PlasmaGrid.isInjector(cell.component())) {
                lines.add(Component.literal("Argon / plasma: " + number(cell.throughput()) + " per tick"));
                lines.add(Component.literal("Coil extraction stack: " + number(cell.extractionMultiplier()) + "x"));
                lines.add(Component.literal("Heat generated (incl. FE): " + number(cell.heatGenerated()) + "/t"));
                lines.add(Component.literal("Allocated cooling: " + number(cell.cooling()) + "/t"));
                lines.add(Component.literal("Passive cooling: " + number(PlasmaGrid.PASSIVE_COOLING
                    + controller.getHeat(slot) * PlasmaGrid.PASSIVE_COOLING_FRACTION) + "/t"));
            } else if (cell.component() == PlasmaGrid.HEATSINK) {
                int injectors = 0;
                for (int neighbor : PlasmaGrid.affectedSlots(slot, PlasmaGrid.HEATSINK))
                    if (PlasmaGrid.isInjector(grid.cells().get(neighbor).component())) injectors++;
                lines.add(Component.literal("Cooling: up to " + PlasmaGrid.HEATSINK_COOLING + " heat/t per adjacent Injector (" + injectors + ")"));
                lines.add(Component.literal("Cooling stacks from multiple Heatsinks"));
                lines.add(Component.literal("All 8 neighboring Coils lose 20% efficiency each (minimum 0%)"));
            } else if (cell.component() == PlasmaGrid.COIL) {
                lines.add(Component.literal("Plasma received: " + number(cell.plasmaReceived()) + "/t"));
                lines.add(Component.literal("Processed: " + number(cell.plasmaProcessed()) + " / " + PlasmaGrid.COIL_CAPACITY + " per tick"));
                lines.add(Component.literal("Efficiency: " + number(cell.efficiency() * 100) + "%"));
                lines.add(Component.literal("Injector extraction stack: " + number(cell.extractionMultiplier()) + "x"));
                lines.add(Component.literal("Each Coil reduces Injectors by 0.5 mB/t; High Flow Injectors by 1 mB/t"));
            } else lines.add(Component.literal("One Injector, High Flow Injector, Heatsink, or Coil; live editing allowed"));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (controller != null && isHovering(FLUID_X, FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.literal("Argon"),
                Component.literal(controller.getArgonTank().getFluidAmount() + " / "
                    + PlasmaGeneratorControllerBlockEntity.TANK_CAPACITY + " mB")), mouseX, mouseY);
        } else renderTooltip(graphics, mouseX, mouseY);
    }
}
