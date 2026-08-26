package net.crystalnexus.client.gui;

import net.crystalnexus.cli.DepotCraftingService;
import net.crystalnexus.cli.DepotJeiRecipeCache;
import net.crystalnexus.network.payload.C2S_DepotCraftingRequest;
import net.crystalnexus.network.payload.S2C_DepotCraftingResponse;
import net.crystalnexus.network.payload.C2S_DepotProgramRequest;
import net.crystalnexus.network.payload.S2C_DepotProgramResponse;
import net.crystalnexus.program.DepotProgram;
import net.crystalnexus.program.DepotProgramBlocks;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
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
import java.util.LinkedHashMap;

public class DepotCliScreen extends AbstractContainerScreen<DepotCliMenu> {
    private enum Tab { PROGRAMS, CRAFTING }

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
    private Button previousPage;
    private Button nextPage;
    private Button startButton;
    private Button cancelButton;
    private Button automaticButton;
    private Checkbox craftableOnlyCheckbox;
    private Tab tab = Tab.PROGRAMS;
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
    private String message = "Select an item to preview its crafting tree.";
    private boolean messageSuccess = true;
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private List<Component> hoveredRecipe = List.of();
    private List<S2C_DepotProgramResponse.Summary> programSummaries = List.of();
    private final List<DepotProgram.Node> programBody = new ArrayList<>();
    private List<DepotProgram.Variable> programVariables = List.of();
    private UUID programId = UUID.randomUUID();
    private int programRevision;
    private int programIndex;
    private boolean programDirty;
    private boolean programStarted;
    private String programRunStatus = "IDLE";
    private UUID programCurrentNode = S2C_DepotProgramResponse.NONE;
    private final Map<UUID, String> programProblems = new HashMap<>();
    private DepotProgramBlocks.Definition draggedDefinition;
    private DepotProgram.Node draggedNode;
    private UUID selectedProgramNode;
    private double dragMouseX;
    private double dragMouseY;
    private double workspacePanX;
    private double workspacePanY;
    private double workspaceZoom = 1.0;
    private boolean panning;
    private int paletteScroll;
    private int programStatusDelay;
    private EditBox programName;
    private EditBox programArguments;
    private EditBox programVariableInput;
    private Button programTab;
    private Button newProgramButton;
    private Button saveProgramButton;
    private Button deleteProgramButton;
    private Button testProgramButton;
    private Button runProgramButton;
    private Button stopProgramButton;
    private Button previousProgramButton;
    private Button nextProgramButton;
    private Button applyProgramInputButton;
    private final Map<UUID, ProgramRect> programHitboxes = new HashMap<>();
    private boolean loadingProgram;
    private record ProgramRect(int x, int y, int width, int height) {
        boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }

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
        programTab = addRenderableWidget(Button.builder(Component.translatable("gui.crystalnexus.depot_program.programs"), button -> setTab(Tab.PROGRAMS))
                .bounds(leftPos + 8, topPos + 5, 72, 18).build());
        craftingTab = addRenderableWidget(Button.builder(Component.translatable("gui.crystalnexus.depot_cli.crafting"), button -> setTab(Tab.CRAFTING))
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

        programName = new EditBox(font, leftPos + 34, topPos + 29, 104, 16, Component.literal("Program name"));
        programName.setMaxLength(32);
        programName.setValue("New Program");
        programName.setResponder(value -> { if (!loadingProgram) markProgramDirty(); });
        addRenderableWidget(programName);
        previousProgramButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleProgram(-1))
                .bounds(leftPos + 8, topPos + 28, 22, 18).build());
        nextProgramButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleProgram(1))
                .bounds(leftPos + 142, topPos + 28, 22, 18).build());
        newProgramButton = addRenderableWidget(Button.builder(Component.literal("New"), button -> newProgram())
                .bounds(leftPos + 170, topPos + 28, 42, 18).build());
        saveProgramButton = addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveProgram())
                .bounds(leftPos + 214, topPos + 28, 42, 18).build());
        deleteProgramButton = addRenderableWidget(Button.builder(Component.literal("Delete"), button -> deleteProgram())
                .bounds(leftPos + 258, topPos + 28, 48, 18).build());
        testProgramButton = addRenderableWidget(Button.builder(Component.literal("Test"), button -> testProgram())
                .bounds(leftPos + 308, topPos + 28, 42, 18).build());
        runProgramButton = addRenderableWidget(Button.builder(Component.literal("Run"), button -> runProgram())
                .bounds(leftPos + 352, topPos + 28, 42, 18).build());
        stopProgramButton = addRenderableWidget(Button.builder(Component.literal("Stop"), button -> stopProgram())
                .bounds(leftPos + 396, topPos + 28, 42, 18).build());
        programVariableInput = new EditBox(font, leftPos + imageWidth - 147, topPos + 59, 139, 16, Component.literal("Variables"));
        programVariableInput.setMaxLength(128);
        programVariableInput.setHint(Component.literal("count:number=0"));
        programVariableInput.setResponder(value -> { if (!loadingProgram) markProgramDirty(); });
        addRenderableWidget(programVariableInput);
        programArguments = new EditBox(font, leftPos + imageWidth - 147, topPos + 99, 139, 16, Component.literal("Block inputs"));
        programArguments.setMaxLength(256);
        programArguments.setHint(Component.literal("item=minecraft:iron_ingot, amount=1"));
        addRenderableWidget(programArguments);
        applyProgramInputButton = addRenderableWidget(Button.builder(Component.literal("Apply inputs"), button -> applyProgramInputs())
                .bounds(leftPos + imageWidth - 147, topPos + 119, 90, 18).build());
        updateWidgets();
        requestPrograms(C2S_DepotProgramRequest.Action.LIST, C2S_DepotProgramRequest.NONE, null);
    }

    private void setTab(Tab next) {
        tab = next;
        updateWidgets();
        if (next == Tab.PROGRAMS) {
            setInitialFocus(programName);
            if (!programStarted) {
                programStarted = true;
                requestPrograms(C2S_DepotProgramRequest.Action.LIST, C2S_DepotProgramRequest.NONE, null);
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
        craftableOnlyCheckbox.visible = crafting;
        programName.visible = !crafting;
        programVariableInput.visible = !crafting;
        programArguments.visible = !crafting;
        programTab.active = crafting;
        craftingTab.active = !crafting;
        for (Button button : List.of(newProgramButton, saveProgramButton, deleteProgramButton, testProgramButton,
                runProgramButton, stopProgramButton, previousProgramButton, nextProgramButton, applyProgramInputButton)) {
            if (button != null) button.visible = !crafting;
        }
        saveProgramButton.active = programDirty;
        deleteProgramButton.active = programRevision > 0 && !programRunStatus.startsWith("RUNNING") && !programRunStatus.startsWith("WAITING");
        runProgramButton.active = programRevision > 0 && !programDirty && programProblems.isEmpty()
                && !programRunStatus.startsWith("RUNNING") && !programRunStatus.startsWith("WAITING");
        stopProgramButton.active = programRunStatus.startsWith("RUNNING") || programRunStatus.startsWith("WAITING");
        applyProgramInputButton.active = selectedProgramNode != null;
        previousPage.active = catalogPage > 0;
        nextPage.active = catalogPage + 1 < catalogPages;
        startButton.active = preview != null && preview.startable() && !menu.hasJob();
        cancelButton.active = menu.hasJob();
        automaticButton.active = selectedNode() != null && selectedNode().selectedRoute() != null;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        DepotProgram.Node jeiBlock = net.crystalnexus.client.DepotCliJeiTransferHandler.pendingBlock.getAndSet(null);
        if (jeiBlock != null) {
            programBody.add(jeiBlock);
            selectedProgramNode = jeiBlock.id();
            programArguments.setValue(arguments(jeiBlock));
            markProgramDirty();
            messageSuccess = true;
            message = "JEI recipe added to the program.";
        }
        if (searchDelay > 0 && --searchDelay == 0) {
            catalogPage = 0;
            requestCatalog(false);
        }
        if (refreshDelay > 0 && --refreshDelay == 0) {
            requestPreview();
        }
        if (tab == Tab.PROGRAMS && ++programStatusDelay >= 20) {
            programStatusDelay = 0;
            requestPrograms(C2S_DepotProgramRequest.Action.STATUS, programId, null);
        }
        updateWidgets();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (draggedDefinition != null || draggedNode != null || panning) {
                draggedDefinition = null; draggedNode = null; panning = false;
                return true;
            }
            onClose();
            return true;
        }
        if (getFocused() instanceof EditBox && minecraft != null
                && minecraft.options.keyInventory.matches(keyCode, scanCode)) return true;
        if (tab == Tab.CRAFTING) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (searchInput.isFocused()) requestCatalog(true);
                else if (amountInput.isFocused()) requestPreview();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE && selectedProgramNode != null) {
            if (removeNode(programBody, selectedProgramNode) != null) markProgramDirty();
            selectedProgramNode = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (tab == Tab.PROGRAMS) {
            if (mouseX < leftPos + 120) paletteScroll = Math.max(0, paletteScroll + (deltaY < 0 ? 1 : -1));
            else workspaceZoom = Mth.clamp(workspaceZoom + deltaY * 0.1, 0.5, 1.5);
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
                    Math.max(0, node.alternatives().size() - routeRows(node)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == Tab.PROGRAMS) {
            if (button == 1 || button == 2) {
                panning = true; dragMouseX = mouseX; dragMouseY = mouseY; return true;
            }
            if (button == 0 && mouseY >= topPos + 52) {
                int paletteIndex = paletteScroll + (int) ((mouseY - topPos - 58) / 24);
                List<DepotProgramBlocks.Definition> palette = paletteDefinitions();
                if (mouseX >= leftPos + 7 && mouseX < leftPos + 119 && paletteIndex >= 0 && paletteIndex < palette.size()) {
                    draggedDefinition = palette.get(paletteIndex); dragMouseX = mouseX; dragMouseY = mouseY; return true;
                }
                DepotProgram.Node hit = nodeAt(mouseX, mouseY);
                if (hit != null) {
                    selectedProgramNode = hit.id();
                    programArguments.setValue(arguments(hit));
                    draggedNode = removeNode(programBody, hit.id());
                    dragMouseX = mouseX; dragMouseY = mouseY;
                    return true;
                }
            }
        }
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (tab == Tab.PROGRAMS && panning) {
            workspacePanX += mouseX - dragMouseX; workspacePanY += mouseY - dragMouseY;
            dragMouseX = mouseX; dragMouseY = mouseY; return true;
        }
        if (tab == Tab.PROGRAMS && (draggedDefinition != null || draggedNode != null)) {
            dragMouseX = mouseX; dragMouseY = mouseY; return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (tab == Tab.PROGRAMS && (button == 1 || button == 2)) { panning = false; return true; }
        if (tab == Tab.PROGRAMS && button == 0 && (draggedDefinition != null || draggedNode != null)) {
            DepotProgram.Node node = draggedNode != null ? draggedNode : defaultNode(draggedDefinition);
            boolean trash = mouseX >= leftPos + imageWidth - 55 && mouseY >= topPos + imageHeight - 34;
            if (!trash) dropNode(node, mouseX, mouseY);
            draggedDefinition = null; draggedNode = null;
            markProgramDirty();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
            renderProgramEditor(graphics, mouseX, mouseY);
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
        int statusX = imageWidth - 8 - font.width(status);
        graphics.drawString(font, status, statusX, 9,
                menu.isConnected() ? 0xFF63FF86 : 0xFFFF6565, false);
        if (tab == Tab.PROGRAMS) {
            graphics.drawString(font, "BLOCKS", 8, 48, 0xFF55FFF2, false);
            graphics.drawString(font, "WORKSPACE", 126, 48, 0xFF55FFF2, false);
            graphics.drawString(font, "INSPECTOR", imageWidth - 147, 48, 0xFF55FFF2, false);
            graphics.drawString(font, "Variables", imageWidth - 147, 78, 0xFF8FA8A5, false);
            graphics.drawString(font, "Selected block inputs", imageWidth - 147, 88, 0xFF8FA8A5, false);
            graphics.drawString(font, font.plainSubstrByWidth(message, imageWidth - 175), 8, imageHeight - 17,
                    messageSuccess ? 0xFF69FF91 : 0xFFFF6B6B, false);
            graphics.drawString(font, programRunStatus, imageWidth - 145, imageHeight - 17,
                    programRunStatus.equals("ERROR") ? 0xFFFF6B6B : 0xFFFFD166, false);
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

    public void handleProgramResponse(S2C_DepotProgramResponse response) {
        if (response.menuId() != menu.containerId) return;
        programSummaries = response.programs();
        programRunStatus = response.runStatus();
        programCurrentNode = response.currentNode();
        programProblems.clear();
        response.problems().forEach(problem -> programProblems.put(problem.nodeId(), problem.message()));
        messageSuccess = response.success();
        message = response.message();
        if (!response.selected().isEmpty()) installProgram(DepotProgram.load(response.selected()));
        else if (!programStarted && !programSummaries.isEmpty()) {
            programStarted = true;
            programIndex = 0;
            requestPrograms(C2S_DepotProgramRequest.Action.LOAD, programSummaries.getFirst().id(), null);
        }
        updateWidgets();
    }

    private void requestPrograms(C2S_DepotProgramRequest.Action action, UUID id, DepotProgram program) {
        PacketDistributor.sendToServer(new C2S_DepotProgramRequest(menu.containerId, action,
                id == null ? C2S_DepotProgramRequest.NONE : id, program == null ? new net.minecraft.nbt.CompoundTag() : program.save()));
    }

    private DepotProgram draftProgram() {
        return new DepotProgram(programId, programName.getValue().trim(), programRevision,
                parseVariables(), List.copyOf(programBody));
    }

    private void installProgram(DepotProgram program) {
        loadingProgram = true;
        programId = program.id();
        programRevision = program.revision();
        programName.setValue(program.name());
        programVariables = program.variables();
        programVariableInput.setValue(formatVariables(program.variables()));
        programBody.clear();
        programBody.addAll(program.body());
        selectedProgramNode = null;
        programArguments.setValue("");
        programDirty = false;
        loadingProgram = false;
        for (int i = 0; i < programSummaries.size(); i++) if (programSummaries.get(i).id().equals(program.id())) programIndex = i;
    }

    private void newProgram() {
        installProgram(DepotProgram.empty("New Program"));
        programDirty = true;
        programProblems.clear();
        message = "Drag a block from the palette into the workspace.";
        messageSuccess = true;
    }

    private void cycleProgram(int direction) {
        if (programSummaries.isEmpty()) return;
        programIndex = Math.floorMod(programIndex + direction, programSummaries.size());
        requestPrograms(C2S_DepotProgramRequest.Action.LOAD, programSummaries.get(programIndex).id(), null);
    }

    private void saveProgram() { requestPrograms(C2S_DepotProgramRequest.Action.SAVE, programId, draftProgram()); }
    private void testProgram() { requestPrograms(C2S_DepotProgramRequest.Action.VALIDATE, programId, draftProgram()); }
    private void runProgram() { requestPrograms(C2S_DepotProgramRequest.Action.RUN, programId, null); }
    private void stopProgram() { requestPrograms(C2S_DepotProgramRequest.Action.CANCEL, programId, null); }
    private void deleteProgram() { requestPrograms(C2S_DepotProgramRequest.Action.DELETE, programId, null); newProgram(); }

    private void markProgramDirty() {
        if (!loadingProgram) programDirty = true;
        programProblems.clear();
    }

    private List<DepotProgram.Variable> parseVariables() {
        String raw = programVariableInput.getValue().trim();
        if (raw.isEmpty()) return List.of();
        List<DepotProgram.Variable> result = new ArrayList<>();
        for (String declaration : raw.split(";")) {
            String[] assignment = declaration.trim().split("=", 2);
            String[] named = assignment[0].trim().split(":", 2);
            if (named.length != 2) continue;
            DepotProgram.ValueType type;
            try { type = DepotProgram.ValueType.valueOf(named[1].trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { continue; }
            String value = assignment.length == 2 ? assignment[1].trim() : "";
            DepotProgram.Value initial;
            try {
                initial = switch (type) {
                    case NUMBER -> DepotProgram.Value.number(value.isEmpty() ? 0 : Long.parseLong(value));
                    case BOOLEAN -> DepotProgram.Value.bool(Boolean.parseBoolean(value));
                    default -> DepotProgram.Value.text(type, value);
                };
            } catch (NumberFormatException ignored) { initial = DepotProgram.Value.number(0); }
            result.add(new DepotProgram.Variable(named[0].trim(), type, initial));
        }
        programVariables = List.copyOf(result);
        return programVariables;
    }

    private static String formatVariables(List<DepotProgram.Variable> variables) {
        return variables.stream().map(variable -> variable.name() + ":" + variable.type().name().toLowerCase(Locale.ROOT)
                + "=" + switch (variable.type()) {
                    case NUMBER -> Long.toString(variable.initial().number());
                    case BOOLEAN -> Boolean.toString(variable.initial().bool());
                    default -> variable.initial().text();
                }).collect(java.util.stream.Collectors.joining("; "));
    }

    private List<DepotProgramBlocks.Definition> paletteDefinitions() {
        return DepotProgramBlocks.all();
    }

    private DepotProgram.Node defaultNode(DepotProgramBlocks.Definition definition) {
        Map<String, DepotProgram.Node> inputs = new LinkedHashMap<>();
        for (DepotProgramBlocks.Input input : definition.inputs()) inputs.put(input.name(), defaultLiteral(input.type()));
        Map<String, List<DepotProgram.Node>> stacks = new LinkedHashMap<>();
        definition.stacks().forEach(stack -> stacks.put(stack, List.of()));
        Map<String, String> fields = new LinkedHashMap<>();
        switch (definition.opcode()) {
            case "number" -> fields.put("number", "1");
            case "boolean" -> fields.put("bool", "true");
            case "text" -> fields.put("text", "text");
            case "item" -> fields.put("text", "minecraft:iron_ingot");
            case "machine" -> fields.put("text", "minecraft:furnace");
        }
        if (definition.opcode().startsWith("variable_")) {
            DepotProgram.ValueType wanted = definition.output();
            DepotProgram.Variable variable = programVariables.stream().filter(candidate -> candidate.type() == wanted)
                    .findFirst().orElse(null);
            fields.put("variable", variable == null ? "variable" : variable.name().toLowerCase(Locale.ROOT));
        }
        if (definition.opcode().equals("set_variable") || definition.opcode().equals("change_variable")) {
            DepotProgram.Variable variable = programVariables.isEmpty() ? null : programVariables.getFirst();
            fields.put("variable", variable == null ? "variable" : variable.name().toLowerCase(Locale.ROOT));
            if (definition.opcode().equals("set_variable") && variable != null)
                inputs.put("value", defaultLiteral(variable.type()));
        }
        if (definition.opcode().equals("define_pattern")) {
            fields.put("output", "minecraft:iron_ingot"); fields.put("amount", "1");
            fields.put("machine", "minecraft:furnace"); fields.put("inputs", "minecraft:iron_ore=1");
            fields.put("outputs", "minecraft:iron_ingot=1");
        }
        return new DepotProgram.Node(UUID.randomUUID(), definition.opcode(), fields, inputs, stacks);
    }

    private static DepotProgram.Node defaultLiteral(DepotProgram.ValueType type) {
        return DepotProgram.Node.literal(switch (type) {
            case NUMBER -> DepotProgram.Value.number(1);
            case BOOLEAN -> DepotProgram.Value.bool(true);
            case ITEM -> DepotProgram.Value.text(type, "minecraft:iron_ingot");
            case BLOCK -> DepotProgram.Value.text(type, "minecraft:furnace");
            case TEXT -> DepotProgram.Value.text(type, "minecraft:crafting_table");
        });
    }

    private void applyProgramInputs() {
        DepotProgram.Node selected = findNode(programBody, selectedProgramNode);
        if (selected == null) return;
        DepotProgramBlocks.Definition definition = DepotProgramBlocks.get(selected.opcode());
        if (definition == null) return;
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : programArguments.getValue().split(",")) {
            String[] split = pair.trim().split("=", 2);
            if (split.length == 2) values.put(split[0].trim(), split[1].trim());
        }
        Map<String, DepotProgram.Node> inputs = new LinkedHashMap<>(selected.inputs());
        for (DepotProgramBlocks.Input input : definition.inputs()) {
            String value = values.get(input.name());
            if (value != null) inputs.put(input.name(), literal(input.type(), value));
        }
        Map<String, String> fields = new LinkedHashMap<>(selected.fields());
        values.forEach((key, value) -> { if (definition.inputs().stream().noneMatch(input -> input.name().equals(key))) fields.put(key, value); });
        replaceNode(programBody, selected.id(), new DepotProgram.Node(selected.id(), selected.opcode(), fields, inputs, selected.stacks()));
        parseVariables();
        markProgramDirty();
    }

    private static DepotProgram.Node literal(DepotProgram.ValueType type, String value) {
        try {
            return DepotProgram.Node.literal(switch (type) {
                case NUMBER -> DepotProgram.Value.number(Long.parseLong(value));
                case BOOLEAN -> DepotProgram.Value.bool(Boolean.parseBoolean(value));
                default -> DepotProgram.Value.text(type, value);
            });
        } catch (NumberFormatException ignored) { return defaultLiteral(type); }
    }

    private static String arguments(DepotProgram.Node node) {
        List<String> values = new ArrayList<>();
        node.inputs().forEach((key, input) -> values.add(key + "=" + literalText(input)));
        node.fields().forEach((key, value) -> values.add(key + "=" + value));
        return String.join(", ", values);
    }

    private static String literalText(DepotProgram.Node node) {
        if (!node.opcode().equals("literal")) return node.opcode();
        return switch (node.fields().getOrDefault("type", "TEXT")) {
            case "NUMBER" -> node.fields().getOrDefault("number", "0");
            case "BOOLEAN" -> node.fields().getOrDefault("bool", "false");
            default -> node.fields().getOrDefault("text", "");
        };
    }

    private void renderProgramEditor(GuiGraphics graphics, int mouseX, int mouseY) {
        int paletteRight = leftPos + 120;
        int inspectorLeft = leftPos + imageWidth - 153;
        graphics.fill(paletteRight, topPos + 50, paletteRight + 1, topPos + imageHeight - 22, 0xFF245E5A);
        graphics.fill(inspectorLeft, topPos + 50, inspectorLeft + 1, topPos + imageHeight - 22, 0xFF245E5A);
        graphics.fill(inspectorLeft + 5, topPos + 139, leftPos + imageWidth - 7, topPos + imageHeight - 40, 0xFF111B1B);
        graphics.fill(leftPos + imageWidth - 55, topPos + imageHeight - 34,
                leftPos + imageWidth - 8, topPos + imageHeight - 22, 0xFF6A2630);
        graphics.drawString(font, "TRASH", leftPos + imageWidth - 49, topPos + imageHeight - 31, 0xFFFFC1C7, false);

        List<DepotProgramBlocks.Definition> palette = paletteDefinitions();
        graphics.enableScissor(leftPos + 5, topPos + 55, paletteRight - 3, topPos + imageHeight - 23);
        int paletteEnd = Math.min(palette.size(), paletteScroll + Math.max(1, (imageHeight - 80) / 24));
        for (int i = paletteScroll; i < paletteEnd; i++) {
            DepotProgramBlocks.Definition definition = palette.get(i);
            int y = topPos + 58 + (i - paletteScroll) * 24;
            drawProgramBlock(graphics, definition, leftPos + 7, y, 108, 20, false, false);
            graphics.drawString(font, font.plainSubstrByWidth(definition.label(), 98), leftPos + 13, y + 6, 0xFFFFFFFF, false);
            if (inside(mouseX, mouseY, leftPos + 7, y, 108, 20))
                hoveredRecipe = List.of(Component.literal(definition.category().name()), Component.literal(definition.tooltip()));
        }
        graphics.disableScissor();

        programHitboxes.clear();
        graphics.enableScissor(paletteRight + 2, topPos + 53, inspectorLeft - 2, topPos + imageHeight - 23);
        int x = (int) (paletteRight + 10 + workspacePanX);
        int y = (int) (topPos + 61 + workspacePanY);
        int workspaceWidth = inspectorLeft - x - 8;
        int hatWidth = Math.max(80, Math.min(210, workspaceWidth));
        int hatHeight = Math.max(18, (int) (22 * workspaceZoom));
        graphics.fill(x + 12, y - 5, x + 55, y, 0xFFFFC94A);
        graphics.fill(x, y, x + hatWidth, y + hatHeight, 0xFFFFB52E);
        graphics.drawString(font, "when Run clicked", x + 8, y + Math.max(5, hatHeight / 2 - 4), 0xFF2A2100, false);
        y += hatHeight;
        for (DepotProgram.Node node : programBody) y = renderProgramNode(graphics, node, x, y, hatWidth, 0);
        graphics.disableScissor();

        DepotProgram.Node selected = findNode(programBody, selectedProgramNode);
        if (selected != null) {
            DepotProgramBlocks.Definition definition = DepotProgramBlocks.get(selected.opcode());
            graphics.drawString(font, definition == null ? selected.opcode() : definition.label(),
                    inspectorLeft + 7, topPos + 143, 0xFFE8F4F3, false);
            String error = programProblems.get(selected.id());
            if (error != null) graphics.drawWordWrap(font, Component.literal(error), inspectorLeft + 7, topPos + 157, 136, 0xFFFF6B6B);
        } else graphics.drawString(font, "Select a block", inspectorLeft + 7, topPos + 143, 0xFF8FA8A5, false);

        if (draggedDefinition != null) drawProgramBlock(graphics, draggedDefinition, (int) dragMouseX - 45,
                (int) dragMouseY - 10, 110, 20, false, false);
        else if (draggedNode != null) {
            DepotProgramBlocks.Definition definition = DepotProgramBlocks.get(draggedNode.opcode());
            if (definition != null) drawProgramBlock(graphics, definition, (int) dragMouseX - 45,
                    (int) dragMouseY - 10, 110, 20, true, programProblems.containsKey(draggedNode.id()));
        }
    }

    private int renderProgramNode(GuiGraphics graphics, DepotProgram.Node node, int x, int y, int width, int depth) {
        DepotProgramBlocks.Definition definition = DepotProgramBlocks.get(node.opcode());
        if (definition == null) return y;
        int height = Math.max(18, (int) (22 * workspaceZoom));
        int nodeWidth = Math.max(90, width - depth * 8);
        boolean selected = node.id().equals(selectedProgramNode) || node.id().equals(programCurrentNode);
        boolean problem = programProblems.containsKey(node.id());
        drawProgramBlock(graphics, definition, x, y, nodeWidth, height, selected, problem);
        String label = blockLabel(node, definition);
        graphics.drawString(font, font.plainSubstrByWidth(label, nodeWidth - 14), x + 7, y + Math.max(5, height / 2 - 4),
                0xFFFFFFFF, false);
        programHitboxes.put(node.id(), new ProgramRect(x, y, nodeWidth, height));
        y += height;
        if (!definition.stacks().isEmpty()) {
            for (String stack : definition.stacks()) {
                List<DepotProgram.Node> children = node.stacks().getOrDefault(stack, List.of());
                graphics.fill(x, y, x + 9, y + Math.max(18, children.size() * height), definition.color());
                graphics.drawString(font, stack, x + 12, y + 4, 0xFFB8C9C7, false);
                y += 14;
                for (DepotProgram.Node child : children) y = renderProgramNode(graphics, child, x + 11, y, nodeWidth - 11, depth + 1);
                if (children.isEmpty()) y += height;
            }
            graphics.fill(x, y, x + nodeWidth, y + 7, definition.color());
            y += 7;
        }
        return y;
    }

    private void drawProgramBlock(GuiGraphics graphics, DepotProgramBlocks.Definition definition,
            int x, int y, int width, int height, boolean selected, boolean problem) {
        int border = problem ? 0xFFFF5252 : selected ? 0xFFFFFFFF : 0xFF071010;
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, border);
        graphics.fill(x, y, x + width, y + height, definition.color());
        if (definition.shape() == DepotProgramBlocks.Shape.COMMAND || definition.shape() == DepotProgramBlocks.Shape.CONTROL) {
            graphics.fill(x + 12, y - 1, x + 28, y + 3, definition.color());
            if (definition.shape() != DepotProgramBlocks.Shape.CAP)
                graphics.fill(x + 12, y + height - 2, x + 28, y + height + 2, 0xFF080D0D);
        } else if (definition.shape() == DepotProgramBlocks.Shape.REPORTER) {
            graphics.fill(x - 3, y + 4, x, y + height - 4, definition.color());
            graphics.fill(x + width, y + 4, x + width + 3, y + height - 4, definition.color());
        } else if (definition.shape() == DepotProgramBlocks.Shape.BOOLEAN) {
            graphics.fill(x - 4, y + height / 2 - 2, x, y + height / 2 + 2, definition.color());
            graphics.fill(x + width, y + height / 2 - 2, x + width + 4, y + height / 2 + 2, definition.color());
        }
    }

    private static String blockLabel(DepotProgram.Node node, DepotProgramBlocks.Definition definition) {
        String label = definition.label();
        for (DepotProgramBlocks.Input input : definition.inputs()) {
            label = label.replace("[" + input.name() + "]", literalText(node.inputs().getOrDefault(input.name(), defaultLiteral(input.type()))));
        }
        if ((node.opcode().equals("set_variable") || node.opcode().equals("change_variable")))
            label += " " + node.fields().getOrDefault("variable", "variable");
        return label;
    }

    private DepotProgram.Node nodeAt(double mouseX, double mouseY) {
        return programHitboxes.entrySet().stream().filter(entry -> entry.getValue().contains(mouseX, mouseY))
                .min(java.util.Comparator.comparingInt(entry -> entry.getValue().width())).map(entry -> findNode(programBody, entry.getKey())).orElse(null);
    }

    private void dropNode(DepotProgram.Node node, double mouseX, double mouseY) {
        DepotProgramBlocks.Definition definition = DepotProgramBlocks.get(node.opcode());
        if (definition == null) return;
        if (definition.output() != null) {
            DepotProgram.Node target = findNode(programBody, selectedProgramNode);
            if (target != null && attachValue(target, node, definition.output())) {
                message = "Value snapped into " + target.opcode() + "."; messageSuccess = true;
            } else {
                message = "Select a compatible command input before dropping a value block."; messageSuccess = false;
            }
            return;
        }
        DepotProgram.Node target = nodeAt(mouseX, mouseY);
        if (target != null) {
            DepotProgramBlocks.Definition targetDefinition = DepotProgramBlocks.get(target.opcode());
            if (targetDefinition != null && !targetDefinition.stacks().isEmpty()
                    && mouseX > programHitboxes.get(target.id()).x() + 10) {
                String stack = targetDefinition.stacks().getFirst();
                Map<String, List<DepotProgram.Node>> stacks = new LinkedHashMap<>(target.stacks());
                List<DepotProgram.Node> nested = new ArrayList<>(stacks.getOrDefault(stack, List.of()));
                nested.add(node); stacks.put(stack, nested);
                replaceNode(programBody, target.id(), new DepotProgram.Node(target.id(), target.opcode(), target.fields(), target.inputs(), stacks));
                selectedProgramNode = node.id();
                return;
            }
        }
        int insertion = programBody.size();
        for (int i = 0; i < programBody.size(); i++) {
            ProgramRect rect = programHitboxes.get(programBody.get(i).id());
            if (rect != null && mouseY < rect.y() + rect.height() / 2.0) { insertion = i; break; }
        }
        programBody.add(insertion, node);
        selectedProgramNode = node.id();
        programArguments.setValue(arguments(node));
    }

    private boolean attachValue(DepotProgram.Node target, DepotProgram.Node value, DepotProgram.ValueType type) {
        DepotProgramBlocks.Definition definition = DepotProgramBlocks.get(target.opcode());
        if (definition == null) return false;
        for (DepotProgramBlocks.Input input : definition.inputs()) {
            if (input.type() != type) continue;
            Map<String, DepotProgram.Node> inputs = new LinkedHashMap<>(target.inputs());
            inputs.put(input.name(), value);
            replaceNode(programBody, target.id(), new DepotProgram.Node(target.id(), target.opcode(), target.fields(), inputs, target.stacks()));
            return true;
        }
        return false;
    }

    private static DepotProgram.Node findNode(List<DepotProgram.Node> nodes, UUID id) {
        if (id == null) return null;
        for (DepotProgram.Node node : nodes) {
            if (node.id().equals(id)) return node;
            for (List<DepotProgram.Node> stack : node.stacks().values()) {
                DepotProgram.Node found = findNode(stack, id);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean replaceNode(List<DepotProgram.Node> nodes, UUID id, DepotProgram.Node replacement) {
        for (int i = 0; i < nodes.size(); i++) {
            DepotProgram.Node node = nodes.get(i);
            if (node.id().equals(id)) { nodes.set(i, replacement); return true; }
            Map<String, List<DepotProgram.Node>> stacks = new LinkedHashMap<>(node.stacks());
            for (Map.Entry<String, List<DepotProgram.Node>> entry : node.stacks().entrySet()) {
                List<DepotProgram.Node> nested = new ArrayList<>(entry.getValue());
                if (replaceNode(nested, id, replacement)) {
                    stacks.put(entry.getKey(), nested);
                    nodes.set(i, new DepotProgram.Node(node.id(), node.opcode(), node.fields(), node.inputs(), stacks));
                    return true;
                }
            }
        }
        return false;
    }

    private static DepotProgram.Node removeNode(List<DepotProgram.Node> nodes, UUID id) {
        for (int i = 0; i < nodes.size(); i++) {
            DepotProgram.Node node = nodes.get(i);
            if (node.id().equals(id)) { nodes.remove(i); return node; }
            Map<String, List<DepotProgram.Node>> stacks = new LinkedHashMap<>(node.stacks());
            for (Map.Entry<String, List<DepotProgram.Node>> entry : node.stacks().entrySet()) {
                List<DepotProgram.Node> nested = new ArrayList<>(entry.getValue());
                DepotProgram.Node removed = removeNode(nested, id);
                if (removed != null) {
                    stacks.put(entry.getKey(), nested);
                    nodes.set(i, new DepotProgram.Node(node.id(), node.opcode(), node.fields(), node.inputs(), stacks));
                    return removed;
                }
            }
        }
        return null;
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

}
