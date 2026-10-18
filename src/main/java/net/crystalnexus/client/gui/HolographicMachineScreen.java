package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.MachineGuiLayout;
import net.crystalnexus.world.inventory.MachineGuiLayout.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Opt-in only: custom celestial and biological screens retain their original superclass. */
public abstract class HolographicMachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private int progressIndex;
    private float guiScale = 1;
    protected HolographicMachineScreen(M menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    protected abstract BlockPos machinePosition();
    protected String machineFallbackId() { return "machine_frame"; }

    protected int machineTier() {
        return MachineGuiTheme.tier(minecraft == null ? null : minecraft.level, machinePosition(), machineFallbackId());
    }

    protected Rect machineBounds() {
        Rect bounds = new Rect(0, -MachineGuiLayout.HEADER_HEIGHT, imageWidth, imageHeight + MachineGuiLayout.HEADER_HEIGHT);
        for (var slot : menu.slots) if (slot.isActive())
            bounds = bounds.union(new Rect(slot.x - 4, slot.y - 4, 24, 24));
        if (MachineGuiTheme.hasEnergy(minecraft == null ? null : minecraft.level, machinePosition()))
            bounds = bounds.union(MachineGuiLayout.energy());
        return bounds;
    }

    @Override protected void init() {
        for (var slot : menu.slots) if (slot.isActive() && slot.x >= 0 && slot.x < imageWidth)
            imageHeight = Math.max(imageHeight, slot.y + 20);
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
        MachineGuiTheme.background(graphics, menu, minecraft.level, machinePosition(), title,
            leftPos, topPos, imageWidth, imageHeight, machineTier());
    }

    protected int machineProgressCount() { return 1; }

    protected void machineProgress(GuiGraphics graphics, double fraction) {
        Rect r = MachineGuiLayout.progress(imageWidth, progressIndex++, machineProgressCount());
        MachineGuiTheme.bar(graphics, leftPos + r.x(), topPos + r.y(), r.width(), r.height(), fraction, MachineGuiTheme.accent(machineTier()));
    }

    protected void machineTank(GuiGraphics graphics, net.neoforged.neoforge.fluids.FluidStack fluid,
            int capacity, int x, int y, int width, int height) {
        MachineGuiTheme.frame(graphics, x - 1, y - 1, width + 2, height + 2, MachineGuiTheme.accent(machineTier()));
        FluidTankRenderer.drawClamped(graphics, fluid, capacity, x, y, width, height);
    }

    protected void machineBlockTank(GuiGraphics graphics, net.minecraft.world.level.LevelAccessor level,
            BlockPos pos, int tank, int x, int y, int width, int height) {
        var handler = level instanceof net.neoforged.neoforge.common.extensions.ILevelExtension extension
            ? extension.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, (net.minecraft.core.Direction) null) : null;
        machineTank(graphics, handler == null || tank >= handler.getTanks() ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : handler.getFluidInTank(tank),
            handler == null || tank >= handler.getTanks() ? 0 : handler.getTankCapacity(tank), x, y, width, height);
    }

    @Override public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().scale(guiScale, guiScale, 1);
        renderMachine(graphics, (int) (mouseX / guiScale), (int) (mouseY / guiScale), partialTick);
        graphics.pose().popPose();
    }

    protected void renderMachine(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        MachineGuiTheme.tooltip(graphics, minecraft.level, machinePosition(), title, leftPos, topPos, imageWidth, mouseX, mouseY);
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
    @Override public int getGuiLeft() { return Math.round(leftPos * guiScale); }
    @Override public int getGuiTop() { return Math.round(topPos * guiScale); }
    @Override public int getXSize() { return Math.round(imageWidth * guiScale); }
    @Override public int getYSize() { return Math.round(imageHeight * guiScale); }

    @Override protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return !machineBounds().contains(mouseX - leftPos, mouseY - topPos);
    }
}
