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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    private static final int PAD = 8;
    private static final int ROW_H = 20;
    private static final int LIST_ROWS = 10;
    private static final int LIST_W = 214;

    // Scrollbar
    private static final int SCROLL_W = 6;

    private int refreshTicks = 0;

    public DepotScreen(DepotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 232;
        this.imageHeight = 288;
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

        // Keep a manual refresh button (nice for debugging)
        this.addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> requestPage())
                .bounds(leftPos + PAD + 166, topPos + PAD + 12, 58, 16)
                .build());

        // Start at top
        this.page = 0;
        this.scrollRow = 0;
        requestPage();
        setFocused(this.searchBox);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        // Don’t spam while typing
        if (searchBox != null && searchBox.isFocused()) return;

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
            // Enter = apply search (reset scroll/page)
            if (keyCode == 257 || keyCode == 335) {
                this.page = 0;
                this.scrollRow = 0;
                requestPage();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

@Override
public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
    int listX = leftPos + PAD;
    int listY = topPos + PAD + 60;

    if (mouseX >= listX && mouseX <= listX + LIST_W + SCROLL_W
            && mouseY >= listY && mouseY <= listY + (LIST_ROWS * ROW_H)) {

        int dir = (deltaY > 0) ? -1 : 1;

        // If we are on the last page (entries not full), clamp within that page.
        boolean canGoNextPage = entries.size() >= DepotMenu.PAGE_SIZE;

        int rowOffsetInPage = scrollRow % LIST_ROWS;

        if (dir > 0) {
            // scrolling DOWN
            // allow moving down within loaded entries
            int maxOffsetThisPage = Math.max(0, entries.size() - 1); // max index we have
            int maxRowOffsetThisPage = Math.min(LIST_ROWS - 1, maxOffsetThisPage);

            if (rowOffsetInPage < maxRowOffsetThisPage) {
                scrollRow++;
            } else {
                // We're at bottom of visible slice for this page.
                // Only go to next server page if we believe there are more.
                if (canGoNextPage) {
                    page++;
                    scrollRow = page * LIST_ROWS; // keep offset at 0 for the new page
                    requestPage();
                }
            }
        } else {
            // scrolling UP
            if (scrollRow > 0) {
                // if we're at the top of a page, move to previous page
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
        int listY = topPos + PAD + 60;

        if (mouseX >= listX && mouseX <= listX + LIST_W
                && mouseY >= listY && mouseY <= listY + (LIST_ROWS * ROW_H)) {

            int rowOnScreen = (int) ((mouseY - listY) / ROW_H);
int rowOffsetInPage = scrollRow % LIST_ROWS;
int idx = rowOffsetInPage + rowOnScreen;

if (idx < 0 || idx >= entries.size()) return true;
 {
                S2C_SendPage.Entry e = entries.get(idx);

                int amount;
                if (Screen.hasControlDown()) amount = Integer.MAX_VALUE;
                else if (Screen.hasShiftDown()) amount = 64;
                else amount = 1;

                PacketDistributor.sendToServer(new C2S_Withdraw(e.itemId(), amount));
                // After withdraw, ask for updated data (instant feeling)
                requestPage();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void requestPage() {
        String s = (searchBox == null) ? "" : searchBox.getValue();
        PacketDistributor.sendToServer(new C2S_RequestPage(s, page));
    }

    /** Called by DepotScreenHooks when S2C_SendPage arrives. */
    public void setEntries(List<S2C_SendPage.Entry> newEntries) {
        this.entries.clear();
        this.entries.addAll(newEntries);

        // If we scrolled past the end of the last page, clamp the scrollRow a bit.
        // This prevents “jumping” if the page shrinks due to search or withdrawals.
        int rowOffsetInPage = scrollRow % LIST_ROWS;
        if (rowOffsetInPage >= entries.size() && !entries.isEmpty()) {
            scrollRow = (page * LIST_ROWS) + Math.max(0, entries.size() - 1);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC101010);

        int listX = leftPos + PAD;
        int listY = topPos + PAD + 60;

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

            // ✅ Display name (not registry id)
            String name = icon.getHoverName().getString();
            String label = shorten(name, 20) + "  x" + e.count();
            g.drawString(font, label, listX + 24, y + 4, 0xFFFFFF);
        }

        // Draw scrollbar (simple)
        drawScrollBar(g, listX + LIST_W + 2, listY, LIST_ROWS * ROW_H);
    }

private void drawScrollBar(GuiGraphics g, int x, int y, int h) {
    // Track
    g.fill(x, y, x + SCROLL_W, y + h, 0x55000000);

    boolean hasMoreBelow = entries.size() >= DepotMenu.PAGE_SIZE;
    int rowOffsetInPage = scrollRow % LIST_ROWS;

    // Thumb size: larger if no more below (end), smaller if more below
    int thumbH = hasMoreBelow ? Math.max(12, h / 6) : Math.max(22, h / 3);

    float t = (float) rowOffsetInPage / (float) (LIST_ROWS - 1);
    int thumbY = y + (int) ((h - thumbH) * t);

    g.fill(x, thumbY, x + SCROLL_W, thumbY + thumbH, 0xAAFFFFFF);

    // Optional: little indicator that there's more below
    if (hasMoreBelow) {
        g.fill(x, y + h - 2, x + SCROLL_W, y + h, 0xAAFFFFFF);
    }
}


@Override
protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    g.drawString(font, this.title, PAD, 4, 0xFFFFFF);

    // Put helper line under the list area
    int helperY = PAD + 60 + (LIST_ROWS * ROW_H) + 6;
    g.drawString(font, Component.literal("Click:1  Shift:64  Ctrl:All"), PAD, helperY, 0x888888);
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
