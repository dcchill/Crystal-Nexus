package net.crystalnexus.client.gui;

import net.crystalnexus.cli.DepotCliParser;
import net.crystalnexus.automation.DepotProgram;
import net.crystalnexus.cli.DepotCraftingService;
import net.crystalnexus.cli.DepotJeiRecipeCache;
import net.crystalnexus.jei.CrystalnexusJeiRuntimePlugin;
import net.crystalnexus.network.payload.C2S_DepotCliRequest;
import net.crystalnexus.network.payload.C2S_DepotCraftingRequest;
import net.crystalnexus.network.payload.S2C_DepotCliResponse;
import net.crystalnexus.network.payload.S2C_DepotCraftingResponse;
import net.crystalnexus.network.payload.C2S_DepotProgramRequest;
import net.crystalnexus.network.payload.S2C_DepotProgramsResponse;
import net.crystalnexus.world.inventory.DepotCliMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
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
import java.util.UUID;

public class DepotCliScreen extends AbstractContainerScreen<DepotCliMenu> {
    private enum Tab { CRAFTING, PROGRAMS }

    private final Set<Integer> collapsed = new HashSet<>();
    private final Map<Integer, DepotCraftingService.PreviewNode> nodesById = new HashMap<>();
    private final Map<Integer, Integer> nodeDepths = new HashMap<>();
    private final Set<Integer> parents = new HashSet<>();
    private List<DepotCraftingService.CatalogEntry> catalog = List.of();
    private List<DepotCraftingService.PreviewNode> visibleNodeCache = List.of();
    private DepotCraftingService.Preview preview;
    private EditBox searchInput;
    private EditBox amountInput;
    private Button craftingTab;
    private Button programsTab;
    private Button newProgramButton;
    private Button saveProgramButton;
    private Button cancelProgramButton;
    private Button triggerTypeButton;
    private Button conditionTypeButton;
    private Button actionTypeButton;
    private EditBox machineInput;
    private final List<Button> programEditButtons = new ArrayList<>();
    private final List<Button> programDeleteButtons = new ArrayList<>();
    private final List<Button> programToggleButtons = new ArrayList<>();
    private final List<Button> programEditorButtons = new ArrayList<>();
    private Button previousPage;
    private Button nextPage;
    private Button startButton;
    private Button cancelButton;
    private Button automaticButton;
    private Checkbox craftableOnlyCheckbox;
    private Tab tab = Tab.CRAFTING;
    private ResourceLocation selectedTarget;
    private int selectedNodeId;
    private int catalogPage;
    private int catalogPages = 1;
    private int treeScroll;
    private int routeScroll;
    private int searchDelay;
    private int refreshDelay;
    private String lastCatalogQuery;
    private int lastCatalogPage = -1;
    private boolean lastCatalogCraftableOnly;
    private boolean craftableOnly = true;
    private List<DepotProgram> programs = List.of();
    private UUID editingProgramId;
    private DepotProgram.TriggerType editingTrigger = DepotProgram.TriggerType.ITEM_ADDED;
    private DepotProgram.ConditionType editingCondition;
    private DepotProgram.ActionType editingAction = DepotProgram.ActionType.SEND_ITEM;
    private ResourceLocation triggerItem;
    private ResourceLocation conditionItem;
    private ResourceLocation actionItem;
    private ResourceLocation machineItem;
    private EditBox programNameInput;
    private EditBox conditionAmountInput;
    private EditBox actionAmountInput;
    private EditBox triggerItemInput;
    private EditBox conditionItemInput;
    private EditBox actionItemInput;
    private EditBox intervalInput;
    private boolean programEditorOpen;
    private boolean editingProgramEnabled = true;
    private String message = "Select an item to preview its crafting tree.";
    private boolean messageSuccess = true;
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private List<Component> hoveredRecipe = List.of();

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
        int editorFieldX = leftPos + 70;
        int editorTypeWidth = Math.min(150, Math.max(90, (imageWidth - 86) / 2));
        int editorValueX = editorFieldX + editorTypeWidth + 8;
        int editorValueWidth = Math.max(48, leftPos + imageWidth - 8 - editorValueX);
        int editorItemWidth = Math.max(40, editorValueWidth - 70);
        int editorActionY = topPos + Math.min(170, imageHeight - 66);
        int editorMachineY = editorActionY + 24;
        craftingTab = addRenderableWidget(Button.builder(Component.translatable("gui.crystalnexus.depot_cli.crafting"), button -> setTab(Tab.CRAFTING))
                .bounds(leftPos + 8, topPos + 5, 72, 18).build());
        programsTab = addRenderableWidget(Button.builder(Component.literal("Programs"), button -> setTab(Tab.PROGRAMS))
                .bounds(leftPos + 82, topPos + 5, 72, 18).build());
        craftableOnlyCheckbox = addRenderableWidget(Checkbox.builder(
                        Component.translatable("gui.crystalnexus.depot_cli.craftable_only"), font)
                .pos(leftPos + 160, topPos + 5)
                .selected(craftableOnly)
                .onValueChange((checkbox, selected) -> {
                    craftableOnly = selected;
                    catalogPage = 0;
                    requestCatalog(true);
                }).build());
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

