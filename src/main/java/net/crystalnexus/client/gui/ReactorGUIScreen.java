package net.crystalnexus.client.gui;

import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.crystalnexus.reactor.ReactorGraphHistory;
import net.crystalnexus.reactor.ReactorBalance;
import net.crystalnexus.world.inventory.ReactorGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ReactorGUIScreen extends HolographicMachineScreen<ReactorGUIMenu> {
    private static final int GRAPH_X = 8, GRAPH_Y = 268, GRAPH_WIDTH = 228, GRAPH_HEIGHT = 38;
    private static final net.crystalnexus.world.inventory.MachineGuiLayout.Rect MASTER_SLIDER =
        new net.crystalnexus.world.inventory.MachineGuiLayout.Grid(8, 140).control(224, 32, 18, 108);
    private static final int MASTER_SLIDER_X = MASTER_SLIDER.x() + 8, MASTER_SLIDER_Y = MASTER_SLIDER.y() + 4, MASTER_SLIDER_HEIGHT = MASTER_SLIDER.height() - 8;
    private Button previous;
    private Button next;
    private Button ratesTab;
    private Button temperatureTab;
    private Button feTab;
    private enum GraphTab { HEAT, TEMPERATURE, FE }
    private GraphTab graphTab = GraphTab.HEAT;
    private int hoveredSample = -1;
	private boolean draggingMasterSlider;
    private int selectedMasterInsertion;
    private final ReactorGraphHistory graphHistory;

    public ReactorGUIScreen(ReactorGUIMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        graphHistory = menu.graphHistory();
        imageWidth = 244;
        imageHeight = 314;
    }

    @Override protected void init() {
        super.init();
        topPos = Math.min(Math.max(15, topPos), Math.max(0, height - imageHeight));
        previous = themedButton("<", 8, 7, 18, button -> changePage(menu.page() - 1));
        next = themedButton(">", 150, 7, 18, button -> changePage(menu.page() + 1));
        ratesTab = themedButton("Heat & cooling", 8, 230, 90, button -> selectGraph(GraphTab.HEAT));
        temperatureTab = themedButton("Temperature", 98, 230, 72, button -> selectGraph(GraphTab.TEMPERATURE));
        feTab = themedButton("FE output", 170, 230, 54, button -> selectGraph(GraphTab.FE));
        selectGraph(graphTab);
        updateButtons();
		selectedMasterInsertion = menu.masterInsertion();
    }

    private void changePage(int page) {
        if (page < 0 || page >= menu.pageCount()) return;
        menu.setClientPage(page);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
        updateButtons();
		if (!draggingMasterSlider) selectedMasterInsertion = menu.masterInsertion();
    }

    private void selectGraph(GraphTab tab) {
        graphTab = tab;
        ratesTab.active = tab != GraphTab.HEAT;
        temperatureTab.active = tab != GraphTab.TEMPERATURE;
        feTab.active = tab != GraphTab.FE;
    }

    private Button themedButton(String label, int x, int y, int width, Button.OnPress action) {
        return addRenderableWidget(new Button(leftPos + x, topPos + y, width, 18, Component.literal(label), action, supplier -> supplier.get()) {
            @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                int accent = MachineGuiTheme.accent(machineTier());
                MachineGuiTheme.frame(graphics, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused() ? 0x7de8ff : accent);
                graphics.drawCenteredString(font, font.plainSubstrByWidth(getMessage().getString(), getWidth() - 6),
                    getX() + getWidth() / 2, getY() + (getHeight() - font.lineHeight) / 2,
                    active ? 0xffd7e6ed : 0xff517387);
            }
        });
    }

    private void updateButtons() {
        previous.active = menu.page() > 0;
        next.active = menu.page() + 1 < menu.pageCount();
    }

    @Override protected void containerTick() {
        super.containerTick();
        updateButtons();
        if (menu.controller() != null) {
            var data = menu.controller().getPersistentData();
            graphHistory.tick(data.getDouble("heatGenerated"), data.getDouble("maxCooling"),
                Math.max(ReactorBalance.AMBIENT_TEMPERATURE, data.getDouble("heat")), data.getDouble("lastFEOutput"), data.getDouble("maxFEt"));
        }
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        machineBackground(graphics);

        for (int row = 0; row < 9; row++) {
            if (menu.rodPosition(row) == null) continue;
            for (int cell = 0; cell < 3; cell++) {
                var slot = menu.slots.get(2 + row * 3 + cell);
                MachineGuiTheme.slotFrame(graphics, leftPos + slot.x - 1, topPos + slot.y - 1, 0x517387);
            }
        }
        ReactorComputerBlockEntity controller = menu.controller();
        if (controller != null) {
			machineTank(graphics, controller.getFluidTank().getFluid(), 64000,
                leftPos + 182, topPos + 81, 14, 48);
            var data = controller.getPersistentData();
            var graphBounds = new net.crystalnexus.world.inventory.MachineGuiLayout.Rect(leftPos + GRAPH_X, topPos + GRAPH_Y, GRAPH_WIDTH, GRAPH_HEIGHT);
            hoveredSample = graphBounds.contains(mouseX, mouseY)
                ? graphHistory.sampleAt((mouseX - leftPos - GRAPH_X - 1) / (double) (GRAPH_WIDTH - 3)) : -1;
            if (hoveredSample >= 0) {
                Component age = Component.literal(String.format(java.util.Locale.ROOT, "Sample: %.1f seconds ago", graphHistory.secondsAgo(hoveredSample)));
                if (graphTab == GraphTab.FE) {
                    MachineGuiWidgets.value(graphBounds, age,
                        Component.literal(String.format(java.util.Locale.ROOT, "Actual grid output: %.2f FE/t", graphHistory.feOutput(hoveredSample))),
                        Component.literal(String.format(java.util.Locale.ROOT, "Maximum generation: %.2f FE/t", graphHistory.maxFe(hoveredSample))),
                        Component.literal("Stored energy can temporarily raise output above generation capacity."));
                } else if (graphTab == GraphTab.TEMPERATURE) {
                    MachineGuiWidgets.value(graphBounds, age,
                        Component.literal(String.format(java.util.Locale.ROOT, "Reactor temperature: %.2f \u00b0C", graphHistory.temperature(hoveredSample))));
                } else {
                    MachineGuiWidgets.value(graphBounds, age,
                        Component.literal(String.format(java.util.Locale.ROOT, "Heat generated: %.2f per tick", graphHistory.heat(hoveredSample))),
                        Component.literal(String.format(java.util.Locale.ROOT, "Maximum cooling: %.2f per tick", graphHistory.coolingCapacity(hoveredSample))),
                        Component.literal("Dashed line: maximum cooling available at this point."));
                }
            } else {
                MachineGuiWidgets.value(graphBounds,
                    Component.literal("Hover over recorded graph points to inspect their values."));
            }
            MachineGuiWidgets.value(new net.crystalnexus.world.inventory.MachineGuiLayout.Rect(leftPos + 88, topPos + 103, 88, 9),
                Component.literal("Current reactor temperature: " + (int) data.getDouble("heat") + " °C"));
        }
		int sliderX = leftPos + MASTER_SLIDER_X;
		int sliderY = topPos + MASTER_SLIDER_Y;
		graphics.fill(leftPos + MASTER_SLIDER.x(), topPos + MASTER_SLIDER.y(), leftPos + MASTER_SLIDER.right(), topPos + MASTER_SLIDER.bottom(), 0xff252a35);
		graphics.fill(sliderX, sliderY, sliderX + 2, sliderY + MASTER_SLIDER_HEIGHT, 0xffaeb4c2);
		int knobY = sliderY + selectedMasterInsertion * MASTER_SLIDER_HEIGHT / 100;
		graphics.fill(sliderX - 5, knobY - 3, sliderX + 7, knobY + 4, 0xffdca8ff);
		graphics.renderOutline(sliderX - 5, knobY - 3, 12, 7, 0xffffffff);
        MachineGuiWidgets.value(new net.crystalnexus.world.inventory.MachineGuiLayout.Rect(leftPos + MASTER_SLIDER.x(), topPos + MASTER_SLIDER.y(), MASTER_SLIDER.width(), MASTER_SLIDER.height()),
            Component.literal("Control rod insertion: " + selectedMasterInsertion + "%"));

        MachineGuiWidgets.finishBackground(graphics);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component name = Component.translatable("block.crystalnexus.reactor_computer");
        Component page = Component.literal((menu.page() + 1) + " / " + menu.pageCount());
        graphics.drawString(font, page, 88 - font.width(page) / 2, 12, 0xE3F3FF, false);
        ReactorComputerBlockEntity controller = menu.controller();
        if (controller != null) {
            var data = controller.getPersistentData();
            MachineGuiWidgets.label(graphics, font, Component.literal(font.plainSubstrByWidth("Status: " + data.getString("reactorStatus"), 160)),
                8, 91, 0xE3F3FF, false);
            MachineGuiWidgets.label(graphics, font, Component.literal("FE/t " + compact(data.getDouble("lastFEt"))), 8, 103, 0xFF8BD8FF, false);
            MachineGuiWidgets.label(graphics, font, Component.literal("Temp: " + (int) data.getDouble("heat") + " °C"), 88, 103, 0xffa04520, false);
            MachineGuiWidgets.label(graphics, font, Component.literal("Energy " + compact(controller.getEnergyStorage().getEnergyStored())), 8, 115, 0xFF8BD8FF, false);
            MachineGuiWidgets.label(graphics, font, Component.literal("Coolant " + compact(controller.getFluidTank().getFluidAmount()) + " mB"), 88, 115, 0xFF8BD8FF, false);
			MachineGuiWidgets.label(graphics, font, Component.literal("Heat generated: " + compact(data.getDouble("heatGenerated")) + "/t"),
					8, 127, 0xffa04520, false);
        }
		graphics.drawString(font, Component.literal("All rods"), 200, MASTER_SLIDER.bottom() + 6, 0xE3F3FF, false);
		graphics.drawString(font, Component.literal(selectedMasterInsertion + "%"), 218, MASTER_SLIDER.bottom() + 16, 0xE3F3FF, false);
        renderGraph(graphics);
    }

    private void renderGraph(GuiGraphics graphics) {
        int x = GRAPH_X, y = GRAPH_Y, width = GRAPH_WIDTH, height = GRAPH_HEIGHT;
        if (graphTab == GraphTab.FE) {
            graphics.drawString(font, "FE output/t", x, 252, 0xFFAD55, false);
            graphics.drawString(font, "Max FE/t", x + 78, 252, 0x62DBFF, false);
        } else if (graphTab == GraphTab.TEMPERATURE) {
            graphics.drawString(font, "Temperature (\u00b0C)", x, 252, 0xFFAD55, false);
        } else {
            String heatLabel = "Heat generated/t";
            graphics.drawString(font, heatLabel, x, 252, 0xFFAD55, false);
            graphics.drawString(font, "Max cooling/t", x + font.width(heatLabel) + 10, 252, 0x62DBFF, false);
        }
        String duration = "Last 60s";
        graphics.drawString(font, duration, x + width - font.width(duration), 252, 0xE3F3FF, false);
        graphics.fill(x, y, x + width, y + height, 0xFF0B1625);
        graphics.renderOutline(x, y, width, height, 0xFF517387);
        double maximum = graphTab == GraphTab.TEMPERATURE ? ReactorBalance.MAX_TEMPERATURE : 1;
        int historyCount = graphHistory.size();
        int seriesCount = graphTab == GraphTab.TEMPERATURE ? 1 : 2;
        for (int i = 0; i < historyCount; i++)
            for (int series = 0; series < seriesCount; series++) maximum = Math.max(maximum, graphValue(series, i));
        int hoverX = x + 1 + (ReactorGraphHistory.CAPACITY - historyCount + hoveredSample) * (width - 3) / (ReactorGraphHistory.CAPACITY - 1);
        if (hoveredSample >= 0) graphics.fill(hoverX, y + 1, hoverX + 1, y + height - 1, 0x60517387);
        for (int series = 0; series < seriesCount; series++) {
            int color = series == 0 ? 0xFFFFAD55 : 0xFF62DBFF;
            int linePixels = 0;
            for (int i = 1; i < historyCount; i++) {
                double previous = graphValue(series, i - 1);
                double current = graphValue(series, i);
                int x0 = x + 1 + (ReactorGraphHistory.CAPACITY - historyCount + i - 1) * (width - 3) / (ReactorGraphHistory.CAPACITY - 1);
                int x1 = x + 1 + (ReactorGraphHistory.CAPACITY - historyCount + i) * (width - 3) / (ReactorGraphHistory.CAPACITY - 1);
                int y0 = y + height - 2 - (int) Math.round(previous / maximum * (height - 3));
                int y1 = y + height - 2 - (int) Math.round(current / maximum * (height - 3));
                int steps = Math.max(x1 - x0, Math.abs(y1 - y0));
                for (int step = 0; step <= steps; step++) {
                    int px = x0 + (x1 - x0) * step / steps;
                    int py = y0 + (y1 - y0) * step / steps;
                    if (series == 0 || linePixels % 6 < 3)
                        graphics.fill(px, py, px + 1, py + 1, color);
                    linePixels++;
                }
            }
            if (hoveredSample >= 0) {
                int hoverY = y + height - 2 - (int) Math.round(graphValue(series, hoveredSample) / maximum * (height - 3));
                graphics.fill(hoverX - 1, hoverY - 1, hoverX + 2, hoverY + 2, color);
            }
        }
        graphics.drawString(font, compact(maximum) + (graphTab == GraphTab.TEMPERATURE ? " \u00b0C" : ""), x + 2, y + 2, 0xA9BDCF, false);
    }

    private double graphValue(int series, int sample) {
        if (graphTab == GraphTab.TEMPERATURE) return graphHistory.temperature(sample);
        if (graphTab == GraphTab.FE) return series == 0 ? graphHistory.feOutput(sample) : graphHistory.maxFe(sample);
        return series == 0 ? graphHistory.heat(sample) : graphHistory.coolingCapacity(sample);
    }

    @Override protected void renderMachine(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderMachine(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

	@Override protected boolean machineMouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && masterSliderContains(mouseX, mouseY)) {
			draggingMasterSlider = true;
			updateMasterSlider(mouseY);
			return true;
		}
		return super.machineMouseClicked(mouseX, mouseY, button);
	}

	@Override protected boolean machineMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (draggingMasterSlider && button == 0) {
			updateMasterSlider(mouseY);
			return true;
		}
		return super.machineMouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override protected boolean machineMouseReleased(double mouseX, double mouseY, int button) {
		draggingMasterSlider = false;
		return super.machineMouseReleased(mouseX, mouseY, button);
	}

	private boolean masterSliderContains(double mouseX, double mouseY) {
		return MASTER_SLIDER.contains(mouseX - leftPos, mouseY - topPos);
	}

	private void updateMasterSlider(double mouseY) {
		int insertion = Math.max(0, Math.min(100, (int) Math.round((mouseY - topPos - MASTER_SLIDER_Y) * 100 / MASTER_SLIDER_HEIGHT)));
		if (insertion == selectedMasterInsertion || minecraft == null || minecraft.gameMode == null) return;
		selectedMasterInsertion = insertion;
		menu.setClientMasterInsertion(insertion);
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ReactorGUIMenu.MASTER_INSERTION_BUTTON + insertion);
	}

    private static String compact(double value) {
        if (value >= 1_000_000) return String.format(java.util.Locale.ROOT, "%.1fM", value / 1_000_000);
        if (value >= 10_000) return Math.round(value / 1_000) + "k";
        return Long.toString(Math.round(value));
    }
    @Override protected net.minecraft.core.BlockPos machinePosition() { return menu.controller() == null ? null : menu.controller().getBlockPos(); }
    @Override protected String machineFallbackId() { return "reactor_computer"; }
}
