package net.crystalnexus.client.gui;

import net.crystalnexus.network.payload.C2S_RequestPage;
import net.crystalnexus.network.payload.C2S_Withdraw;
import net.crystalnexus.network.payload.S2C_SendPage;
import net.crystalnexus.world.inventory.DepotMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class DepotScreen extends AbstractContainerScreen<DepotMenu> {

    private EditBox searchBox;

    // Server page index (hidden from player)
    private int page = 0;

    // Scroll position in ROWS across the whole list (hidden from player)
    private int scrollRow = 0;

    // Latest entries for the current server page
    private final List<S2C_SendPage.Entry> entries = new ArrayList<>();

    // ✅ UI stats from server
    private int uiUpgradeLevel = 0;
    private long uiUsed = 0L;
    private long uiCapacity = 0L;

    private static final int PAD = 8;
    private static final int ROW_H = 20;
    private static final int LIST_ROWS = 10;
    private static final int LIST_W = 214;
    private static final int HEADER_H = 44;
    private static final int FOOTER_H = 18;

    // Scrollbar
    private static final int SCROLL_W = 6;

    private int refreshTicks = 0;

    public DepotScreen(DepotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 232;
        this.imageHeight = PAD + HEADER_H + (LIST_ROWS * ROW_H) + FOOTER_H + PAD;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos  = (this.height - this.imageHeight) / 2;

        this.searchBox = new EditBox(this.font,
                leftPos + PAD, topPos + PAD + 12,
                160, 16,
                Component.literal("Search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue("");
        this.addRenderableWidget(this.searchBox);

        // Start at top
        this.page = 0;
        this.scrollRow = 0;
        requestPage();
        setFocused(this.searchBox);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        // Light polling so uploader changes show up
        refreshTicks++;
        if (refreshTicks >= 8) { // ~2.5x/sec
            refreshTicks = 0;
            requestPage();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {

            // Block inventory-close key while typing
            if (keyCode == GLFW.GLFW_KEY_E) {
                return true;
            }

            // ESC unfocus instead of closing the menu
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.searchBox.setFocused(false);
                return true;
            }

            // Enter applies search
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                page = 0;
                scrollRow = 0;
                requestPage();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int listX = leftPos + PAD;
        int listY = topPos + PAD + HEADER_H;

        if (mouseX >= listX && mouseX <= listX + LIST_W + SCROLL_W
                && mouseY >= listY && mouseY <= listY + (LIST_ROWS * ROW_H)) {

            int dir = (deltaY > 0) ? -1 : 1;

            // If we are on the last page (entries not full), clamp within that page.
            boolean canGoNextPage = entries.size() >= DepotMenu.PAGE_SIZE;

            int rowOffsetInPage = scrollRow % LIST_ROWS;

            if (dir > 0) {
                // scrolling DOWN
                int maxOffsetThisPage = Math.max(0, entries.size() - 1);
                int maxRowOffsetThisPage = Math.min(LIST_ROWS - 1, maxOffsetThisPage);

                if (rowOffsetInPage < maxRowOffsetThisPage) {
                    scrollRow++;
                } else {
                    if (canGoNextPage) {
                        page++;
                        scrollRow = page * LIST_ROWS;
                        requestPage();
                    }
                }
            } else {
                // scrolling UP
                if (scrollRow > 0) {
                    if (rowOffsetInPage == 0 && page > 0) {
                        page--;
                        scrollRow = page * LIST_ROWS + (LIST_ROWS - 1);
                        requestPage();
                    } else {
                        scrollRow--;
                    }
                }
            }

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = leftPos + PAD;
        int listY = topPos + PAD + HEADER_H;

        if (mouseX >= listX && mouseX <= listX + LIST_W
                && mouseY >= listY && mouseY <= listY + (LIST_ROWS * ROW_H)) {

            int rowOnScreen = (int) ((mouseY - listY) / ROW_H);
            int rowOffsetInPage = scrollRow % LIST_ROWS;
            int idx = rowOffsetInPage + rowOnScreen;

            if (idx < 0 || idx >= entries.size()) return true;

            S2C_SendPage.Entry e = entries.get(idx);

            int amount;
            if (Screen.hasControlDown()) amount = Integer.MAX_VALUE;
            else if (Screen.hasShiftDown()) amount = 64;
            else amount = 1;

            PacketDistributor.sendToServer(new C2S_Withdraw(e.itemId(), amount));
            requestPage();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void requestPage() {
        String s = (searchBox == null) ? "" : searchBox.getValue();
        PacketDistributor.sendToServer(new C2S_RequestPage(s, page));
    }

    /** Called by DepotScreenHooks when S2C_SendPage arrives. */
    public void setPage(S2C_SendPage packet) {
        this.entries.clear();
        this.entries.addAll(packet.entries());

        this.uiUpgradeLevel = packet.upgradeLevel();
        this.uiUsed = packet.used();
        this.uiCapacity = packet.capacity();

        int rowOffsetInPage = scrollRow % LIST_ROWS;
        if (rowOffsetInPage >= entries.size() && !entries.isEmpty()) {
            scrollRow = (page * LIST_ROWS) + Math.max(0, entries.size() - 1);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC101010);

        int listX = leftPos + PAD;
        int listY = topPos + PAD + HEADER_H;

        // List background
        g.fill(listX, listY, listX + LIST_W, listY + (LIST_ROWS * ROW_H), 0xAA000000);

        // Row separators
        for (int i = 1; i < LIST_ROWS; i++) {
            int y = listY + i * ROW_H;
            g.fill(listX, y, listX + LIST_W, y + 1, 0x33000000);
        }

        int rowOffsetInPage = scrollRow % LIST_ROWS;

        // Render only visible rows
        for (int row = 0; row < LIST_ROWS; row++) {
            int idx = rowOffsetInPage + row;
            if (idx < 0 || idx >= entries.size()) break;

            S2C_SendPage.Entry e = entries.get(idx);

            int y = listY + row * ROW_H + 2;

            ItemStack icon = toIconStack(e.itemId());
            g.renderItem(icon, listX + 2, y);

            String name = icon.getHoverName().getString();
            String label = shorten(name, 20) + "  x" + e.count();
            g.drawString(font, label, listX + 24, y + 4, 0xFFFFFF);
        }

        drawScrollBar(g, listX + LIST_W + 2, listY, LIST_ROWS * ROW_H);
    }

    private void drawScrollBar(GuiGraphics g, int x, int y, int h) {
        g.fill(x, y, x + SCROLL_W, y + h, 0x55000000);

        boolean hasMoreBelow = entries.size() >= DepotMenu.PAGE_SIZE;
        int rowOffsetInPage = scrollRow % LIST_ROWS;

        int thumbH = hasMoreBelow ? Math.max(12, h / 6) : Math.max(22, h / 3);

        float t = (float) rowOffsetInPage / (float) (LIST_ROWS - 1);
        int thumbY = y + (int) ((h - thumbH) * t);

        g.fill(x, thumbY, x + SCROLL_W, thumbY + thumbH, 0xAAFFFFFF);

        if (hasMoreBelow) {
            g.fill(x, y + h - 2, x + SCROLL_W, y + h, 0xAAFFFFFF);
        }
    }

@Override
protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    g.drawString(font, this.title, PAD, 4, 0xFFFFFF);

    // ✅ Depot stats on the RIGHT side of header (won't overlap search)
    int statsX = PAD + 166; // same X region as refresh button area
    int y0 = 4;             // keep in the very top line area

    g.drawString(font, Component.literal("Upg: " + uiUpgradeLevel), statsX, y0, 0xAAAAAA);
    g.drawString(font, Component.literal("Used: " + fmt(uiUsed)), statsX, y0 + 10, 0xAAAAAA);

    long free = Math.max(0L, uiCapacity - uiUsed);
    g.drawString(font, Component.literal("Free: " + fmt(free)), statsX, y0 + 20, 0xAAAAAA);
    g.drawString(font, Component.literal("Max: " + fmt(uiCapacity)), statsX, y0 + 30, 0xAAAAAA);

    int listY = PAD + HEADER_H;
    int helperY = listY + (LIST_ROWS * ROW_H) + 4;
    g.drawString(font, Component.literal("Click:1  Shift:64  Ctrl:All"), PAD, helperY, 0x888888);
}


    private static String fmt(long v) {
        return String.format("%,d", v);
    }

    private static ItemStack toIconStack(ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) return new ItemStack(Items.BARRIER);
        return new ItemStack(item);
    }

    private static String shorten(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }
}
