package net.crystalnexus.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.crystalnexus.network.ConveyorFilterTagSelect;
import net.crystalnexus.world.inventory.ConveyorFilterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ConveyorFilterScreen extends HolographicMachineScreen<ConveyorFilterMenu> {
    private int selectedSlot;
    private int scroll;
    private boolean choosing;
    private List<ResourceLocation> tags = List.of();
    private final List<Button> tagButtons = new ArrayList<>();
    private Button previous;
    private Button next;
    private Button choose;

    public ConveyorFilterScreen(ConveyorFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 442;
        imageHeight = 190;
    }

    @Override protected void init() {
        super.init();
        tagButtons.clear();
        themedButton(26, 76, 160, () -> text(menu.isBlacklist() ? "blacklist" : "whitelist"), ignored -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        });
        choose = themedButton(224, 22, 198, () -> text("choose_tag", selectedSlot + 1), ignored -> {
            choosing = !choosing;
            scroll = 0;
            refreshChoices();
        });
        for (int row = 0; row < 5; row++) {
            int offset = row;
            tagButtons.add(themedButton(224, 58 + row * 18, 198, () -> choiceLabel(scroll + offset), ignored -> {
                int index = scroll + offset;
                if (index <= tags.size()) PacketDistributor.sendToServer(new ConveyorFilterTagSelect(
                    menu.containerId, selectedSlot, index == 0 ? null : tags.get(index - 1)));
            }));
        }
        previous = themedButton(224, 154, 90, () -> text("previous"), ignored -> scrollChoices(-1));
        next = themedButton(332, 154, 90, () -> text("next"), ignored -> scrollChoices(1));
        refreshChoices();
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.crystalnexus.conveyor_filters." + key, args);
    }

    private Button themedButton(int x, int y, int width, Supplier<Component> label, Button.OnPress action) {
        return addRenderableWidget(new Button(leftPos + x, topPos + y, width, 18, label.get(), action, supplier -> supplier.get()) {
            @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                setMessage(label.get());
                int accent = MachineGuiTheme.accent(machineTier());
                MachineGuiTheme.frame(graphics, getX(), getY(), getWidth(), getHeight(),
                    isHoveredOrFocused() ? 0x7de8ff : accent);
                graphics.drawCenteredString(font, font.plainSubstrByWidth(getMessage().getString(), getWidth() - 8),
                    getX() + getWidth() / 2, getY() + (getHeight() - font.lineHeight) / 2,
                    active ? 0xffd7e6ed : 0xff517387);
            }
        });
    }

    private Component choiceLabel(int index) {
        if (index > tags.size()) return Component.empty();
        ResourceLocation tag = index == 0 ? null : tags.get(index - 1);
        Component label = tag == null ? text("exact_item") : Component.literal("#" + tag);
        return java.util.Objects.equals(tag, menu.getFilterTag(selectedSlot)) ? text("selected", label) : label;
    }

    private void refreshChoices() {
        tags = menu.getSlot(selectedSlot).getItem().getTags().map(tag -> tag.location())
            .sorted(java.util.Comparator.comparing(ResourceLocation::toString)).toList();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, tags.size() + 1 - tagButtons.size())));
        choose.active = menu.getSlot(selectedSlot).hasItem();
        for (int row = 0; row < tagButtons.size(); row++) {
            var button = tagButtons.get(row);
            button.visible = choosing && choose.active && scroll + row <= tags.size();
            button.setMessage(choiceLabel(scroll + row));
            button.setTooltip(Tooltip.create(button.getMessage()));
        }
        previous.visible = next.visible = choosing && choose.active;
        previous.active = scroll > 0;
        next.active = scroll + tagButtons.size() < tags.size() + 1;
    }

    private void scrollChoices(int amount) {
        scroll += amount;
        refreshChoices();
    }

    @Override protected void containerTick() {
        super.containerTick();
        refreshChoices();
    }

    @Override protected boolean machineMouseClicked(double x, double y, int button) {
        for (int i = 0; i < ConveyerBeltBaseBlockEntity.FILTER_SLOTS; i++) {
            var slot = menu.getSlot(i);
            if (x >= leftPos + slot.x && x < leftPos + slot.x + 16
                    && y >= topPos + slot.y && y < topPos + slot.y + 16) {
                selectedSlot = i;
                choosing = false;
                scroll = 0;
                refreshChoices();
                // Selecting a populated filter with an empty hand must not erase it.
                if (button == 0 && menu.getCarried().isEmpty() && slot.hasItem()) return true;
                break;
            }
        }
        return super.machineMouseClicked(x, y, button);
    }

    @Override protected boolean machineMouseScrolled(double x, double y, double dx, double dy) {
        if (choosing && x >= leftPos + 220 && x < leftPos + imageWidth
                && y >= topPos + 50 && y < topPos + 176) {
            scrollChoices(dy > 0 ? -1 : dy < 0 ? 1 : 0);
            return true;
        }
        return super.machineMouseScrolled(x, y, dx, dy);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        machineBackground(graphics);
        MachineGuiWidgets.finishBackground(graphics);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 7, 0xff7de8ff, false);
        graphics.drawString(font, text("inventory_only"), 8, 22, 0xff91aab7, false);
        graphics.drawString(font, text("instructions"), 8, 36, 0xff91aab7, false);
        graphics.drawString(font, playerInventoryTitle, 26, 97, 0xff91aab7, false);
        var slot = menu.getSlot(selectedSlot);
        graphics.renderOutline(slot.x - 2, slot.y - 2, 20, 20, 0xff7de8ff);
        for (int i = 0; i < ConveyerBeltBaseBlockEntity.FILTER_SLOTS; i++) {
            if (menu.getFilterTag(i) != null) {
                var tagged = menu.getSlot(i);
                graphics.drawString(font, "#", tagged.x + 10, tagged.y, 0xffffff55, true);
            }
        }
        var tag = menu.getFilterTag(selectedSlot);
        if (!choosing) {
            String value = tag == null ? text("exact_item").getString() : "#" + tag;
            graphics.drawWordWrap(font, Component.literal(value), 224, 58, 198, 0xff91aab7);
        } else if (tags.isEmpty()) {
            graphics.drawString(font, text("no_tags"), 224, 42, 0xff91aab7, false);
        }
    }

    @Override protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredSlot != null && hoveredSlot.index < ConveyerBeltBaseBlockEntity.FILTER_SLOTS) {
            var tag = menu.getFilterTag(hoveredSlot.index);
            if (tag != null) {
                graphics.renderTooltip(font, List.of(hoveredSlot.getItem().getHoverName(), Component.literal("#" + tag)),
                    java.util.Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderMachine(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.renderMachine(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected net.minecraft.core.BlockPos machinePosition() { return menu.conveyorPos(); }
    @Override protected String machineFallbackId() { return "conveyer_belt"; }
}
