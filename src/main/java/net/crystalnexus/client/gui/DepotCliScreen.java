package net.crystalnexus.client.gui;

import net.crystalnexus.cli.DepotCliParser;
import net.crystalnexus.cli.DepotCraftingService;
import net.crystalnexus.jei.CrystalnexusJeiRuntimePlugin;
import net.crystalnexus.network.payload.C2S_DepotCliRequest;
import net.crystalnexus.network.payload.C2S_DepotCraftingRequest;
import net.crystalnexus.network.payload.S2C_DepotCliResponse;
import net.crystalnexus.network.payload.S2C_DepotCraftingResponse;
import net.crystalnexus.world.inventory.DepotCliMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DepotCliScreen extends AbstractContainerScreen<DepotCliMenu> {
    private static final int MAX_OUTPUT = 200;
    private static final int MAX_HISTORY = 50;
    private enum Tab { CRAFTING, TERMINAL }

    private final List<String> output = new ArrayList<>();
    private final List<String> history = new ArrayList<>();
    private final Set<Integer> collapsed = new HashSet<>();
    private final Map<Integer, DepotCraftingService.PreviewNode> nodesById = new HashMap<>();
    private final Map<Integer, Integer> nodeDepths = new HashMap<>();
    private final Set<Integer> parents = new HashSet<>();
    private List<String> suggestions = List.of();
    private List<DepotCraftingService.CatalogEntry> catalog = List.of();
    private List<DepotCraftingService.PreviewNode> visibleNodeCache = List.of();
    private DepotCraftingService.Preview preview;
    private EditBox terminalInput;
    private EditBox searchInput;
    private EditBox amountInput;
    private Button craftingTab;
    private Button terminalTab;
    private Button previousPage;
    private Button nextPage;
    private Button startButton;
    private Button cancelButton;
    private Button automaticButton;
    private Tab tab = Tab.CRAFTING;
    private ResourceLocation selectedTarget;
    private int selectedNodeId;
    private int catalogPage;
    private int catalogPages = 1;
    private int historyIndex;
    private int suggestionIndex;
    private int terminalScroll;
    private int treeScroll;
    private int routeScroll;
    private int searchDelay;
    private int refreshDelay;
    private String lastCatalogQuery;
    private int lastCatalogPage = -1;
    private boolean terminalStarted;
    private boolean applyingSuggestion;
    private String message = "Select an item to preview its crafting tree.";
    private boolean messageSuccess = true;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public DepotCliScreen(DepotCliMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 500;
        imageHeight = 300;
    }

    @Override
    protected void init() {
        imageWidth = Math.min(500, width - 16);
        imageHeight = Math.min(300, height - 16);
        super.init();
        craftingTab = addRenderableWidget(Button.builder(Component.translatable("gui.crystalnexus.depot_cli.crafting"), button -> setTab(Tab.CRAFTING))
                .bounds(leftPos + 8, topPos + 5, 72, 18).build());
        terminalTab = addRenderableWidget(Button.builder(Component.translatable("gui.crystalnexus.depot_cli.terminal"), button -> setTab(Tab.TERMINAL))
                .bounds(leftPos + 82, topPos + 5, 72, 18).build());
        searchInput = new EditBox(font, leftPos + 8, topPos + 31, 126, 16,
                Component.translatable("gui.crystalnexus.depot_cli.search"));
        searchInput.setHint(Component.translatable("gui.crystalnexus.depot_cli.search"));
        searchInput.setMaxLength(64);
        searchInput.setResponder(value -> searchDelay = 12);
        addRenderableWidget(searchInput);
        amountInput = new EditBox(font, leftPos + 166, topPos + imageHeight - 24, 48, 16, Component.literal("Amount"));
        amountInput.setValue("1");
        amountInput.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        amountInput.setResponder(value -> {
            if (selectedTarget != null) refreshDelay = 6;
        });
        addRenderableWidget(amountInput);
        previousPage = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(leftPos + 8, topPos + imageHeight - 24, 20, 18).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(leftPos + 114, topPos + imageHeight - 24, 20, 18).build());
        startButton = addRenderableWidget(Button.builder(Component.translatable("gui.crystalnexus.depot_cli.start"), button -> startCraft())
                .bounds(leftPos + 220, topPos + imageHeight - 24, 58, 18).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("gui.crystalnexus.depot_cli.cancel"), button -> cancelJob())
                .bounds(leftPos + imageWidth - 88, topPos + imageHeight - 24, 80, 18).build());
        automaticButton = addRenderableWidget(Button.builder(Component.translatable("gui.crystalnexus.depot_cli.automatic"), button -> clearRoute())
                .bounds(leftPos + imageWidth - 88, topPos + 31, 80, 18).build());

        terminalInput = new EditBox(font, leftPos + 21, topPos + imageHeight - 24, imageWidth - 33, 16,
                Component.literal("Depot command"));
        terminalInput.setMaxLength(256);
        terminalInput.setBordered(false);
        terminalInput.setTextColor(0xFFE8E8E8);
        terminalInput.setResponder(value -> { if (!applyingSuggestion) suggestions = List.of(); });
        addRenderableWidget(terminalInput);
        updateWidgets();
        requestCatalog(true);
    }

    private void setTab(Tab next) {
        tab = next;
        updateWidgets();
        if (next == Tab.TERMINAL) {
            setInitialFocus(terminalInput);
            if (!terminalStarted) {
                terminalStarted = true;
                PacketDistributor.sendToServer(new C2S_DepotCliRequest(menu.containerId, "", false));
            }
        } else setInitialFocus(searchInput);
    }

    private void updateWidgets() {
        boolean crafting = tab == Tab.CRAFTING;
        searchInput.visible = crafting;
        amountInput.visible = crafting;
        previousPage.visible = crafting;
        nextPage.visible = crafting;
        startButton.visible = crafting;
        cancelButton.visible = crafting && menu.hasJob();
        automaticButton.visible = crafting;
        terminalInput.visible = !crafting;
        craftingTab.active = !crafting;
        terminalTab.active = crafting;
        previousPage.active = catalogPage > 0;
        nextPage.active = catalogPage + 1 < catalogPages;
        startButton.active = preview != null && preview.startable() && !menu.hasJob();
        cancelButton.active = menu.hasJob();
        automaticButton.active = selectedNode() != null && selectedNode().selectedRoute() != null;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (searchDelay > 0 && --searchDelay == 0) {
            catalogPage = 0;
            requestCatalog(false);
        }
        if (refreshDelay > 0 && --refreshDelay == 0) {
            requestPreview();
        }
        updateWidgets();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (tab == Tab.CRAFTING) {
            if (searchInput.isFocused() && keyCode == GLFW.GLFW_KEY_E) return true;
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (searchInput.isFocused()) requestCatalog(true);
                else if (amountInput.isFocused()) requestPreview();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            terminalScroll = Math.max(0, terminalScroll - visibleTerminalLines());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            terminalScroll = Math.min(maxTerminalScroll(), terminalScroll + visibleTerminalLines());
            return true;
        }
        if (terminalInput.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { submit(); return true; }
            if (keyCode == GLFW.GLFW_KEY_UP) { moveHistory(-1); return true; }
            if (keyCode == GLFW.GLFW_KEY_DOWN) { moveHistory(1); return true; }
            if (keyCode == GLFW.GLFW_KEY_TAB) { autocomplete(); return true; }
            terminalInput.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (tab == Tab.TERMINAL) {
            terminalScroll = Mth.clamp(terminalScroll + (deltaY < 0 ? 1 : -1), 0, maxTerminalScroll());
            return true;
        }
        if (mouseX >= leftPos + 140 && mouseX < leftPos + imageWidth - 145) {
            treeScroll = Mth.clamp(treeScroll + (deltaY < 0 ? 1 : -1), 0,
                    Math.max(0, visibleNodes().size() - treeRows()));
            return true;
        }
        DepotCraftingService.PreviewNode node = selectedNode();
        if (node != null && mouseX >= leftPos + imageWidth - 142) {
            routeScroll = Mth.clamp(routeScroll + (deltaY < 0 ? 1 : -1), 0,
                    Math.max(0, node.alternatives().size() - 4));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == Tab.CRAFTING && button == 0) {
            int catalogIndex = (int) ((mouseY - topPos - 51) / 18);
            if (mouseX >= leftPos + 8 && mouseX < leftPos + 134 && mouseY >= topPos + 51
                    && catalogIndex >= 0 && catalogIndex < catalog.size()) {
                selectTarget(catalog.get(catalogIndex).itemId());
                return true;
            }
            List<DepotCraftingService.PreviewNode> visible = visibleNodes();
            int treeIndex = treeScroll + (int) ((mouseY - topPos - 31) / 18);
            if (mouseX >= leftPos + 140 && mouseX < leftPos + imageWidth - 145
                    && treeIndex >= 0 && treeIndex < visible.size()) {
                DepotCraftingService.PreviewNode node = visible.get(treeIndex);
                selectedNodeId = node.id();
                routeScroll = 0;
                if (hasChildren(node.id())) {
                    if (!collapsed.add(node.id())) collapsed.remove(node.id());
                    rebuildVisibleNodes();
                }
                updateWidgets();
                return true;
            }
            DepotCraftingService.PreviewNode node = selectedNode();
            if (node != null && mouseX >= leftPos + imageWidth - 138 && mouseX < leftPos + imageWidth - 8) {
                int routeIndex = (int) ((mouseY - topPos - 72) / 25);
                routeIndex += routeScroll;
                if (routeIndex >= routeScroll && routeIndex < Math.min(routeScroll + 4, node.alternatives().size())) {
                    setPreference(C2S_DepotCraftingRequest.Action.SET_ROUTE, node.itemId(),
                            node.alternatives().get(routeIndex).id());
                    return true;
                }
                List<ResourceLocation> machines = machines(node);
                int machineIndex = (int) ((mouseY - topPos - 189) / 18);
                if (machineIndex >= 0 && machineIndex < Math.min(3, machines.size())) {
                    setPreference(C2S_DepotCraftingRequest.Action.SET_MACHINE, node.itemId(), machines.get(machineIndex));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredStack = ItemStack.EMPTY;
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!hoveredStack.isEmpty()) graphics.renderTooltip(font, hoveredStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF080D0D);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 2, 0xFF3FCFC5);
        if (tab == Tab.TERMINAL) {
            graphics.fill(leftPos, topPos + imageHeight - 31, leftPos + imageWidth, topPos + imageHeight - 30, 0xFF245E5A);
            graphics.fill(leftPos + 10, topPos + imageHeight - 26, leftPos + imageWidth - 10,
                    topPos + imageHeight - 6, 0xFF111B1B);
            return;
        }
        int divider = leftPos + 138;
        int details = leftPos + imageWidth - 142;
        graphics.fill(divider, topPos + 27, divider + 1, topPos + imageHeight - 30, 0xFF245E5A);
        graphics.fill(details, topPos + 27, details + 1, topPos + imageHeight - 30, 0xFF245E5A);
        renderCatalog(graphics, mouseX, mouseY);
        renderTree(graphics, mouseX, mouseY);
        renderDetails(graphics, mouseX, mouseY);
        renderJob(graphics);
    }

    private void renderCatalog(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < catalog.size(); i++) {
            DepotCraftingService.CatalogEntry entry = catalog.get(i);
            int x = leftPos + 8;
            int y = topPos + 51 + i * 18;
            boolean selected = entry.itemId().equals(selectedTarget);
            if (selected) graphics.fill(x - 2, y - 1, x + 126, y + 17, 0xFF164440);
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(entry.itemId()));
            graphics.renderItem(stack, x, y);
            String name = font.plainSubstrByWidth(stack.getHoverName().getString(), 82);
            graphics.drawString(font, name, x + 19, y + 1, entry.craftable() ? 0xFFDCF8F4 : 0xFFFF9A9A, false);
            graphics.drawString(font, compact(entry.stored()), x + 19, y + 9, 0xFF8FA8A5, false);
            if (inside(mouseX, mouseY, x, y, 126, 17)) hoveredStack = stack;
        }
    }

    private void renderTree(GuiGraphics graphics, int mouseX, int mouseY) {
        List<DepotCraftingService.PreviewNode> nodes = visibleNodes();
        int end = Math.min(nodes.size(), treeScroll + treeRows());
        for (int row = treeScroll; row < end; row++) {
            DepotCraftingService.PreviewNode node = nodes.get(row);
            int depth = depth(node);
            int x = leftPos + 144 + Math.min(5, depth) * 11;
            int y = topPos + 31 + (row - treeScroll) * 18;
            if (node.id() == selectedNodeId) graphics.fill(leftPos + 141, y - 1,
                    leftPos + imageWidth - 144, y + 17, 0xFF164440);
            if (hasChildren(node.id())) graphics.drawString(font, collapsed.contains(node.id()) ? "+" : "-",
                    x - 9, y + 4, 0xFF55FFF2, false);
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(node.itemId()));
            graphics.renderItem(stack, x, y);
            int color = switch (node.source()) {
                case STORED -> 0xFF8FD8FF;
                case CRAFTING -> 0xFF69FF91;
                case MACHINE -> 0xFFFFD166;
                case MISSING -> 0xFFFF6B6B;
            };
            String label = compact(node.required()) + "x " + stack.getHoverName().getString();
            graphics.drawString(font, font.plainSubstrByWidth(label, Math.max(30, imageWidth - 315 - depth * 11)),
                    x + 19, y + 4, color, false);
            if (inside(mouseX, mouseY, x, y, imageWidth - 285, 17)) hoveredStack = stack;
        }
    }

    private void renderDetails(GuiGraphics graphics, int mouseX, int mouseY) {
        DepotCraftingService.PreviewNode node = selectedNode();
        int x = leftPos + imageWidth - 136;
        if (node == null) {
            graphics.drawString(font, "Select a tree node", x, topPos + 57, 0xFF8FA8A5, false);
            return;
        }
        ItemStack selected = new ItemStack(BuiltInRegistries.ITEM.get(node.itemId()));
        graphics.renderItem(selected, x, topPos + 53);
        graphics.drawString(font, font.plainSubstrByWidth(selected.getHoverName().getString(), 105),
                x + 19, topPos + 57, 0xFFE8F4F3, false);
        for (int i = 0; i < Math.min(4, node.alternatives().size() - routeScroll); i++) {
            DepotCraftingService.RecipeChoice choice = node.alternatives().get(routeScroll + i);
            int y = topPos + 72 + i * 25;
            boolean preferred = choice.id().equals(node.selectedRoute());
            graphics.fill(x, y, leftPos + imageWidth - 8, y + 22, preferred ? 0xFF176158 : 0xFF111B1B);
            graphics.drawString(font, font.plainSubstrByWidth(choice.category(), 122), x + 3, y + 3,
                    choice.processing() ? 0xFFFFD166 : 0xFF69FF91, false);
            String inputs = choice.inputs().size() + " input" + (choice.inputs().size() == 1 ? "" : "s");
            graphics.drawString(font, inputs, x + 3, y + 12, 0xFF8FA8A5, false);
        }
        List<ResourceLocation> machines = machines(node);
        if (!machines.isEmpty()) graphics.drawString(font, "Machine", x, topPos + 177, 0xFF55FFF2, false);
        for (int i = 0; i < Math.min(3, machines.size()); i++) {
            ResourceLocation machine = machines.get(i);
            int y = topPos + 189 + i * 18;
            ItemStack stack = new ItemStack(BuiltInRegistries.BLOCK.get(machine));
            boolean preferred = machine.equals(node.selectedMachine());
            graphics.fill(x, y, leftPos + imageWidth - 8, y + 16, preferred ? 0xFF176158 : 0xFF111B1B);
            graphics.renderItem(stack, x, y);
            graphics.drawString(font, font.plainSubstrByWidth(stack.getHoverName().getString(), 103),
                    x + 19, y + 4, 0xFFE8F4F3, false);
            if (inside(mouseX, mouseY, x, y, 128, 16)) hoveredStack = stack;
        }
    }

    private void renderJob(GuiGraphics graphics) {
        int y = topPos + imageHeight - 47;
        if (menu.hasJob()) {
            graphics.fill(leftPos + 141, y, leftPos + imageWidth - 8, y + 15, 0xFF111B1B);
            graphics.fill(leftPos + 142, y + 1,
                    leftPos + 142 + (imageWidth - 151) * menu.getJobPercent() / 100, y + 14, 0xFF176158);
            String current = menu.isProcessing() ? "Machine: " : "Crafting: ";
            current += menu.getCurrentStepAmount() + "x " + menu.getCurrentStep().getHoverName().getString();
            graphics.drawString(font, font.plainSubstrByWidth(current, imageWidth - 250), leftPos + 145, y + 4,
                    0xFFE8F4F3, false);
            graphics.drawString(font, menu.getJobPercent() + "%", leftPos + imageWidth - 112, y + 4,
                    0xFFE8F4F3, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String status = menu.isConnected() ? "CONNECTED" : "OFFLINE";
        graphics.drawString(font, status, imageWidth - 8 - font.width(status), 9,
                menu.isConnected() ? 0xFF63FF86 : 0xFFFF6565, false);
        if (tab == Tab.TERMINAL) {
            List<FormattedCharSequence> lines = wrappedOutput();
            int start = Math.min(terminalScroll, Math.max(0, lines.size() - visibleTerminalLines()));
            int end = Math.min(lines.size(), start + visibleTerminalLines());
            int y = 30;
            for (int i = start; i < end; i++) {
                graphics.drawString(font, lines.get(i), 11, y, 0xFFFFFFFF, false);
                y += font.lineHeight + 1;
            }
            graphics.drawString(font, ">", 11, imageHeight - 22, 0xFF55FFF2, false);
            return;
        }
        graphics.drawString(font, "OUTPUTS", 8, 22, 0xFF55FFF2, false);
        graphics.drawString(font, "CRAFTING TREE", 144, 22, 0xFF55FFF2, false);
        graphics.drawString(font, "ROUTE", imageWidth - 136, 22, 0xFF55FFF2, false);
        graphics.drawString(font, (catalogPage + 1) + "/" + catalogPages, 49, imageHeight - 20, 0xFF8FA8A5, false);
        graphics.drawString(font, "Qty", 143, imageHeight - 20, 0xFF8FA8A5, false);
        graphics.drawString(font, font.plainSubstrByWidth(message, Math.max(40, imageWidth - 250)), 160, 10,
                messageSuccess ? 0xFF69FF91 : 0xFFFF6B6B, false);
    }

    public void handleCraftingResponse(S2C_DepotCraftingResponse response) {
        if (response.menuId() != menu.containerId) return;
        switch (response.kind()) {
            case CATALOG -> {
                catalog = response.catalog().entries();
                catalogPage = response.catalog().page();
                catalogPages = response.catalog().totalPages();
            }
            case PREVIEW -> {
                preview = response.preview();
                selectedNodeId = 0;
                routeScroll = 0;
                collapsed.clear();
                rebuildTreeCache();
                messageSuccess = preview.success();
                message = preview.details().isEmpty()
                        ? preview.success() ? "Ready: " + duration(preview.estimatedTicks()) : "No complete route."
                        : preview.details().getFirst();
            }
            case RESULT -> {
                messageSuccess = response.success();
                message = response.message();
                refreshDelay = 4;
            }
        }
        updateWidgets();
    }

    public void handleResponse(S2C_DepotCliResponse response) {
        if (response.menuId() != menu.containerId) return;
        if (!response.lines().isEmpty()) append(response.lines());
        if (!response.suggestions().isEmpty()) {
            suggestions = response.suggestions();
            suggestionIndex = 0;
            applySuggestion(suggestions.getFirst());
        }
    }

    private void requestCatalog(boolean force) {
        if (tab != Tab.CRAFTING) return;
        String query = searchInput == null ? "" : searchInput.getValue();
        if (!force && query.equals(lastCatalogQuery) && catalogPage == lastCatalogPage) return;
        lastCatalogQuery = query;
        lastCatalogPage = catalogPage;
        PacketDistributor.sendToServer(new C2S_DepotCraftingRequest(menu.containerId,
                C2S_DepotCraftingRequest.Action.CATALOG, query,
                catalogPage, C2S_DepotCraftingRequest.NONE, 0, C2S_DepotCraftingRequest.NONE,
                C2S_DepotCraftingRequest.NONE, 0));
    }

    private void requestPreview() {
        if (selectedTarget == null) return;
        PacketDistributor.sendToServer(new C2S_DepotCraftingRequest(menu.containerId,
                C2S_DepotCraftingRequest.Action.PREVIEW, selectedTarget, amount()));
    }

    private void selectTarget(ResourceLocation itemId) {
        selectedTarget = itemId;
        treeScroll = 0;
        requestPreview();
    }

    private void startCraft() {
        if (selectedTarget == null) return;
        PacketDistributor.sendToServer(new C2S_DepotCraftingRequest(menu.containerId,
                C2S_DepotCraftingRequest.Action.START, selectedTarget, amount()));
    }

    private void cancelJob() {
        PacketDistributor.sendToServer(new C2S_DepotCraftingRequest(menu.containerId,
                C2S_DepotCraftingRequest.Action.CANCEL, "", 0, C2S_DepotCraftingRequest.NONE, 0,
                C2S_DepotCraftingRequest.NONE, C2S_DepotCraftingRequest.NONE, menu.getJobId()));
    }

    private void clearRoute() {
        DepotCraftingService.PreviewNode node = selectedNode();
        if (node != null) setPreference(C2S_DepotCraftingRequest.Action.CLEAR_ROUTE, node.itemId(),
                C2S_DepotCraftingRequest.NONE);
    }

    private void setPreference(C2S_DepotCraftingRequest.Action action, ResourceLocation subject, ResourceLocation choice) {
        PacketDistributor.sendToServer(new C2S_DepotCraftingRequest(menu.containerId, action, "", 0,
                selectedTarget == null ? C2S_DepotCraftingRequest.NONE : selectedTarget, amount(), subject, choice, 0));
    }

    private void changePage(int delta) {
        int next = Mth.clamp(catalogPage + delta, 0, catalogPages - 1);
        if (next != catalogPage) {
            catalogPage = next;
            requestCatalog(true);
        }
    }

    private int amount() {
        try { return Mth.clamp(Integer.parseInt(amountInput.getValue()), 1, 1_000_000); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private DepotCraftingService.PreviewNode selectedNode() {
        return nodesById.get(selectedNodeId);
    }

    private boolean hasChildren(int id) {
        return parents.contains(id);
    }

    private int depth(DepotCraftingService.PreviewNode node) {
        return nodeDepths.getOrDefault(node.id(), 0);
    }

    private List<DepotCraftingService.PreviewNode> visibleNodes() {
        return visibleNodeCache;
    }

    private void rebuildTreeCache() {
        nodesById.clear();
        nodeDepths.clear();
        parents.clear();
        if (preview != null) {
            preview.nodes().forEach(node -> {
                nodesById.put(node.id(), node);
                if (node.parentId() >= 0) parents.add(node.parentId());
            });
            preview.nodes().forEach(node -> {
                int depth = 0;
                for (DepotCraftingService.PreviewNode current = nodesById.get(node.parentId()); current != null && depth < 16;
                        current = nodesById.get(current.parentId())) depth++;
                nodeDepths.put(node.id(), depth);
            });
        }
        rebuildVisibleNodes();
    }

    private void rebuildVisibleNodes() {
        if (preview == null) {
            visibleNodeCache = List.of();
            return;
        }
        visibleNodeCache = preview.nodes().stream().filter(node -> {
            for (DepotCraftingService.PreviewNode parent = nodesById.get(node.parentId()); parent != null;
                    parent = nodesById.get(parent.parentId())) if (collapsed.contains(parent.id())) return false;
            return true;
        }).toList();
    }

    private List<ResourceLocation> machines(DepotCraftingService.PreviewNode node) {
        return node.alternatives().stream().flatMap(choice -> choice.machineTypes().stream()).distinct().limit(3).toList();
    }

    private int treeRows() { return Math.max(1, (imageHeight - 83) / 18); }

    private static String compact(long value) {
        if (value < 1_000) return Long.toString(value);
        if (value < 1_000_000) return value / 1_000 + "K";
        if (value < 1_000_000_000) return value / 1_000_000 + "M";
        return value / 1_000_000_000 + "B";
    }

    private static String duration(long ticks) {
        if (ticks == Long.MAX_VALUE) return "paused";
        long seconds = Math.max(1, ticks / 20);
        return seconds < 60 ? seconds + "s" : seconds / 60 + "m " + seconds % 60 + "s";
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void submit() {
        String command = terminalInput.getValue().trim();
        if (command.isEmpty()) return;
        if (history.isEmpty() || !history.getLast().equals(command)) {
            history.add(command);
            if (history.size() > MAX_HISTORY) history.removeFirst();
        }
        historyIndex = history.size();
        terminalInput.setValue("");
        suggestions = List.of();
        if (command.equalsIgnoreCase("clear") || command.equalsIgnoreCase("cls")) {
            output.clear();
            terminalScroll = 0;
            return;
        }
        append(List.of("> " + command));
        if (command.equalsIgnoreCase("history")) {
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < history.size(); i++) lines.add((i + 1) + "  " + history.get(i));
            append(lines);
            return;
        }
        String lower = command.toLowerCase(Locale.ROOT);
        if (lower.equals("jei") || lower.startsWith("jei ")) {
            handleJeiCommand(command);
            return;
        }
        rememberProgrammedRecipe(command);
        PacketDistributor.sendToServer(new C2S_DepotCliRequest(menu.containerId, command, false));
    }

    private static void rememberProgrammedRecipe(String command) {
        List<String> tokens = DepotCliParser.parse(command);
        if (tokens.size() < 4 || !tokens.getFirst().equalsIgnoreCase("recipe")
                || !tokens.get(1).equalsIgnoreCase("add")) return;
        ResourceLocation output = ResourceLocation.tryParse(tokens.get(3));
        if (output != null) net.crystalnexus.client.DepotCliJeiTransferHandler.markProgrammed(output);
    }

    private void handleJeiCommand(String command) {
        List<String> tokens = DepotCliParser.parse(command);
        if (tokens.size() < 2) {
            append(List.of("[INFO] Usage: jei [--machine <id>] <item> [<amount>]"));
            return;
        }
        int machineIndex = tokens.indexOf("--machine");
        String machine = null;
        if (machineIndex >= 0 && machineIndex + 1 < tokens.size()) {
            machine = tokens.get(machineIndex + 1);
            List<String> filtered = new ArrayList<>(tokens);
            filtered.remove(machineIndex + 1);
            filtered.remove(machineIndex);
            tokens = filtered;
        }
        java.util.OptionalInt parsedAmount = DepotCliParser.positiveQuantity(tokens.getLast(), 999999);
        int itemEnd = parsedAmount.isPresent() ? tokens.size() - 1 : tokens.size();
        if (itemEnd < 2) return;
        String itemName = String.join(" ", tokens.subList(1, itemEnd));
        ResourceLocation id = ResourceLocation.tryParse(itemName);
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        if (item != null && item != Items.AIR) CrystalnexusJeiRuntimePlugin.showRecipesFor(new ItemStack(item));
        String craft = "craft " + (machine == null ? "" : "--machine " + machine + " ")
                + itemName + " " + parsedAmount.orElse(1);
        append(List.of("[OK] Showing JEI recipes for " + itemName + "."));
        applySuggestion(craft);
    }

    private void autocomplete() {
        if (!suggestions.isEmpty()) {
            suggestionIndex = (suggestionIndex + 1) % suggestions.size();
            applySuggestion(suggestions.get(suggestionIndex));
        } else PacketDistributor.sendToServer(new C2S_DepotCliRequest(menu.containerId, terminalInput.getValue(), true));
    }

    private void applySuggestion(String suggestion) {
        applyingSuggestion = true;
        terminalInput.setValue(suggestion);
        terminalInput.moveCursorToEnd(false);
        applyingSuggestion = false;
    }

    private void moveHistory(int direction) {
        if (history.isEmpty()) return;
        historyIndex = Mth.clamp(historyIndex + direction, 0, history.size());
        terminalInput.setValue(historyIndex == history.size() ? "" : history.get(historyIndex));
        terminalInput.moveCursorToEnd(false);
    }

    private void append(List<String> lines) {
        output.addAll(lines);
        while (output.size() > MAX_OUTPUT) output.removeFirst();
        terminalScroll = maxTerminalScroll();
    }

    private List<FormattedCharSequence> wrappedOutput() {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (String line : output) {
            int color = terminalColor(line);
            Component styled = Component.literal(line).withStyle(style -> style.withColor(color));
            if (line.isEmpty()) wrapped.add(FormattedCharSequence.EMPTY);
            else wrapped.addAll(font.split(styled, imageWidth - 22));
        }
        return wrapped;
    }

    private int visibleTerminalLines() { return Math.max(1, (imageHeight - 67) / (font.lineHeight + 1)); }
    private int maxTerminalScroll() { return Math.max(0, wrappedOutput().size() - visibleTerminalLines()); }
    private static int terminalColor(String line) {
        if (line.startsWith("[OK]")) return 0xFF69FF91;
        if (line.startsWith("[WARN]")) return 0xFFFFD166;
        if (line.startsWith("[ERROR]")) return 0xFFFF6B6B;
        if (line.startsWith(">")) return 0xFF55FFF2;
        return 0xFFD7E3E2;
    }
}
