package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.MachineGuiLayout;
import net.crystalnexus.world.inventory.MachineGuiLayout.Rect;
import net.crystalnexus.world.inventory.MachineUpgradeSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Per-frame hover regions shared by native and custom machine themes. */
@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT)
public final class MachineGuiWidgets {
    private MachineGuiWidgets() {}
    private record Hover(Rect bounds, List<Component> lines) {}
    private record Flow(List<MachineGuiFlow.Point> points, boolean head) {}
    private record Arrows(List<Rect> obstacles, List<Flow> paths) {}
    private static final Map<AbstractContainerMenu, Arrows> ROUTES = new WeakHashMap<>();
    private static Frame frame;
    private static final class Frame {
        AbstractContainerScreen<?> screen;
        AbstractContainerMenu menu;
        int left, top, width, height, color;
        final List<Hover> hovers = new ArrayList<>();
        final List<Rect> tanks = new ArrayList<>();
    }

    @SubscribeEvent public static void before(ScreenEvent.Render.Pre event) { frame = null; }
    public static void begin(AbstractContainerMenu menu, int left, int top, int width, int height, int color) {
        frame = new Frame();
        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() == menu)
            frame.screen = screen;
        frame.menu = menu; frame.left = left; frame.top = top; frame.width = width; frame.height = height; frame.color = color;

    }

    public static double progress(LevelAccessor level, BlockPos pos) {
        return progress(level, pos, "progress", "maxProgress");
    }
    public static double progress(LevelAccessor level, BlockPos pos, String progress, String maximum) {
        BlockEntity machine = level == null || pos == null ? null : level.getBlockEntity(pos);
        return machine == null ? 0 : MachineGuiLayout.fraction(machine.getPersistentData().getDouble(progress),
                machine.getPersistentData().getDouble(maximum));
    }
    public static void value(Rect bounds, Component... lines) {
        if (frame != null) frame.hovers.add(new Hover(bounds, List.of(lines)));
    }
    public static Slot slot(AbstractContainerMenu menu, int index) {
        for (var slot : menu.slots) if (!(slot.container instanceof Inventory) && slot.getSlotIndex() == index) return slot;
        throw new IllegalArgumentException("No machine slot " + index);
    }
    /** Screen-space frame of a machine slot; colored overlays and their hover regions both use this. */
    public static Rect slotFrame(AbstractContainerMenu menu, int index, int left, int top) {
        Slot slot = slot(menu, index);
        Rect r = MachineGuiLayout.slot(slot.x, slot.y);
        return new Rect(left + r.x(), top + r.y(), r.width(), r.height());
    }
    public static void overlay(GuiGraphics g, net.minecraft.resources.ResourceLocation texture, Rect frame) {
        g.blit(texture, frame.x(), frame.y(), 0, 0, frame.width(), frame.height(), frame.width(), frame.height());
    }
    public static void alignControl(AbstractContainerMenu menu, AbstractWidget widget, int left, int top) {
        if (menu.slots.stream().noneMatch(slot -> slot.isActive() && slot.container instanceof Inventory)) return;
        Rect r = MachineGuiLayout.grid(menu.slots).control(widget.getX() - left, widget.getY() - top, widget.getWidth(), widget.getHeight());
        widget.setPosition(left + r.x(), top + r.y());
        widget.setWidth(r.width()); widget.setHeight(r.height());
    }
    public static void alignControls(AbstractContainerScreen<?> screen) {
        for (var child : screen.children()) if (child instanceof AbstractWidget widget)
            alignControl(screen.getMenu(), widget, screen.getGuiLeft(), screen.getGuiTop());
    }
    public static int label(GuiGraphics g, net.minecraft.client.gui.Font font, String text, int x, int y, int color, boolean shadow) {
        return label(g, font, Component.literal(text), x, y, color, shadow);
    }
    public static int label(GuiGraphics g, net.minecraft.client.gui.Font font, Component text, int x, int y, int color, boolean shadow) {
        if (frame != null) value(new Rect(frame.left + x, frame.top + y, font.width(text), font.lineHeight), text);
        return g.drawString(font, text, x, y, color, shadow);
    }
    public static int label(GuiGraphics g, net.minecraft.client.gui.Font font, String text, int x, int y, int color) { return label(g,font,text,x,y,color,true); }
    public static int label(GuiGraphics g, net.minecraft.client.gui.Font font, Component text, int x, int y, int color) { return label(g,font,text,x,y,color,true); }
    public static List<Component> tankTooltip(FluidStack fluid, int capacity) {
        return List.of(fluid.isEmpty() ? Component.translatable("gui.crystalnexus.machine.empty") : fluid.getHoverName(),
                Component.literal(String.format(Locale.ROOT, "%,d / %,d mB", fluid.isEmpty() ? 0 : fluid.getAmount(), Math.max(0, capacity))));
    }
    public static Rect tankBounds(int x, int y, int width, int height) {
        if (frame == null || frame.menu.getClass().getSimpleName().equals("DepotMenu")) return new Rect(x, y, width, height);
        var grid = MachineGuiLayout.grid(frame.menu.slots);
        boolean inventory = frame.menu.slots.stream().anyMatch(slot -> slot.isActive() && slot.container instanceof Inventory && slot.getSlotIndex() == 9);
        Rect r = grid.tank(x - frame.left, y - frame.top, width, height, inventory ? grid.y() : frame.height - 4);
        return new Rect(frame.left + r.x(), frame.top + r.y(), r.width(), r.height());
    }
    public static Rect tank(GuiGraphics g, FluidStack fluid, int capacity, int x, int y, int width, int height, boolean custom) {
        Rect r = tankBounds(x, y, width, height);
        if (frame != null && !frame.menu.getClass().getSimpleName().equals("DepotMenu")) {
            Rect outer = new Rect(r.x() - 1, r.y() - 1, r.width() + 2, r.height() + 2);
            frame.tanks.add(new Rect(outer.x() - frame.left, outer.y() - frame.top, outer.width(), outer.height()));
            value(outer, tankTooltip(fluid, capacity).toArray(Component[]::new));
            if (custom) MachineGuiTheme.frame(g, outer.x(), outer.y(), outer.width(), outer.height(), frame.color);
        }
        return r;
    }

    static boolean hasTankFrame() { return frame != null && !frame.menu.getClass().getSimpleName().equals("DepotMenu"); }

    public static void finishBackground(GuiGraphics g) {
        if (frame == null) return;
        int[][] connections = MachineGuiRoutes.connections(frame.menu.getClass().getSimpleName());
        if (connections.length == 0) return;
        List<Rect> obstacles = new ArrayList<>(frame.tanks);
        if (java.util.Arrays.stream(connections).anyMatch(connection -> connection[1] == -101)) obstacles.add(endpoint(-101));
        for (var slot : frame.menu.slots) if (slot.isActive()) obstacles.add(MachineGuiLayout.slot(slot.x, slot.y));
        if (frame.screen != null) for (var child : frame.screen.children()) if (child instanceof AbstractWidget widget && widget.visible)
            obstacles.add(new Rect(widget.getX() - frame.left, widget.getY() - frame.top, widget.getWidth(), widget.getHeight()));
        Arrows cached = ROUTES.get(frame.menu);
        if (cached == null || !cached.obstacles.equals(obstacles)) {
            List<Flow> paths = new ArrayList<>();
            Rect area = new Rect(3, 2, frame.width - 6, Math.max(1, MachineGuiLayout.grid(frame.menu.slots).y() - 5));
            boolean combining = MachineGuiRoutes.combining(frame.menu.getClass().getSimpleName());
            for (var route : MachineGuiFlow.mergeAdjacent(connections, MachineGuiWidgets::endpoint, !combining)) {
                var path = MachineGuiFlow.path(route.from(), route.to(), obstacles, area);
                if (!path.isEmpty()) paths.add(new Flow(path, true));
            }
            if (frame.menu.getClass().getSimpleName().equals("CircuitPressGUIMenu")) {
                var feeds = MachineGuiFlow.press(endpoint(0), endpoint(2), endpoint(-1), endpoint(1));
                paths.clear();
                for (int i = 0; i < feeds.size(); i++) paths.add(new Flow(feeds.get(i), i == feeds.size() - 1));
            }
            cached = new Arrows(List.copyOf(obstacles), List.copyOf(paths)); ROUTES.put(frame.menu, cached);
        }
        g.pose().pushPose(); g.pose().translate(frame.left, frame.top, 0);
        if (java.util.Arrays.stream(connections).anyMatch(connection -> connection[1] == -101)) {
            Rect target = endpoint(-101);
            g.drawString(Minecraft.getInstance().font, "FE", target.x(), target.y(), 0xFF000000 | frame.color, false);
            value(new Rect(frame.left + target.x(), frame.top + target.y(), target.width(), target.height()),
                Component.translatable("gui.crystalnexus.machine.energy_output"));
        }
        for (var flow : cached.paths) {
            var path = flow.points;
            MachineGuiFlow.draw(g, path, flow.head);
            for (int i = 1; i < path.size(); i++) {
                var a = path.get(i - 1); var b = path.get(i);
                value(new Rect(frame.left + Math.min(a.x(), b.x()) - 4, frame.top + Math.min(a.y(), b.y()) - 4,
                        Math.abs(a.x() - b.x()) + 8, Math.abs(a.y() - b.y()) + 8),
                        Component.translatable("gui.crystalnexus.machine.flow"));
            }
        }
        g.pose().popPose();
    }

    private static Rect endpoint(int id) {
        String menu = frame.menu.getClass().getSimpleName();
        if (id == -100) {
            Rect output = switch (menu) {
                case "NodeExtractorGUIMenu" -> endpoint(-1);
                case "NodeMinerGUIMenu" -> slotGroup(0, 1, 2);
                default -> null;
            };
            return new Rect(62, output == null ? 42 : output.y() + output.height() / 2, 1, 1);
        }
        if (id == -101) return new Rect(144, 47, 10, 8);
        if (id == -102) return slotGroup(0, 1, 2, 3, 4, 5, 6, 7, 8);
        if (id < 0) return -id <= frame.tanks.size() ? frame.tanks.get(-id - 1) : null;
        if (menu.equals("AcceleratorGuiMenu")) {
            if (id == 0) return slotGroup(0, 2, 3, 4);
            if (id == 1) return slotGroup(1, 5, 6, 7);
        }
        if (menu.equals("ExtractinatorGuiMenu") && id == 1) return slotGroup(1, 2, 3, 4, 5, 6);
        if (menu.equals("NodeMinerGUIMenu") && id == 0) return slotGroup(0, 1, 2);
        return slotGroup(id);
    }

    private static Rect slotGroup(int... indices) {
        Rect bounds = null;
        for (var slot : frame.menu.slots) if (slot.isActive() && !(slot.container instanceof Inventory)
                && !(slot instanceof MachineUpgradeSlot) && slot.x < frame.width) {
            for (int index : indices) if (slot.getSlotIndex() == index) {
                Rect r = MachineGuiLayout.slot(slot.x, slot.y);
                bounds = bounds == null ? r : bounds.union(r);
            }
        }
        return bounds;
    }

    @SubscribeEvent public static void after(ScreenEvent.Render.Post event) {
        if (frame == null) return;
        Frame current = frame; frame = null;
        if (current.screen != event.getScreen()) return;
        float scale = current.screen instanceof HolographicMachineScreen<?> screen ? screen.getGuiScale() : 1;
        double mx = event.getMouseX() / scale, my = event.getMouseY() / scale;
        List<Component> tooltip = null;
        for (var hover : current.hovers) if (hover.bounds.contains(mx, my)) { tooltip = hover.lines; break; }
        if (tooltip == null) for (var slot : current.menu.slots) {
            if (!slot.isActive() || slot.hasItem() || slot.container instanceof Inventory
                    || !MachineGuiLayout.slot(current.left + slot.x, current.top + slot.y).contains(mx, my)) continue;
            String role = slot instanceof MachineUpgradeSlot || slot.x >= current.width ? "upgrade"
                    : MachineGuiRoutes.role(current.menu.getClass().getSimpleName(), slot.getSlotIndex());
            if (current.menu.getClass().getSimpleName().equals("ReactorGUIMenu"))
                role = slot.index == 0 ? "upgrade" : slot.index == 1 ? "output" : "fuel";
            tooltip = List.of(current.menu instanceof net.crystalnexus.world.inventory.CraftingFactoryGUIMenu
                    && slot.getSlotIndex() == net.crystalnexus.world.inventory.CraftingFactoryGUIMenu.FILTER_SLOT
                    ? Component.translatable("gui.crystalnexus.crafting_factory_gui.tooltip_crafting_filter")
                    : Component.translatable("gui.crystalnexus.machine." + role)); break;
        }
        if (tooltip == null) for (var child : current.screen.children()) if (child instanceof AbstractWidget widget
                && widget.visible && widget.isMouseOver(mx, my) && widget.getTooltip() == null && !widget.getMessage().getString().isBlank()) {
            String label = widget.getMessage().getString();
            tooltip = List.of(label.equalsIgnoreCase("X") ? Component.translatable("gui.crystalnexus.machine.purge") : widget.getMessage()); break;
        }
        if (tooltip != null) {
            var g = event.getGuiGraphics(); g.pose().pushPose(); g.pose().translate(0, 0, 600);
            g.renderComponentTooltip(Minecraft.getInstance().font, tooltip, event.getMouseX(), event.getMouseY());
            g.pose().popPose();
        }
    }
}
