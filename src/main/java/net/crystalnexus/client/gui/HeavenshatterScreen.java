package net.crystalnexus.client.gui;

import net.crystalnexus.block.entity.HeavenshatterBeamControllerBlockEntity;
import net.crystalnexus.network.payload.C2S_HeavenshatterControls;
import net.crystalnexus.world.inventory.HeavenshatterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class HeavenshatterScreen extends AbstractContainerScreen<HeavenshatterMenu> {
    private static final int CYAN = 0xff63dfec, AMBER = 0xffffbc69, TEXT = 0xffdfd6c8;
    private TuningSlider frequency, azimuth, elevation;
    private Button lock, pull, next, composition;
    private TuningSlider draggingSlider;
    private boolean dirty;
    private int selected;
    private long lastSent = -100, lockTime = -100;
    private boolean wasLocked;
    private CompoundTag lastInformation;
    private long lastCost = -1;

    public HeavenshatterScreen(HeavenshatterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 316; imageHeight = 348;
    }
    private static Component text(String key, Object... args) { return Component.translatable("gui.crystalnexus.celestial." + key, args); }
    @Override protected void init() {
        super.init();
        lastInformation = null; lastCost = -1;
        var state = menu.tracking;
        selected = state.getInt("selected");
        azimuth = addRenderableWidget(new TuningSlider(140, 24, 359, "azimuth", state.getInt("azimuth")));
        elevation = addRenderableWidget(new TuningSlider(140, 46, 90, "elevation", state.getInt("elevation")));
        frequency = addRenderableWidget(new TuningSlider(140, 68, 100, "frequency", state.getInt("frequency")));
        next = addRenderableWidget(Button.builder(text("next"), b -> {
            int size = menu.tracking.getList("markers", Tag.TAG_COMPOUND).size();
            if (size > 0) { selected = (selected + 1) % size; dirty = true; }
        }).bounds(leftPos + 12, topPos + 126, 116, 18).tooltip(Tooltip.create(text("instructions"))).build());
        composition = addRenderableWidget(Button.builder(text("composition"), b -> {}).bounds(leftPos + 12, topPos + 168, 292, 12).build());
        lock = addRenderableWidget(Button.builder(text("lock"), b -> send(1)).bounds(leftPos + 12, topPos + 190, 142, 20).build());
        pull = addRenderableWidget(Button.builder(text("pull"), b -> send(2)).bounds(leftPos + 162, topPos + 190, 142, 20).build());
    }
    @Override protected void containerTick() {
        super.containerTick();
        long tick = minecraft.level.getGameTime();
        var state = menu.tracking;
        boolean scanning = state.getInt("stage") == 0 && state.getBoolean("formed") && state.getInt("energy") >= 1000
            && menu.getSlot(0).getItem().isEmpty() && !state.getList("markers", Tag.TAG_COMPOUND).isEmpty();
        if (dirty && tick - lastSent >= 2) send(0);
        if (!dirty && tick - lastSent > 6) {
            if (!azimuth.isHoveredOrFocused()) azimuth.sync(state.getInt("azimuth"));
            if (!elevation.isHoveredOrFocused()) elevation.sync(state.getInt("elevation"));
            if (!frequency.isHoveredOrFocused()) frequency.sync(state.getInt("frequency"));
            selected = state.getInt("selected");
        }
        azimuth.active = elevation.active = next.active = scanning;
        frequency.active = scanning && state.getBoolean("angularAligned");
        lock.active = scanning && state.getBoolean("identified") && state.getInt("stability") >= 60 && !dirty && state.getInt("energy") >= 1000;
        pull.active = state.getInt("stage") == 1 && state.getString("status").equals("locked");
        if (menu.controller() != null && lastCost != menu.controller().cost()) {
            lastCost = menu.controller().cost(); pull.setTooltip(Tooltip.create(text("pull_cost", lastCost)));
        }
        composition.visible = state.getBoolean("identified");
        var info = state.getCompound("information");
        if (composition.visible && !info.equals(lastInformation)) {
            lastInformation = info;
            var details = Component.empty();
            for (Tag entry : info.getList("materials", Tag.TAG_COMPOUND)) {
                var material = (CompoundTag) entry;
                var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(material.getString("block")));
                if (!details.getSiblings().isEmpty()) details.append("\n");
                details.append(text("material", block.getName(), material.getInt("count"), material.getInt("count") * 100 / Math.max(1, info.getInt("mass"))));
            }
            composition.setTooltip(Tooltip.create(details));
        }
        boolean locked = state.getInt("stage") == 1;
        if (locked && !wasLocked) lockTime = tick;
        wasLocked = locked;
    }
    private void send(int action) {
        PacketDistributor.sendToServer(new C2S_HeavenshatterControls(menu.containerId, selected, frequency.setting(), azimuth.setting(), elevation.setting(), action));
        lastSent = minecraft.level.getGameTime(); dirty = false;
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff181b20);
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xffae7742);
        for (var slot : menu.slots) {
            g.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xff081317);
            g.renderOutline(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, 0xff627078);
        }
        g.fill(leftPos + 7, topPos + 20, leftPos + 133, topPos + 122, 0xff081317);
        double time = minecraft.level.getGameTime() + partial;
        circle(g, 70, 72, 17, 0xff25434a); circle(g, 70, 72, 34, 0xff25434a); circle(g, 70, 72, 49, 0xff25434a);
        line(g, 21, 72, 119, 72, 0xff25434a); line(g, 70, 23, 70, 121, 0xff25434a);
        double sweep = time * 0.035;
        for (int i = 0; i < 8; i++) line(g, 70, 72, 70 + (int)(Math.sin(sweep - i * .035) * 49),
            72 - (int)(Math.cos(sweep - i * .035) * 49), (0x18 + (7-i)*9) << 24 | 0x63dfec);
        var state = menu.tracking;
        var markers = state.getList("markers", Tag.TAG_COMPOUND);
        for (int i = 0; i < markers.size(); i++) {
            var marker = markers.getCompound(i);
            int x = markerX(marker), y = markerY(marker);
            int intensity = i == selected ? 100 + state.getInt("strength") * 155 / 100 : 175;
            int color = intensity << 24 | ((marker.getBoolean("known") ? AMBER : CYAN) & 0xffffff);
            g.fill(leftPos + x - 2, topPos + y - 2, leftPos + x + 3, topPos + y + 3, color);
            if (i == selected) g.renderOutline(leftPos + x - 5, topPos + y - 5, 11, 11, TEXT);
            g.drawString(font, Integer.toString(i + 1), leftPos + x + 5, topPos + y - 5, color, false);
        }
        double angle = Math.toRadians(azimuth.setting()), radius = 49 * (1 - elevation.setting() / 100.0);
        int x = 70 + (int)(Math.sin(angle) * radius), y = 72 - (int)(Math.cos(angle) * radius);
        line(g, x-4, y, x+4, y, AMBER); line(g, x, y-4, x, y+4, AMBER);
        if (time - lockTime < 30) circle(g, 70, 72, 15 + (int)(time - lockTime), AMBER);
        g.fill(leftPos + 140, topPos + 101, leftPos + 304, topPos + 122, 0xff081317);
        int match = state.getInt("frequencySignal");
        for (int i = 1; i < 163; i++) {
            double phase = i * .13 + time * .1;
            int target = 111 + (int)(Math.sin(phase) * 7);
            int tuned = 111 + (int)(Math.sin(phase + (100-match)*.035) * 7 + Math.sin(i*.73 + time*.23)*(100-match)*.02);
            g.fill(leftPos + 140 + i, topPos + target, leftPos + 141 + i, topPos + target + 1, CYAN);
            g.fill(leftPos + 140 + i, topPos + tuned, leftPos + 141 + i, topPos + tuned + 1, AMBER);
        }
        bar(g, 12, 182, 292, state.getInt("stability"), 60);
    }
    private int markerX(CompoundTag m) { return 70 + (int)(Math.sin(Math.toRadians(m.getInt("azimuth"))) * 49 * (1 - m.getInt("elevation") / 100.0)); }
    private int markerY(CompoundTag m) { return 72 - (int)(Math.cos(Math.toRadians(m.getInt("azimuth"))) * 49 * (1 - m.getInt("elevation") / 100.0)); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && (draggingSlider = sliderAt(x, y)) != null && draggingSlider.active) {
            draggingSlider.setFocused(true);
            draggingSlider.setFromMouse(x);
            return true;
        }
        if (button == 0 && next.active) {
            var markers = menu.tracking.getList("markers", Tag.TAG_COMPOUND);
            for (int i = 0; i < markers.size(); i++) if (Math.hypot(x - leftPos - markerX(markers.getCompound(i)), y - topPos - markerY(markers.getCompound(i))) < 8) {
                selected = i; dirty = true; return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        if (button == 0 && draggingSlider != null) {
            draggingSlider.setFromMouse(x);
            return true;
        }
        return super.mouseDragged(x, y, button, dragX, dragY);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && draggingSlider != null) {
            draggingSlider = null;
            return true;
        }
        return super.mouseReleased(x, y, button);
    }
    private TuningSlider sliderAt(double x, double y) {
        for (TuningSlider slider : new TuningSlider[]{azimuth, elevation, frequency}) {
            if (slider != null && x >= slider.getX() && x < slider.getX() + slider.getWidth()
                && y >= slider.getY() && y < slider.getY() + slider.getHeight()) return slider;
        }
        return null;
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        var state = menu.tracking;
        g.drawString(font, text("title"), 12, 8, AMBER, false);
        g.drawString(font, text("signal", state.getInt("strength"), state.getInt("frequencySignal")), 140, 92, CYAN, false);
        String a = state.getInt("azimuthHint") == 0 ? "aligned" : state.getInt("azimuthHint") > 0 ? "right" : "left";
        String e = state.getInt("elevationHint") == 0 ? "aligned" : state.getInt("elevationHint") > 0 ? "up" : "down";
        g.drawString(font, text("hints", text(a), text(e)), 140, 128, TEXT, false);
        var info = state.getCompound("information");
        if (!info.isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth(info.getString("name"), 130), 12, 148, CYAN, false);
            g.drawString(font, text("mass", info.getInt("mass"), info.getInt("yield")), 140, 148, TEXT, false);
            g.drawString(font, text("distance", info.getInt("distance"), state.getInt("stability") * 100 / 60), 12, 160, TEXT, false);

        } else {
            g.drawWordWrap(font, text(state.getBoolean("angularAligned") ? "tune_help" : "scan_help"), 12, 148, 290, TEXT);
            g.drawString(font, text("stability", state.getInt("stability") * 100 / 60), 12, 172, AMBER, false);
        }
        g.drawString(font, text("power", state.getInt("energy"), state.getInt("discoveries")), 12, 213, TEXT, false);
        Component status = Component.translatable("gui.crystalnexus.heavenshatter.status." + state.getString("status"));
        var controller = menu.controller();
        if (state.getInt("stage") == 2 && controller != null) status = Component.translatable("gui.crystalnexus.heavenshatter.retrieval", controller.progress() * 100 / controller.duration());
        g.drawString(font, font.plainSubstrByWidth(status.getString(), 292), 12, 224, AMBER, false);
        g.drawString(font, text("meteor_filter"), 34, 244, TEXT, false);
        g.drawString(font, playerInventoryTitle, 77, 256, TEXT, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
    private void bar(GuiGraphics g, int x, int y, int width, int value, int max) {
        g.fill(leftPos+x, topPos+y, leftPos+x+width, topPos+y+4, 0xff354249);
        g.fill(leftPos+x, topPos+y, leftPos+x+width*Math.clamp(value,0,max)/max, topPos+y+4, CYAN);
    }
    private void circle(GuiGraphics g, int x, int y, int radius, int color) {
        for (int i=0;i<96;i++) {
            double a=i*Math.PI/48, b=(i+1)*Math.PI/48;
            line(g,x+(int)(Math.cos(a)*radius),y+(int)(Math.sin(a)*radius),x+(int)(Math.cos(b)*radius),y+(int)(Math.sin(b)*radius),color);
        }
    }
    private void line(GuiGraphics g, int x, int y, int endX, int endY, int color) {
        int steps=Math.max(Math.abs(endX-x),Math.abs(endY-y));
        for(int i=0;i<=steps;i++) {
            int px=x+(endX-x)*i/Math.max(1,steps), py=y+(endY-y)*i/Math.max(1,steps);
            g.fill(leftPos+px,topPos+py,leftPos+px+1,topPos+py+1,color);
        }
    }
    private final class TuningSlider extends AbstractSliderButton {
        private final int maximum; private final String label;
        TuningSlider(int x,int y,int maximum,String label,int setting) {
            super(leftPos+x,topPos+y,164,20,Component.empty(),setting/(double)maximum);
            this.maximum=maximum; this.label=label; updateMessage();
        }
        int setting() { return (int)Math.round(value*maximum); }
        void sync(int setting) { value=setting/(double)maximum; updateMessage(); }
        void setFromMouse(double mouseX) {
            int previous = setting();
            value = Math.clamp((mouseX - (getX() + 4.0)) / (getWidth() - 8.0), 0.0, 1.0);
            updateMessage();
            if (setting() != previous) applyValue();
        }
        @Override protected void updateMessage() { setMessage(text(label,setting())); }
        @Override protected void applyValue() { dirty=true; }
        @Override public boolean keyPressed(int key,int scan,int modifiers) {
            if(active && (key==GLFW.GLFW_KEY_LEFT || key==GLFW.GLFW_KEY_RIGHT)) {
                sync(Math.clamp(setting()+(key==GLFW.GLFW_KEY_LEFT?-1:1),0,maximum)); applyValue(); return true;
            }
            return super.keyPressed(key,scan,modifiers);
        }
    }
}
