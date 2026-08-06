package net.crystalnexus.client.gui;

import net.crystalnexus.block.DepotCliBlock;
import net.crystalnexus.cli.DepotCliParser;
import net.crystalnexus.jei.CrystalnexusJeiRuntimePlugin;
import net.crystalnexus.network.payload.C2S_DepotCliRequest;
import net.crystalnexus.network.payload.S2C_DepotCliResponse;
import net.crystalnexus.world.inventory.DepotCliMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DepotCliScreen extends AbstractContainerScreen<DepotCliMenu> {
    private static final int MAX_OUTPUT = 200;
    private static final int MAX_HISTORY = 50;
    private final List<String> output = new ArrayList<>();
    private final List<String> history = new ArrayList<>();
    private List<String> suggestions = List.of();
    private EditBox input;
    private int historyIndex;
    private int suggestionIndex;
    private int scrollOffset;
    private boolean connected;
    private boolean applyingSuggestion;

    public DepotCliScreen(DepotCliMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 400;
        imageHeight = 250;
    }

    @Override
    protected void init() {
        imageWidth = Math.min(420, width - 24);
        imageHeight = Math.min(270, height - 24);
        super.init();
        input = new EditBox(font, leftPos + 21, topPos + imageHeight - 24, imageWidth - 33, 16, Component.literal("Depot command"));
        input.setMaxLength(256);
        input.setBordered(false);
        input.setTextColor(0xFFE8E8E8);
        input.setResponder(value -> {
            if (!applyingSuggestion) suggestions = List.of();
        });
        addRenderableWidget(input);
        setInitialFocus(input);
        PacketDistributor.sendToServer(new C2S_DepotCliRequest(menu.containerId, "", false));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (minecraft != null && minecraft.level != null) {
            var state = minecraft.level.getBlockState(menu.getBlockPos());
            connected = state.hasProperty(DepotCliBlock.CONNECTED) && state.getValue(DepotCliBlock.CONNECTED);
        }
        // Poll for pending JEI transfer commands
        String pending = net.crystalnexus.client.DepotCliJeiTransferHandler.pendingCommand.getAndSet(null);
        if (pending != null) {
            append(List.of("[OK] Recipe transferred from JEI."));
            input.setValue(pending);
            input.moveCursorToEnd(false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scrollOffset = Math.max(0, scrollOffset - visibleLines());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            scrollOffset = Math.min(maxScroll(), scrollOffset + visibleLines());
            return true;
        }
        if (input != null && input.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                submit();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                moveHistory(-1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                moveHistory(1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                autocomplete();
                return true;
            }
            input.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scrollOffset = Mth.clamp(scrollOffset + (deltaY < 0 ? 1 : -1), 0, maxScroll());
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF080D0D);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 2, 0xFF3FCFC5);
        graphics.fill(leftPos, topPos + imageHeight - 31, leftPos + imageWidth, topPos + imageHeight - 30, 0xFF245E5A);
        graphics.fill(leftPos + 10, topPos + imageHeight - 26, leftPos + imageWidth - 10, topPos + imageHeight - 6, 0xFF111B1B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "CRYSTAL NEXUS // DEPOT CLI", 10, 9, 0xFF55FFF2, false);
        String status = connected ? "CONNECTED" : "OFFLINE";
        graphics.drawString(font, status, imageWidth - 10 - font.width(status), 9, connected ? 0xFF63FF86 : 0xFFFF6565, false);

        List<RenderLine> lines = wrappedOutput();
        int start = Math.min(scrollOffset, Math.max(0, lines.size() - visibleLines()));
        int end = Math.min(lines.size(), start + visibleLines());
        int y = 25;
        for (int i = start; i < end; i++) {
            RenderLine line = lines.get(i);
            graphics.drawString(font, line.text(), 11, y, line.color(), false);
            y += font.lineHeight + 1;
        }
        graphics.drawString(font, ">", 11, imageHeight - 22, 0xFF55FFF2, false);
    }

    public void handleResponse(S2C_DepotCliResponse response) {
        if (response.menuId() != menu.containerId) return;
        connected = response.connected();
        if (!response.lines().isEmpty()) append(response.lines());
        if (!response.suggestions().isEmpty()) {
            suggestions = response.suggestions();
            suggestionIndex = 0;
            applySuggestion(suggestions.getFirst());
        }
    }

    private void submit() {
        String command = input.getValue().trim();
        if (command.isEmpty()) return;
        if (history.isEmpty() || !history.getLast().equals(command)) {
            history.add(command);
            if (history.size() > MAX_HISTORY) history.removeFirst();
        }
        historyIndex = history.size();
        input.setValue("");
        suggestions = List.of();
        if (command.equalsIgnoreCase("clear") || command.equalsIgnoreCase("cls")) {
            output.clear();
            scrollOffset = 0;
            return;
        }
        append(List.of("> " + command));
        if (command.equalsIgnoreCase("history")) {
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < history.size(); i++) lines.add((i + 1) + "  " + history.get(i));
            append(lines);
            return;
        }
        // Handle jei command locally to open JEI recipe GUI
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
            append(List.of("[INFO] Opens JEI recipes for the given item."));
            append(List.of("[INFO] Use --machine <block_id> to auto-fill the craft command with a machine preference."));
            return;
        }
        // Parse --machine flag
        int machineIndex = tokens.indexOf("--machine");
        String machineArg = null;
        if (machineIndex >= 0 && machineIndex + 1 < tokens.size()) {
            machineArg = tokens.get(machineIndex + 1);
            List<String> filtered = new ArrayList<>(tokens);
            filtered.remove(machineIndex + 1);
            filtered.remove(machineIndex);
            tokens = filtered;
        }
        // Parse item and optional amount
        java.util.OptionalInt amount = DepotCliParser.positiveQuantity(tokens.getLast(), 999999);
        int itemEnd = amount.isPresent() ? tokens.size() - 1 : tokens.size();
        if (itemEnd < 2) {
            append(List.of("[INFO] Please specify an item name."));
            return;
        }
        String itemName = String.join(" ", tokens.subList(1, itemEnd));
        int craftAmount = amount.orElse(1);
        // Open JEI GUI focused on the item
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.tryParse(itemName));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            // Try to resolve by name - use the registry lookup
            append(List.of("[INFO] Opening JEI for " + itemName + "..."));
        }
        net.crystalnexus.jei.CrystalnexusJeiRuntimePlugin.showRecipesFor(
                new net.minecraft.world.item.ItemStack(item != null ? item : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        net.minecraft.resources.ResourceLocation.parse(itemName))));
        // Suggest the craft command
        StringBuilder craftCmd = new StringBuilder("craft ");
        if (machineArg != null) craftCmd.append("--machine ").append(machineArg).append(" ");
        craftCmd.append(itemName).append(" ").append(craftAmount);
        append(List.of("[OK] Showing JEI recipes for " + itemName + "."));
        append(List.of("[INFO] Use: " + craftCmd));
        // Pre-fill the input with the craft command
        applySuggestion(craftCmd.toString());
    }

    private void autocomplete() {
        if (!suggestions.isEmpty()) {
            suggestionIndex = (suggestionIndex + 1) % suggestions.size();
            applySuggestion(suggestions.get(suggestionIndex));
            return;
        }
        PacketDistributor.sendToServer(new C2S_DepotCliRequest(menu.containerId, input.getValue(), true));
    }

    private void applySuggestion(String suggestion) {
        applyingSuggestion = true;
        input.setValue(suggestion);
        input.moveCursorToEnd(false);
        applyingSuggestion = false;
    }

    private void moveHistory(int direction) {
        if (history.isEmpty()) return;
        historyIndex = Mth.clamp(historyIndex + direction, 0, history.size());
        input.setValue(historyIndex == history.size() ? "" : history.get(historyIndex));
        input.moveCursorToEnd(false);
    }

    private void append(List<String> lines) {
        output.addAll(lines);
        while (output.size() > MAX_OUTPUT) output.removeFirst();
        scrollOffset = maxScroll();
    }

    private List<RenderLine> wrappedOutput() {
        List<RenderLine> wrapped = new ArrayList<>();
        for (String line : output) {
            int color = color(line);
            if (line.isEmpty()) wrapped.add(new RenderLine(FormattedCharSequence.EMPTY, color));
            else font.split(Component.literal(line), imageWidth - 22).forEach(text -> wrapped.add(new RenderLine(text, color)));
        }
        return wrapped;
    }

    private record RenderLine(FormattedCharSequence text, int color) {}

    private int visibleLines() {
        return Math.max(1, (imageHeight - 62) / (font.lineHeight + 1));
    }

    private int maxScroll() {
        return Math.max(0, wrappedOutput().size() - visibleLines());
    }

    private static int color(String line) {
        if (line.startsWith("[OK]")) return 0xFF69FF91;
        if (line.startsWith("[WARN]")) return 0xFFFFD166;
        if (line.startsWith("[ERROR]")) return 0xFFFF6B6B;
        if (line.startsWith(">")) return 0xFF55FFF2;
        return 0xFFD7E3E2;
    }
}
