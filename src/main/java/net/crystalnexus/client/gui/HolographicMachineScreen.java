package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.MachineGuiLayout;
import net.crystalnexus.world.inventory.MachineGuiLayout.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Shared holographic machine theme, layout and scaled mouse handling. */
public abstract class HolographicMachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private int progressIndex;
    private float guiScale = 1;
    protected HolographicMachineScreen(M menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    protected abstract BlockPos machinePosition();
    protected String machineFallbackId() { return "machine_frame"; }

    @Override protected <T extends net.minecraft.client.gui.components.events.GuiEventListener
            & net.minecraft.client.gui.components.Renderable
            & net.minecraft.client.gui.narration.NarratableEntry> T addRenderableWidget(T widget) {
        if (widget instanceof net.minecraft.client.gui.components.AbstractWidget control)
            MachineGuiWidgets.alignControl(menu, control, leftPos, topPos);
        return super.addRenderableWidget(widget);
    }

    protected int machineTier() {
        return MachineGuiTheme.tier(minecraft == null ? null : minecraft.level, machinePosition(), machineFallbackId());
    }

    protected Rect machineBounds() {
        Rect bounds = new Rect(0, -MachineGuiLayout.HEADER_HEIGHT, imageWidth, imageHeight + MachineGuiLayout.HEADER_HEIGHT);
        for (var slot : menu.slots) if (slot.isActive())
            bounds = bounds.union(new Rect(slot.x - 4, slot.y - 4, 24, 24));
        Rect bay = MachineGuiLayout.upgradeBay(menu.slots, imageWidth, imageHeight);
        if (bay != null) bounds = bounds.union(bay);
        if (MachineGuiTheme.hasEnergy(minecraft == null ? null : minecraft.level, machinePosition()))
            bounds = bounds.union(MachineGuiLayout.energy(menu.slots, imageHeight));
        return bounds;
    }

    @Override protected void init() {
        for (var slot : menu.slots) if (slot.isActive() && slot.x >= 0 && slot.y + 20 > imageHeight)
            imageHeight += ((slot.y + 20 - imageHeight + 17) / 18) * 18;
        super.init();
        Rect bounds = machineBounds();
        int viewportWidth = minecraft.getWindow().getGuiScaledWidth();
        int viewportHeight = minecraft.getWindow().getGuiScaledHeight();
        guiScale = MachineGuiLayout.fitScale(viewportWidth, viewportHeight, bounds);
        width = (int) (viewportWidth / guiScale);
        height = (int) (viewportHeight / guiScale);
        leftPos = MachineGuiLayout.centeredOrigin(width, bounds.x(), bounds.width());
        topPos = MachineGuiLayout.centeredOrigin(height, bounds.y(), bounds.height());
    }

    protected void machineBackground(GuiGraphics graphics) {
        progressIndex = 0;
        MachineGuiWidgets.begin(menu, leftPos, topPos, imageWidth, imageHeight, MachineGuiTheme.accent(machineTier()));
        MachineGuiTheme.background(graphics, menu, minecraft.level, machinePosition(), title,
            leftPos, topPos, imageWidth, imageHeight, machineTier());
    }

    protected java.util.List<String> machineReadings() { return MachineGuiLayout.readings(menu.getClass().getSimpleName()); }
    protected final int machineProgressCount() { return machineReadings().size(); }

    protected void machineProgress(GuiGraphics graphics, double fraction) {
        machineGauge(graphics, fraction,
            Component.translatable("gui.crystalnexus.machine.progress", (int) Math.round(MachineGuiLayout.fraction(fraction, 1) * 100)));
    }

    protected void machineGauge(GuiGraphics graphics, double fraction, Component tooltip) {
        var readings = machineReadings();
        int index = progressIndex++;
        Rect row = MachineGuiLayout.progress(imageWidth, index, readings.size());
        Rect r = row;
        int color = MachineGuiTheme.accent(machineTier());
        if (readings.size() > 1) {
            // Separate readings carry a fixed-width label column so a stored level never reads as a second progress bar.
            int column = 0;
            for (String key : readings) column = Math.max(column, font.width(readingLabel(key)));
            String key = readings.get(Math.min(index, readings.size() - 1));
            graphics.drawString(font, readingLabel(key), leftPos + row.x(), topPos + row.y(), color, false);
            r = new Rect(row.x() + column + 4, row.y(), row.width() - column - 4, row.height());
            if (key.equals("stored")) color = 0xFFD15B;
        }
        MachineGuiTheme.bar(graphics, leftPos + r.x(), topPos + r.y(), r.width(), r.height(), fraction, color);
        MachineGuiWidgets.value(new Rect(leftPos + row.x(), topPos + row.y(), row.width(), row.height()), tooltip);
    }

    private static Component readingLabel(String key) {
        return Component.translatable("gui.crystalnexus.machine.reading." + key);
    }

    protected void machineTank(GuiGraphics graphics, net.neoforged.neoforge.fluids.FluidStack fluid,
            int capacity, int x, int y, int width, int height) {
        Rect r = MachineGuiWidgets.tankBounds(x, y, width, height);
        MachineGuiTheme.frame(graphics, r.x() - 1, r.y() - 1, r.width() + 2, r.height() + 2, MachineGuiTheme.accent(machineTier()));
        FluidTankRenderer.drawClamped(graphics, fluid, capacity, r.x(), r.y(), r.width(), r.height());
    }

    protected void machineBlockTank(GuiGraphics graphics, net.minecraft.world.level.LevelAccessor level,
            BlockPos pos, int tank, int x, int y, int width, int height) {
        var handler = level instanceof net.neoforged.neoforge.common.extensions.ILevelExtension extension
            ? extension.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, (net.minecraft.core.Direction) null) : null;
        machineTank(graphics, handler == null || tank < 0 || tank >= handler.getTanks() ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : handler.getFluidInTank(tank),
            handler == null || tank < 0 || tank >= handler.getTanks() ? 0 : handler.getTankCapacity(tank), x, y, width, height);
    }

    @Override public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().scale(guiScale, guiScale, 1);
        renderMachine(graphics, (int) (mouseX / guiScale), (int) (mouseY / guiScale), partialTick);
        graphics.pose().popPose();
    }

    protected void renderMachine(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        MachineGuiTheme.tooltip(graphics, menu, minecraft.level, machinePosition(), title, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
    }

    @Override public final boolean mouseClicked(double x, double y, int button) { return machineMouseClicked(x / guiScale, y / guiScale, button); }
    protected boolean machineMouseClicked(double x, double y, int button) { return super.mouseClicked(x, y, button); }
    @Override public final boolean mouseReleased(double x, double y, int button) { return machineMouseReleased(x / guiScale, y / guiScale, button); }
    protected boolean machineMouseReleased(double x, double y, int button) { return super.mouseReleased(x, y, button); }
    @Override public final boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return machineMouseDragged(x / guiScale, y / guiScale, button, dx / guiScale, dy / guiScale);
    }
    protected boolean machineMouseDragged(double x, double y, int button, double dx, double dy) { return super.mouseDragged(x, y, button, dx, dy); }
    @Override public final boolean mouseScrolled(double x, double y, double dx, double dy) { return machineMouseScrolled(x / guiScale, y / guiScale, dx, dy); }
    protected boolean machineMouseScrolled(double x, double y, double dx, double dy) { return super.mouseScrolled(x, y, dx, dy); }
    @Override public final void mouseMoved(double x, double y) { machineMouseMoved(x / guiScale, y / guiScale); }
    protected void machineMouseMoved(double x, double y) { super.mouseMoved(x, y); }

    // External overlays (JEI/EMI) must see the same bounds as the player.
    public float getGuiScale() { return guiScale; }
    @Override public int getGuiLeft() { return Math.round(leftPos * guiScale); }
    @Override public int getGuiTop() { return Math.round(topPos * guiScale); }
    @Override public int getXSize() { return Math.round(imageWidth * guiScale); }
    @Override public int getYSize() { return Math.round(imageHeight * guiScale); }

    @Override protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return !machineBounds().contains(mouseX - leftPos, mouseY - topPos);
    }
}