        newProgramButton = addRenderableWidget(Button.builder(Component.literal("+ New Program"), button -> editProgram(null))
                .bounds(leftPos + 8, topPos + 31, 100, 18).build());
        saveProgramButton = addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveProgram())
                .bounds(leftPos + 8, topPos + imageHeight - 25, 60, 18).build());
        cancelProgramButton = addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> closeProgramEditor())
                .bounds(leftPos + 72, topPos + imageHeight - 25, 60, 18).build());
        triggerTypeButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            editingTrigger = switch (editingTrigger) {
                case ITEM_ADDED -> DepotProgram.TriggerType.FLUID_ADDED;
                case FLUID_ADDED -> DepotProgram.TriggerType.INVENTORY_CHANGED;
                case INVENTORY_CHANGED -> DepotProgram.TriggerType.TIMED_INTERVAL;
                case TIMED_INTERVAL -> DepotProgram.TriggerType.ITEM_ADDED;
            };
            onTriggerItemTextChanged();
            updateProgramWidgets();
        }).bounds(editorFieldX, topPos + 70, editorTypeWidth, 18).build());
        conditionTypeButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            DepotProgram.ConditionType[] values = DepotProgram.ConditionType.values();
            editingCondition = editingCondition == null ? values[0]
                    : editingCondition.ordinal() + 1 >= values.length ? null : values[editingCondition.ordinal() + 1];
            onConditionItemTextChanged();
            updateProgramWidgets();
        }).bounds(editorFieldX, topPos + 112, editorTypeWidth, 18).build());
        actionTypeButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            DepotProgram.ActionType[] values = DepotProgram.ActionType.values();
            editingAction = values[(editingAction.ordinal() + 1) % values.length];
            onActionItemTextChanged();
            updateProgramWidgets();
        }).bounds(editorFieldX, editorActionY, editorTypeWidth, 18).build());
        triggerItemInput = new EditBox(font, editorValueX, topPos + 70, editorValueWidth, 16, Component.literal("Item"));
        triggerItemInput.setSuggestion("mod:item");
        triggerItemInput.setMaxLength(128);
        triggerItemInput.setResponder(value -> {
            updateSuggestion(triggerItemInput, "mod:item");
            onTriggerItemTextChanged();
        });
        addRenderableWidget(triggerItemInput);
        conditionItemInput = new EditBox(font, editorValueX, topPos + 112, editorItemWidth, 16, Component.literal("Item"));
        conditionItemInput.setSuggestion("mod:item");
        conditionItemInput.setMaxLength(128);
        conditionItemInput.setResponder(value -> {
            updateSuggestion(conditionItemInput, "mod:item");
            onConditionItemTextChanged();
        });
        addRenderableWidget(conditionItemInput);
        actionItemInput = new EditBox(font, editorValueX, editorActionY, editorItemWidth, 16, Component.literal("Output item"));
        actionItemInput.setSuggestion("output item");
        actionItemInput.setMaxLength(128);
        actionItemInput.setResponder(value -> {
            updateSuggestion(actionItemInput, "output item");
            onActionItemTextChanged();
        });
        addRenderableWidget(actionItemInput);
        intervalInput = numberInput(editorValueX, topPos + 70, editorValueWidth, "Ticks");
        intervalInput.setValue("100");
        intervalInput.visible = false;
        addRenderableWidget(intervalInput);
        programNameInput = new EditBox(font, editorFieldX, topPos + 39,
                leftPos + imageWidth - 8 - editorFieldX, 18, Component.literal("Program name"));
        programNameInput.setSuggestion("Program name");
        programNameInput.setMaxLength(64);
        programNameInput.setResponder(value -> updateSuggestion(programNameInput, "Program name"));
        addRenderableWidget(programNameInput);
        conditionAmountInput = numberInput(editorValueX + editorItemWidth + 6, topPos + 112, 64, "Count");
        actionAmountInput = numberInput(editorValueX + editorItemWidth + 6, editorActionY, 64, "Count");
        addRenderableWidget(conditionAmountInput);
        addRenderableWidget(actionAmountInput);
        machineInput = new EditBox(font, editorValueX, editorMachineY, editorValueWidth, 16, Component.literal("Machine"));
        machineInput.setSuggestion("mod:machine");
        machineInput.setMaxLength(128);
        machineInput.setResponder(value -> {
            updateSuggestion(machineInput, "mod:machine");
            machineItem = parseBlock(value);
        });
        addRenderableWidget(machineInput);
        machineInput.visible = false;
        for (int row = 0; row < 6; row++) {
            final int index = row;
            programToggleButtons.add(addRenderableWidget(Button.builder(Component.empty(), button -> programRequest(C2S_DepotProgramRequest.Action.TOGGLE, index))
                    .bounds(leftPos + imageWidth - 190, topPos + 58 + row * 34, 68, 18).build()));
            programEditButtons.add(addRenderableWidget(Button.builder(Component.literal("Edit"), button -> editProgramAt(index))
                    .bounds(leftPos + imageWidth - 118, topPos + 58 + row * 34, 50, 18).build()));
            programDeleteButtons.add(addRenderableWidget(Button.builder(Component.literal("Delete"), button -> programRequest(C2S_DepotProgramRequest.Action.DELETE, index))
                    .bounds(leftPos + imageWidth - 64, topPos + 58 + row * 34, 56, 18).build()));
        }
        updateWidgets();
        requestCatalog(true);
    }

    private EditBox numberInput(int x, int y, int width, String placeholder) {
        EditBox input = new EditBox(font, x, y, width, 18, Component.literal(""));
        input.setSuggestion(placeholder);
        input.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        input.setResponder(value -> updateSuggestion(input, placeholder));
        return input;
    }

    private static void updateSuggestion(EditBox input, String suggestion) {
        input.setSuggestion(input.getValue().isEmpty() ? suggestion : null);
    }

    private void setTab(Tab next) {
        tab = next;
        updateWidgets();
        if (next == Tab.PROGRAMS) {
            PacketDistributor.sendToServer(C2S_DepotProgramRequest.list(menu.containerId));
        } else setInitialFocus(searchInput);
    }

    private void updateWidgets() {
        boolean crafting = tab == Tab.CRAFTING;
        boolean programTabVisible = tab == Tab.PROGRAMS;
        boolean editing = programTabVisible && programEditorOpen;
        searchInput.visible = crafting;
        amountInput.visible = crafting;
        previousPage.visible = crafting;
        nextPage.visible = crafting;
        startButton.visible = crafting;
        cancelButton.visible = crafting && menu.hasJob();
        automaticButton.visible = crafting;
        craftableOnlyCheckbox.visible = crafting;
        craftingTab.active = !crafting;
        programsTab.active = !programTabVisible;
        previousPage.active = catalogPage > 0;
        nextPage.active = catalogPage + 1 < catalogPages;
        startButton.active = preview != null && preview.startable();
        cancelButton.active = menu.hasJob();
        automaticButton.active = selectedNode() != null && selectedNode().selectedRoute() != null;
        newProgramButton.visible = programTabVisible && !editing;
        saveProgramButton.visible = programTabVisible && editing;
        cancelProgramButton.visible = programTabVisible && editing;
        programNameInput.visible = programTabVisible && editing;
        conditionAmountInput.visible = programTabVisible && editing && editingCondition != null
                && (editingCondition == DepotProgram.ConditionType.COUNT_AT_LEAST
                || editingCondition == DepotProgram.ConditionType.COUNT_LESS
                || editingCondition == DepotProgram.ConditionType.FLUID_AT_LEAST
                || editingCondition == DepotProgram.ConditionType.FLUID_LESS);
        actionAmountInput.visible = programTabVisible && editing;
        triggerTypeButton.visible = programTabVisible && editing;
        conditionTypeButton.visible = programTabVisible && editing;
        actionTypeButton.visible = programTabVisible && editing;
        triggerItemInput.visible = programTabVisible && editing && (editingTrigger == DepotProgram.TriggerType.ITEM_ADDED
                || editingTrigger == DepotProgram.TriggerType.FLUID_ADDED);
        conditionItemInput.visible = programTabVisible && editing && editingCondition != null;
        actionItemInput.visible = programTabVisible && editing;
        machineInput.visible = programTabVisible && editing && editingAction == DepotProgram.ActionType.PROCESS;
        intervalInput.visible = programTabVisible && editing && editingTrigger == DepotProgram.TriggerType.TIMED_INTERVAL;
        for (int i = 0; i < programEditButtons.size(); i++) {
            int visibleRows = Math.min(6, Math.max(1, (imageHeight - 91) / 34));
            boolean rowVisible = programTabVisible && !editing && i < programs.size() && i < visibleRows;
            programEditButtons.get(i).visible = rowVisible;
            programDeleteButtons.get(i).visible = rowVisible;
            programToggleButtons.get(i).visible = rowVisible;
        }
        updateProgramWidgets();
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
        if (tab == Tab.PROGRAMS) {
            if (programNameInput.isFocused() && keyCode == GLFW.GLFW_KEY_E) return true;
            if (triggerItemInput != null && triggerItemInput.isFocused() && keyCode == GLFW.GLFW_KEY_E) return true;
            if (conditionItemInput != null && conditionItemInput.isFocused() && keyCode == GLFW.GLFW_KEY_E) return true;
            if (actionItemInput.isFocused() && keyCode == GLFW.GLFW_KEY_E) return true;
            if (intervalInput != null && intervalInput.isFocused() && keyCode == GLFW.GLFW_KEY_E) return true;
            if (machineInput != null && machineInput.isFocused() && keyCode == GLFW.GLFW_KEY_E) return true;
            if (conditionAmountInput.isFocused() && keyCode == GLFW.GLFW_KEY_E) return true;
            if (actionAmountInput.isFocused() && keyCode == GLFW.GLFW_KEY_E) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX >= leftPos + 140 && mouseX < leftPos + imageWidth - 145) {
            treeScroll = Mth.clamp(treeScroll + (deltaY < 0 ? 1 : -1), 0,
                    Math.max(0, visibleNodes().size() - treeRows()));
            return true;
        }
        DepotCraftingService.PreviewNode node = selectedNode();
        if (node != null && mouseX >= leftPos + imageWidth - 142) {
            routeScroll = Mth.clamp(routeScroll + (deltaY < 0 ? 1 : -1), 0,
                    Math.max(0, node.alternatives().size() - routeRows(node)));
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
                if (mouseY >= topPos + 72 && routeIndex >= routeScroll
                        && routeIndex < Math.min(routeScroll + routeRows(node), node.alternatives().size())) {
                    setPreference(C2S_DepotCraftingRequest.Action.SET_ROUTE, node.itemId(),
                            node.alternatives().get(routeIndex).id());
                    return true;
                }
                List<ResourceLocation> machines = machines(node);
                int machineStart = machineStart(machines.size());
                int machineIndex = (int) ((mouseY - machineStart) / 18);
                if (mouseY >= machineStart && machineIndex >= 0 && machineIndex < Math.min(3, machines.size())) {
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
        hoveredRecipe = List.of();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!hoveredRecipe.isEmpty()) graphics.renderComponentTooltip(font, hoveredRecipe, mouseX, mouseY);
        else if (!hoveredStack.isEmpty()) graphics.renderTooltip(font, hoveredStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF080D0D);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 2, 0xFF3FCFC5);
        if (tab == Tab.PROGRAMS) {
            if (programEditorOpen) renderProgramEditor(graphics, mouseX, mouseY);
            else renderPrograms(graphics);
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
            graphics.drawString(font, name, x + 19, y + 1,
                    !craftableOnly || entry.craftable() ? 0xFFDCF8F4 : 0xFFFF9A9A, false);
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
            if (node.source() == DepotCraftingService.PreviewSource.MISSING) {
                label += " [" + compact(node.stored()) + "/" + compact(node.required()) + "]";
            }
            graphics.drawString(font, font.plainSubstrByWidth(label, Math.max(30, imageWidth - 315 - depth * 11)),
                    x + 19, y + 4, color, false);
            if (inside(mouseX, mouseY, x, y, imageWidth - 285, 17)) {
                hoveredStack = stack;
                if (node.source() == DepotCraftingService.PreviewSource.MISSING) hoveredRecipe = problemTooltip(node);
            }
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
        int routeRows = routeRows(node);
        for (int i = 0; i < Math.min(routeRows, node.alternatives().size() - routeScroll); i++) {
            DepotCraftingService.RecipeChoice choice = node.alternatives().get(routeScroll + i);
            int y = topPos + 72 + i * 25;
            boolean preferred = choice.id().equals(node.selectedRoute());
            graphics.fill(x, y, leftPos + imageWidth - 8, y + 22, preferred ? 0xFF176158 : 0xFF111B1B);
            graphics.drawString(font, font.plainSubstrByWidth(choice.category(), 122), x + 3, y + 3,
                    choice.processing() ? 0xFFFFD166 : 0xFF69FF91, false);
            String inputs = choice.inputs().size() + " input" + (choice.inputs().size() == 1 ? "" : "s");
            graphics.drawString(font, inputs, x + 3, y + 12, 0xFF8FA8A5, false);
            if (inside(mouseX, mouseY, x, y, 128, 22)) hoveredRecipe = recipeTooltip(choice);
        }
        List<ResourceLocation> machines = machines(node);
        int machineStart = machineStart(machines.size());
        if (!machines.isEmpty()) graphics.drawString(font, "Machine", x, machineStart - 12, 0xFF55FFF2, false);
        for (int i = 0; i < Math.min(3, machines.size()); i++) {
            ResourceLocation machine = machines.get(i);
            int y = machineStart + i * 18;
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
            String current = "Job 1/" + menu.getJobCount() + " | " + (menu.isProcessing() ? "Machine: " : "Crafting: ");
            current += menu.getCurrentStepAmount() + "x " + menu.getCurrentStep().getHoverName().getString();
            graphics.drawString(font, font.plainSubstrByWidth(current, imageWidth - 250), leftPos + 145, y + 4,
                    0xFFE8F4F3, false);
            graphics.drawString(font, menu.getJobPercent() + "%", leftPos + imageWidth - 112, y + 4,
                    0xFFE8F4F3, false);
        }
    }

    private void renderPrograms(GuiGraphics graphics) {
        int visibleRows = Math.min(6, Math.max(1, (imageHeight - 91) / 34));
        for (int i = 0; i < Math.min(visibleRows, programs.size()); i++) {
            DepotProgram program = programs.get(i);
            int y = topPos + 55 + i * 34;
            graphics.fill(leftPos + 8, y, leftPos + imageWidth - 8, y + 28, 0xFF111B1B);
            graphics.drawString(font, font.plainSubstrByWidth(program.name(), imageWidth - 220), leftPos + 13, y + 4,
                    program.enabled() ? 0xFF69FF91 : 0xFF8FA8A5, false);
            graphics.drawString(font, font.plainSubstrByWidth(program.summary(), imageWidth - 220), leftPos + 13, y + 16,
                    0xFFB8CDCA, false);
        }
    }

    private void renderProgramEditor(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(leftPos + 8, topPos + 31, leftPos + imageWidth - 8, topPos + imageHeight - 31, 0xFF111B1B);
        renderProgramItem(graphics, triggerItem, leftPos + 46, topPos + 71, mouseX, mouseY, false);
        if (editingCondition != null)
            renderProgramItem(graphics, conditionItem, leftPos + 46, topPos + 113, mouseX, mouseY, false);
        int actionY = topPos + Math.min(170, imageHeight - 66);
        renderProgramItem(graphics, actionItem, leftPos + 46, actionY + 1, mouseX, mouseY, false);
        if (editingAction == DepotProgram.ActionType.PROCESS)
            renderProgramItem(graphics, machineItem, leftPos + 46, actionY + 25, mouseX, mouseY, true);
    }

    private void renderProgramItem(GuiGraphics graphics, ResourceLocation id, int x, int y,
            int mouseX, int mouseY, boolean block) {
        if (id == null) return;
        ItemStack stack = block ? new ItemStack(BuiltInRegistries.BLOCK.get(id))
                : new ItemStack(BuiltInRegistries.ITEM.get(id));
        if (stack.isEmpty()) return;
        graphics.renderItem(stack, x, y);
        if (inside(mouseX, mouseY, x, y, 18, 18)) hoveredStack = stack;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String status = menu.isConnected() ? "CONNECTED" : "OFFLINE";
        int statusX = imageWidth - 8 - font.width(status);
        graphics.drawString(font, status, statusX, 9,
                menu.isConnected() ? 0xFF63FF86 : 0xFFFF6565, false);
        if (tab == Tab.PROGRAMS) {
            graphics.drawString(font, programEditorOpen ? "PROGRAM EDITOR" : "DEPOT PROGRAMS", 8, 22, 0xFF55FFF2, false);
            if (programEditorOpen) {
                graphics.drawString(font, "Name", 12, 44, 0xFFB8CDCA, false);
                graphics.drawString(font, "WHEN", 12, 74, 0xFF55FFF2, false);
                graphics.drawString(font, "IF", 12, 116, 0xFF55FFF2, false);
                int actionY = Math.min(174, imageHeight - 62);
                graphics.drawString(font, "DO", 12, actionY, 0xFF55FFF2, false);
                if (editingAction == DepotProgram.ActionType.PROCESS)
                    graphics.drawString(font, "MACHINE", 70, actionY + 24, 0xFF55FFF2, false);
                if (imageHeight >= 270) graphics.drawString(font,
                        "Use registry IDs, such as minecraft:iron_ingot.", 12, 226, 0xFF8FA8A5, false);
                graphics.drawString(font, font.plainSubstrByWidth(message, imageWidth - 148), 140,
                        imageHeight - 20, messageSuccess ? 0xFF69FF91 : 0xFFFF6B6B, false);
            }
            return;
        }
        graphics.drawString(font, "OUTPUTS", 8, 22, 0xFF55FFF2, false);
        graphics.drawString(font, "CRAFTING TREE", 144, 22, 0xFF55FFF2, false);
        graphics.drawString(font, "ROUTE", imageWidth - 136, 22, 0xFF55FFF2, false);
        graphics.drawString(font, (catalogPage + 1) + "/" + catalogPages, 49, imageHeight - 20, 0xFF8FA8A5, false);
        graphics.drawString(font, "Qty", 143, imageHeight - 20, 0xFF8FA8A5, false);
        int messageX = craftableOnlyCheckbox.getX() - leftPos + craftableOnlyCheckbox.getWidth() + 6;
        int messageWidth = statusX - messageX - 6;
        if (messageWidth > 0) {
            graphics.drawString(font, font.plainSubstrByWidth(message, messageWidth), messageX, 10,
                    messageSuccess ? 0xFF69FF91 : 0xFFFF6B6B, false);
        }
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
                        : preview.details().stream().filter(detail -> !detail.endsWith("crafting paths:"))
                                .findFirst().orElse(preview.details().getFirst());
            }
            case RESULT -> {
                messageSuccess = response.success();
                message = response.message();
                refreshDelay = 4;
            }
        }
        updateWidgets();
    }

    public void handleProgramsResponse(S2C_DepotProgramsResponse response) {
        if (response.menuId() != menu.containerId) return;
        programs = response.programs() != null ? response.programs() : List.of();
        closeProgramEditor();
        updateWidgets();
    }

    private void editProgramAt(int index) {
        if (index >= 0 && index < programs.size()) {
            DepotProgram prog = programs.get(index);
            if (prog != null) editProgram(prog);
        }
    }

    private void editProgram(DepotProgram program) {
        programEditorOpen = true;
        messageSuccess = true;
        message = "Drag from JEI or enter registry IDs.";
        editingProgramId = program == null ? UUID.randomUUID() : program.id();
        editingProgramEnabled = program == null || program.enabled();
        programNameInput.setValue(program == null ? "New Program" : program.name());
        DepotProgram.ProgramTrigger progTrigger = program == null ? null : program.trigger();
        boolean hasTrigger = progTrigger != null;
        editingTrigger = hasTrigger ? progTrigger.type() : DepotProgram.TriggerType.ITEM_ADDED;
        triggerItem = hasTrigger ? progTrigger.itemId() : null;
        DepotProgram.ProgramCondition firstCondition = program == null || program.conditions().isEmpty() ? null : program.conditions().getFirst();
        boolean hasCondition = firstCondition != null;
        editingCondition = hasCondition ? firstCondition.type() : null;
        conditionItem = hasCondition ? firstCondition.itemId() : null;
        if (conditionAmountInput != null) conditionAmountInput.setValue(hasCondition ? Long.toString(firstCondition.amount()) : "1");
        DepotProgram.ProgramAction firstAction = program == null || program.actions().isEmpty() ? null : program.actions().getFirst();
        boolean hasAction = firstAction != null;
        editingAction = hasAction ? firstAction.type() : DepotProgram.ActionType.SEND_ITEM;
        actionItem = hasAction ? firstAction.itemId() : null;
        if (actionAmountInput != null) actionAmountInput.setValue(hasAction ? Integer.toString(firstAction.amount()) : "64");
        machineItem = hasAction ? firstAction.machineId() : null;
        // Update text inputs
        if (triggerItemInput != null) triggerItemInput.setValue(triggerItem != null ? triggerItem.toString() : "");
        if (conditionItemInput != null) conditionItemInput.setValue(conditionItem != null ? conditionItem.toString() : "");
        if (actionItemInput != null) actionItemInput.setValue(actionItem != null ? actionItem.toString() : "");
        if (machineInput != null) machineInput.setValue(machineItem != null ? machineItem.toString() : "");
        int interval = hasTrigger && progTrigger.type() == DepotProgram.TriggerType.TIMED_INTERVAL
                ? progTrigger.interval() : 100;
        if (intervalInput != null) intervalInput.setValue(Integer.toString(interval));
        updateWidgets();
        if (programNameInput != null) setInitialFocus(programNameInput);
    }

    private void closeProgramEditor() {
        programEditorOpen = false;
        editingProgramId = null;
        updateWidgets();
    }

    private void saveProgram() {
        if (!programEditorOpen) return;
        if (programNameInput.getValue().isBlank()) {
            programError("Enter a program name.");
            return;
        }
        if ((editingTrigger == DepotProgram.TriggerType.ITEM_ADDED || editingTrigger == DepotProgram.TriggerType.FLUID_ADDED)
                && triggerItem == null) {
            programError("Choose a valid trigger resource.");
            return;
        }
        if (editingCondition != null && conditionItem == null) {
            programError("Choose a valid condition item.");
            return;
        }
        if (actionItem == null) {
            programError("Choose a valid output item.");
            return;
        }
        if (editingAction == DepotProgram.ActionType.PROCESS && machineItem == null) {
            programError("Choose a valid processing machine.");
            return;
        }
        List<DepotProgram.ProgramCondition> conditions = editingCondition == null ? List.of()
                : List.of(new DepotProgram.ProgramCondition(editingCondition, conditionItem, number(conditionAmountInput, 1)));
        int interval = editingTrigger == DepotProgram.TriggerType.TIMED_INTERVAL
                ? (int) Math.max(20, number(intervalInput, 100)) : 0;
        DepotProgram.ProgramTrigger trigger = switch (editingTrigger) {
            case ITEM_ADDED -> new DepotProgram.ProgramTrigger(editingTrigger, triggerItem, 0);
            case FLUID_ADDED -> new DepotProgram.ProgramTrigger(editingTrigger, triggerItem, 0);
            case INVENTORY_CHANGED -> new DepotProgram.ProgramTrigger(editingTrigger, null, 0);
            case TIMED_INTERVAL -> new DepotProgram.ProgramTrigger(editingTrigger, null, interval);
        };
        int amount = (int) Math.min(Integer.MAX_VALUE, number(actionAmountInput, 1));
        DepotProgram.ProgramAction action = editingAction == DepotProgram.ActionType.PROCESS
                ? new DepotProgram.ProgramAction(editingAction, actionItem, amount, machineItem)
                : new DepotProgram.ProgramAction(editingAction, actionItem, amount);
        DepotProgram program = new DepotProgram(editingProgramId, programNameInput.getValue(), editingProgramEnabled,
                trigger, conditions, List.of(action));
        PacketDistributor.sendToServer(new C2S_DepotProgramRequest(menu.containerId,
                C2S_DepotProgramRequest.Action.UPSERT, program.id(), program));
        messageSuccess = true;
        message = "Saving program...";
    }

    private void programError(String error) {
        messageSuccess = false;
        message = error;
    }

    private void programRequest(C2S_DepotProgramRequest.Action action, int index) {
        if (index < 0 || index >= programs.size()) return;
        DepotProgram program = programs.get(index);
        PacketDistributor.sendToServer(new C2S_DepotProgramRequest(menu.containerId, action, program.id(), null));
    }

    private void updateProgramWidgets() {
        if (triggerTypeButton == null) return;
        triggerTypeButton.setMessage(Component.literal(switch (editingTrigger) {
            case ITEM_ADDED -> "Item Added";
            case FLUID_ADDED -> "Fluid Added";
            case INVENTORY_CHANGED -> "Inventory Changed";
            case TIMED_INTERVAL -> "Timed Interval";
        }));
        conditionTypeButton.setMessage(Component.literal(editingCondition == null ? "None" : switch (editingCondition) {
            case COUNT_AT_LEAST -> "Item Count >=";
            case COUNT_LESS -> "Item Count <";
            case EXISTS -> "Item Exists";
            case MISSING -> "Item Missing";
            case FLUID_AT_LEAST -> "Fluid mB >=";
            case FLUID_LESS -> "Fluid mB <";
        }));
        actionTypeButton.setMessage(Component.literal(switch (editingAction) {
            case SEND_ITEM -> "Send Item";
            case SEND_FLUID -> "Send Fluid";
            case CRAFT -> "Craft";
            case PROCESS -> "Process";
        }));
        for (int i = 0; i < programToggleButtons.size(); i++) if (i < programs.size())
            programToggleButtons.get(i).setMessage(Component.literal(programs.get(i).enabled() ? "Enabled" : "Disabled"));
    }

    private void cycleProgramItem(int field, int direction) {
        List<ResourceLocation> items = programItems();
        if (items.isEmpty()) return;
        ResourceLocation current = field == 0 ? triggerItem : field == 1 ? conditionItem : actionItem;
        int index = Math.max(0, items.indexOf(current));
        ResourceLocation next = items.get(Math.floorMod(index + direction, items.size()));
        if (field == 0) triggerItem = next;
        else if (field == 1) conditionItem = next;
        else actionItem = next;
    }

    private List<ResourceLocation> programItems() {
        java.util.LinkedHashSet<ResourceLocation> items = new java.util.LinkedHashSet<>();
        if (selectedTarget != null) items.add(selectedTarget);
        catalog.forEach(entry -> items.add(entry.itemId()));
        if (triggerItem != null) items.add(triggerItem);
        if (conditionItem != null) items.add(conditionItem);
        if (actionItem != null) items.add(actionItem);
        return List.copyOf(items);
    }

    private static long number(EditBox input, long fallback) {
        try { return Math.max(0, Long.parseLong(input.getValue())); }
        catch (NumberFormatException exception) { return fallback; }
    }

    private void onTriggerItemTextChanged() {
        if (triggerItemInput == null) return;
        triggerItem = editingTrigger == DepotProgram.TriggerType.FLUID_ADDED
                ? parseFluid(triggerItemInput.getValue()) : parseItem(triggerItemInput.getValue());
    }

    private void onConditionItemTextChanged() {
        if (conditionItemInput == null) return;
        conditionItem = editingCondition == DepotProgram.ConditionType.FLUID_AT_LEAST
                || editingCondition == DepotProgram.ConditionType.FLUID_LESS
                ? parseFluid(conditionItemInput.getValue()) : parseItem(conditionItemInput.getValue());
    }

    private void onActionItemTextChanged() {
        if (actionItemInput == null) return;
        actionItem = editingAction == DepotProgram.ActionType.SEND_FLUID
                ? parseFluid(actionItemInput.getValue()) : parseItem(actionItemInput.getValue());
    }

    private ResourceLocation parseItem(String text) {
        if (text == null || text.isBlank()) return null;
        ResourceLocation id = ResourceLocation.tryParse(text.trim());
        if (id == null) return null;
        Item item = BuiltInRegistries.ITEM.get(id);
        return (item != null && item != Items.AIR) ? id : null;
    }

    private ResourceLocation parseBlock(String text) {
        if (text == null || text.isBlank()) return null;
        ResourceLocation id = ResourceLocation.tryParse(text.trim());
        return id != null && BuiltInRegistries.BLOCK.get(id) != net.minecraft.world.level.block.Blocks.AIR ? id : null;
    }

    private ResourceLocation parseFluid(String text) {
        if (text == null || text.isBlank()) return null;
        ResourceLocation id = ResourceLocation.tryParse(text.trim());
        return id != null && BuiltInRegistries.FLUID.containsKey(id)
                && BuiltInRegistries.FLUID.get(id) != net.minecraft.world.level.material.Fluids.EMPTY ? id : null;
    }

    public List<EditBox> jeiProgramItemInputs() {
        if (tab != Tab.PROGRAMS || !programEditorOpen) return List.of();
        List<EditBox> inputs = new ArrayList<>();
        if (triggerItemInput.visible) inputs.add(triggerItemInput);
        if (conditionItemInput.visible) inputs.add(conditionItemInput);
        if (actionItemInput.visible) inputs.add(actionItemInput);
        return List.copyOf(inputs);
    }

    public EditBox jeiProgramMachineInput() {
        return tab == Tab.PROGRAMS && programEditorOpen && machineInput.visible ? machineInput : null;
    }

    public void acceptJeiIngredient(EditBox input, ItemStack stack) {
        if (input == null || stack.isEmpty()) return;
        ResourceLocation id;
        if (input == machineInput) {
            if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) return;
            id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        } else id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        input.setValue(id.toString());
        input.setFocused(true);
    }

    private void requestCatalog(boolean force) {
        if (tab != Tab.CRAFTING) return;
        String query = searchInput == null ? "" : searchInput.getValue();
        if (!force && query.equals(lastCatalogQuery) && catalogPage == lastCatalogPage
                && craftableOnly == lastCatalogCraftableOnly) return;
        lastCatalogQuery = query;
        lastCatalogPage = catalogPage;
        lastCatalogCraftableOnly = craftableOnly;
        PacketDistributor.sendToServer(new C2S_DepotCraftingRequest(menu.containerId,
                C2S_DepotCraftingRequest.Action.CATALOG, query,
                catalogPage, craftableOnly, C2S_DepotCraftingRequest.NONE, 0, C2S_DepotCraftingRequest.NONE,
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
        if (node.selectedRoute() == null) return List.of();
        return node.alternatives().stream().filter(choice -> choice.id().equals(node.selectedRoute()))
                .flatMap(choice -> choice.machineTypes().stream()).distinct().limit(3).toList();
    }

    private int routeRows(DepotCraftingService.PreviewNode node) {
        List<ResourceLocation> machines = machines(node);
        int bottom = machines.isEmpty() ? topPos + imageHeight - 31 : machineStart(machines.size()) - 12;
        return Math.max(1, (bottom - topPos - 72) / 25);
    }

    private int machineStart(int machineCount) {
        return topPos + imageHeight - 31 - Math.min(3, machineCount) * 18;
    }

    private List<Component> recipeTooltip(DepotCraftingService.RecipeChoice choice) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(choice.category()).withStyle(ChatFormatting.GOLD));
        if (choice.id().equals(DepotCraftingService.NO_RECIPE_ROUTE)) {
            lines.add(Component.literal("Treat this item as a required external input.")
                    .withStyle(ChatFormatting.AQUA));
            return List.copyOf(lines);
        }
        lines.add(Component.literal(choice.id().toString()).withStyle(ChatFormatting.DARK_GRAY));
        ItemStack output = choice.output();
        lines.add(Component.literal("Output: " + output.getCount() + "x " + output.getHoverName().getString())
                .withStyle(ChatFormatting.GREEN));
        for (int i = 0; i < choice.inputs().size(); i++) {
            DepotJeiRecipeCache.Slot slot = choice.inputs().get(i);
            String alternatives = slot.alternatives().stream().map(stack -> stack.count() + "x "
                    + new ItemStack(BuiltInRegistries.ITEM.get(stack.itemId())).getHoverName().getString())
                    .reduce((left, right) -> left + " / " + right).orElse("Unknown");
            lines.add(Component.literal("Input " + (i + 1) + ": " + alternatives));
        }
        if (!choice.machineTypes().isEmpty()) {
            String machines = choice.machineTypes().stream().map(id -> new ItemStack(BuiltInRegistries.BLOCK.get(id))
                    .getHoverName().getString()).distinct().reduce((left, right) -> left + " / " + right).orElse("");
            lines.add(Component.literal("Machine: " + machines).withStyle(ChatFormatting.AQUA));
        }
        return List.copyOf(lines);
    }

    private List<Component> problemTooltip(DepotCraftingService.PreviewNode node) {
        ItemStack item = new ItemStack(BuiltInRegistries.ITEM.get(node.itemId()));
        long missing = Math.max(0, node.required() - node.stored());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Cannot craft " + item.getHoverName().getString()).withStyle(ChatFormatting.RED));
        lines.add(Component.literal("Required: " + node.required()));
        lines.add(Component.literal("Stored: " + node.stored()));
        lines.add(Component.literal("Missing: " + missing).withStyle(ChatFormatting.RED));
        if (preview != null) preview.details().stream().filter(detail -> !detail.endsWith("crafting paths:"))
                .forEach(detail -> lines.add(Component.literal(detail).withStyle(ChatFormatting.YELLOW)));
        if (!node.alternatives().isEmpty()) {
            lines.add(Component.literal("Select this node to inspect its routes.").withStyle(ChatFormatting.AQUA));
        }
        return List.copyOf(lines);
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

    private static void rememberProgrammedRecipe(String command) {
        List<String> tokens = DepotCliParser.parse(command);
        if (tokens.size() < 4 || !tokens.getFirst().equalsIgnoreCase("recipe")
                || !tokens.get(1).equalsIgnoreCase("add")) return;
        ResourceLocation output = ResourceLocation.tryParse(tokens.get(3));
        if (output != null) net.crystalnexus.client.DepotCliJeiTransferHandler.markProgrammed(output);
    }
}
