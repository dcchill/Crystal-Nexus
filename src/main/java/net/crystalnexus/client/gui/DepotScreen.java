package net.crystalnexus.client.gui;

import net.crystalnexus.network.payload.C2S_RequestPage;
import net.crystalnexus.network.payload.S2C_SendPage;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.world.inventory.DepotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepotScreen extends AbstractContainerScreen<DepotMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/depot_gui_54.png");
    private static final int BAR_X = 178;
    private static final int BAR_Y = 18;
    private static final int BAR_HEIGHT = 108;
    private static final int THUMB_HEIGHT = 15;
    private static final float DEPOT_COUNT_SCALE = 0.65F;

    private EditBox searchBox;
    private final Map<Integer, ResourceLocation> depotEntryIds = new HashMap<>();
    private final Map<Integer, Long> depotCounts = new HashMap<>();
    private int page;
    private int totalEntries;
    private int refreshTicks;
    private int uiUpgradeLevel;
    private long uiUsed;
    private long uiCapacity;
    private float scrollVisual;
    private boolean scrolling;

    public DepotScreen(DepotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 190;
        imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        searchBox = new EditBox(font, leftPos + 72, topPos + 4, 96, 12, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search"));
        searchBox.setResponder(value -> {
            page = 0;
            requestPage();
        });
        addRenderableWidget(searchBox);
        requestPage();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (++refreshTicks >= 8) {
            refreshTicks = 0;
            requestPage();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_E) return true;
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchBox.setFocused(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                requestPage();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX >= leftPos && mouseX < leftPos + 190 && mouseY >= topPos && mouseY < topPos + 126) {
            int next = Mth.clamp(page + (deltaY < 0 ? 9 : -9), 0, maxPage());
            if (next != page) {
                page = next;
                requestPage();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overScrollbar(mouseX, mouseY)) {
            scrolling = true;
            scrollTo(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling) {
            scrollTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFluidEntries(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, 176, imageHeight, 256, 256);
        int x = leftPos + BAR_X;
        int y = topPos + BAR_Y;
        graphics.fill(x, y, x + 10, y + BAR_HEIGHT, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 9, y + BAR_HEIGHT - 1, 0xFF373737);

        float target = maxPage() == 0 ? 0 : (float) page / maxPage() * (BAR_HEIGHT - THUMB_HEIGHT);
        scrollVisual += (target - scrollVisual) * 0.35F;
        int thumbY = y + Math.round(scrollVisual);
        graphics.fill(x + 1, thumbY, x + 9, thumbY + THUMB_HEIGHT, 0xFFC6C6C6);
        graphics.fill(x + 2, thumbY + 1, x + 8, thumbY + THUMB_HEIGHT - 1, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.literal("Depot " + (page / 9 + 1) + "/" + (maxPage() / 9 + 1)), 8, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, 128, 0x404040, false);
        String stats = "U" + uiUpgradeLevel + " " + compact(uiUsed) + "/" + compact(uiCapacity);
        graphics.drawString(font, stats, 168 - font.width(stats), 128, 0x404040, false);
    }

    @Override
    protected void renderSlotContents(GuiGraphics graphics, ItemStack stack, Slot slot, String countString) {
        if (menu.isDepotSlot(slot) && DepotSavedData.isFluidKey(depotEntryIds.get(slot.getSlotIndex()))) {
            return;
        }
        if (menu.isDepotSlot(slot) && !stack.isEmpty()) {
            graphics.renderItem(stack, slot.x, slot.y, slot.x + slot.y * imageWidth);
            renderDepotCount(graphics, compact(depotCounts.getOrDefault(slot.getSlotIndex(), 0L)), slot.x, slot.y);
            return;
        }
        super.renderSlotContents(graphics, stack, slot, countString);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        if (hoveredSlot != null && menu.isDepotSlot(hoveredSlot)) {
            int slot = hoveredSlot.getSlotIndex();
            ResourceLocation fluidId = DepotSavedData.fluidId(depotEntryIds.get(slot));
            if (fluidId != null) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(new FluidStack(BuiltInRegistries.FLUID.get(fluidId), 1).getHoverName());
                tooltip.add(Component.literal("Stored: " + String.format("%,d", depotCounts.getOrDefault(slot, 0L)) + " mB")
                        .withStyle(ChatFormatting.GRAY));
                return tooltip;
            }
        }
        List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot != null && menu.isDepotSlot(hoveredSlot)) {
            int slot = hoveredSlot.getSlotIndex();
            long count = depotCounts.getOrDefault(slot, 0L);
            boolean fluid = DepotSavedData.isFluidKey(depotEntryIds.get(slot));
            tooltip.add(Component.literal("Stored: " + String.format("%,d", count) + (fluid ? " mB" : ""))
                    .withStyle(ChatFormatting.GRAY));
        }
        return tooltip;
    }

    public void setPage(S2C_SendPage packet) {
        depotCounts.clear();
        depotEntryIds.clear();
        for (int i = 0; i < packet.entries().size(); i++) {
            S2C_SendPage.Entry entry = packet.entries().get(i);
            depotEntryIds.put(i, entry.itemId());
            depotCounts.put(i, entry.count());
        }
        totalEntries = packet.totalEntries();
        page = Math.min(page, maxPage());
        uiUpgradeLevel = packet.upgradeLevel();
        uiUsed = packet.used();
        uiCapacity = packet.capacity();
    }

    private void renderFluidEntries(GuiGraphics graphics) {
        for (Slot slot : menu.slots) {
            if (!menu.isDepotSlot(slot)) continue;
            ResourceLocation fluidId = DepotSavedData.fluidId(depotEntryIds.get(slot.getSlotIndex()));
            if (fluidId == null) continue;
            FluidStack fluid = new FluidStack(BuiltInRegistries.FLUID.get(fluidId), 1);
            FluidTankRenderer.draw(graphics, fluid, 1, leftPos + slot.x, topPos + slot.y, 16, 16);
            renderDepotCount(graphics, compact(depotCounts.getOrDefault(slot.getSlotIndex(), 0L)),
                leftPos + slot.x, topPos + slot.y);
        }
    }

    private void renderDepotCount(GuiGraphics graphics, String count, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x + 17 - font.width(count) * DEPOT_COUNT_SCALE, y + 17 - 8 * DEPOT_COUNT_SCALE, 200);
        graphics.pose().scale(DEPOT_COUNT_SCALE, DEPOT_COUNT_SCALE, 1);
        graphics.drawString(font, count, 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    private void requestPage() {
        PacketDistributor.sendToServer(new C2S_RequestPage(searchBox == null ? "" : searchBox.getValue(), page));
    }

    private int maxPage() {
        return Math.max(0, ((totalEntries + 8) / 9 - 6) * 9);
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        return mouseX >= leftPos + BAR_X && mouseX < leftPos + BAR_X + 10
                && mouseY >= topPos + BAR_Y && mouseY < topPos + BAR_Y + BAR_HEIGHT;
    }

    private void scrollTo(double mouseY) {
        if (maxPage() == 0) return;
        float position = Mth.clamp((float) (mouseY - topPos - BAR_Y - THUMB_HEIGHT / 2.0)
                / (BAR_HEIGHT - THUMB_HEIGHT), 0, 1);
        int next = Math.round(position * (maxPage() / 9.0F)) * 9;
        if (next != page) {
            page = next;
            requestPage();
        }
    }

    private static String compact(long value) {
        if (value < 100) return Long.toString(value);
        if (value < 1_000) return decimal(value, 1_000, "K");
        if (value < 100_000) return (value / 1_000) + "K";
        if (value < 100_000_000) return decimal(value, 1_000_000, "M");
        if (value < 100_000_000_000L) return decimal(value, 1_000_000_000, "B");
        if (value < 100_000_000_000_000L) return decimal(value, 1_000_000_000_000L, "T");
        return decimal(value, 1_000_000_000_000_000L, "Q");
    }

    private static String decimal(long value, long scale, String suffix) {
        long tenths = value / (scale / 10);
        return tenths % 10 == 0 ? (tenths / 10) + suffix : (tenths / 10) + "." + (tenths % 10) + suffix;
    }
}
