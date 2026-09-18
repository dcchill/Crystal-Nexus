package net.crystalnexus.client.gui;

import net.crystalnexus.assembly.AssemblyGraph;
import net.crystalnexus.assembly.AssemblyLineMachine;
import net.crystalnexus.network.AssemblyLineGraphEdit;
import net.crystalnexus.network.AssemblyLineItemConfigure;
import net.crystalnexus.network.AssemblyLineRequest;
import net.crystalnexus.world.inventory.AssemblyLineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Full-screen, server-owned assembly program editor. JEI drops configure recipes; they never create items. */
public final class AssemblyLineScreen extends AbstractContainerScreen<AssemblyLineMenu> {
    private static final int NODE_W = 190, HEADER_H = 25, ROW_H = 18, OUTPUT_ROW_H = 30, SOCKET_TOP_GAP = 28;
    /** Keep a permanent right-side strip for JEI's ingredient list and search field. */
    private static final int JEI_STRIP = 190;
    private int canvasLeft, canvasTop, canvasRight, canvasBottom;
    private double panX = 24, panY = 24;
    private double zoom = 1.0;
    private int selectedNode = -1, connectingNode = -1, connectingSocket = -1;
    private int openInputNode = -1, openInputSocket = -1;
    private int draggingNode = -1;
    private double dragOffsetX, dragOffsetY, lastMouseX, lastMouseY;
    private boolean panning;
    private static final Map<AssemblyLineMenu, Integer> TRANSFER_SELECTIONS = new WeakHashMap<>();

    public AssemblyLineScreen(AssemblyLineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 640;
        imageHeight = 360;
    }

    public int editorWidth() { return imageWidth; }
    public int editorHeight() { return imageHeight; }
    public int selectedNode() { return selectedNode; }
    private void rememberSelection(int nodeId) {
        selectedNode = nodeId;
        synchronized (TRANSFER_SELECTIONS) { TRANSFER_SELECTIONS.put(menu, nodeId); }
    }
    public static int rememberedSelection(AssemblyLineMenu menu) {
        synchronized (TRANSFER_SELECTIONS) {
            return TRANSFER_SELECTIONS.getOrDefault(menu, -1);
        }
    }
    /** JEI can invoke its transfer button without a graph click having selected a node. */
    public int transferTargetNode() {
        int remembered = rememberedSelection(menu);
        if (menu.controller.graph().node(remembered) != null) return remembered;
        if (menu.controller.graph().node(selectedNode) != null) return selectedNode;
        return menu.controller.graph().nodes.stream()
            .filter(node -> node.machine != 0
                && !node.block.endsWith(":multiblock_item_input")
                && !node.block.endsWith(":multiblock_item_output")
                && !node.block.endsWith(":machine_fluid_input")
                && !node.block.endsWith(":multiblock_fluid_output"))
            .map(node -> node.id)
            .findFirst().orElse(-1);
    }
    private void addSlot(boolean output) {
        AssemblyGraph.Node node = menu.controller.graph().node(selectedNode);
        if (node == null) return;
        List<String> inputs = new ArrayList<>(List.of(node.inputItems));
        List<String> outputs = new ArrayList<>(List.of(node.outputItems));
        if (output) {
            if (outputs.size() >= 32) return;
            outputs.add("");
        } else {
            if (inputs.size() >= 32) return;
            inputs.add("");
        }
        PacketDistributor.sendToServer(new net.crystalnexus.network.AssemblyLineSlotsSet(
            menu.containerId, node.id, inputs, outputs, List.of(), List.of()));
    }

    @Override protected void init() {
        imageWidth = Math.max(320, width - JEI_STRIP);
        imageHeight = Math.max(300, height - 28);
        super.init();
        // AbstractContainerScreen centers containers. This editor is left-aligned so
        // JEI has a contiguous, unobstructed area on the right instead of two slivers.
        leftPos = 8;
        topPos = Math.max(8, (height - imageHeight) / 2);
        canvasLeft = leftPos + 8;
        canvasTop = topPos + 42;
        canvasRight = leftPos + imageWidth - 8;
        canvasBottom = topPos + imageHeight - 8;
        addRenderableWidget(Button.builder(Component.literal(menu.controller.graph().enabled ? "Pause" : "Run"), button -> {
            PacketDistributor.sendToServer(new AssemblyLineGraphEdit(menu.containerId, "toggle", 0, 0, 0, 0, 0, 0));
        }).bounds(leftPos + 10, topPos + 14, 58, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Rescan"), button ->
            PacketDistributor.sendToServer(new AssemblyLineRequest(menu.containerId, "", 0, true, false)))
            .bounds(leftPos + 72, topPos + 14, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("In +"), button -> addSlot(false))
            .bounds(leftPos + 140, topPos + 14, 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Out +"), button -> addSlot(true))
            .bounds(leftPos + 192, topPos + 14, 52, 20).build());
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff11151c);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 40, 0xff252c37);
        graphics.fill(canvasLeft, canvasTop, canvasRight, canvasBottom, 0xff171b22);
        drawGrid(graphics);
    }

    private void drawGrid(GuiGraphics graphics) {
        int spacing = 24;
        int ox = Math.floorMod((int) panX, spacing), oy = Math.floorMod((int) panY, spacing);
        for (int x = canvasLeft + ox; x < canvasRight; x += spacing) graphics.vLine(x, canvasTop, canvasBottom, 0xff202630);
        for (int y = canvasTop + oy; y < canvasBottom; y += spacing) graphics.hLine(canvasLeft, canvasRight, y, 0xff202630);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, shortName(title.getString(), 24), 282, 19, 0xfff2f5fa, false);
        String state = menu.controller.formed() ? menu.controller.graph().enabled ? "RUNNING" : "PAUSED" : "INVALID STRUCTURE";
        int color = menu.controller.graph().enabled ? 0xff69dc8c : menu.controller.formed() ? 0xffffc857 : 0xffff6b6b;
        graphics.drawString(font, state, imageWidth - 105, 19, color, false);
        graphics.enableScissor(canvasLeft, canvasTop, canvasRight, canvasBottom);
        renderEdges(graphics);
        for (AssemblyGraph.Node node : menu.controller.graph().nodes) renderNode(graphics, node, mouseX, mouseY);
        renderInputDropdown(graphics);
        if (connectingNode >= 0) {
            AssemblyGraph.Node from = menu.controller.graph().node(connectingNode);
            if (from != null) drawCable(graphics, outputX(from), socketY(from, connectingSocket, false), mouseX - leftPos, mouseY - topPos, 0xffffc857);
        }
        graphics.disableScissor();
        if (menu.controller.graph().nodes.isEmpty()) graphics.drawCenteredString(font,
            "No compatible machines detected inside the multiblock. Press Rescan after completing the enclosure.", imageWidth / 2, 82, 0xffaeb8c8);
        graphics.drawString(font, "Drag headers • connect sockets • use JEI + to fill slots • drop JEI items on individual slots", 12, imageHeight - 15, 0xff7f8a9b, false);
    }

    private void renderEdges(GuiGraphics graphics) {
        for (AssemblyGraph.Edge edge : menu.controller.graph().edges) {
            AssemblyGraph.Node from = menu.controller.graph().node(edge.from()), to = menu.controller.graph().node(edge.to());
            if (from == null || to == null) continue;
            drawCable(graphics, outputX(from), socketY(from, edge.output(), false), inputX(to), socketY(to, edge.input(), true), 0xff4d94ff);
        }
    }

    private void drawCable(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int segments = Math.max(8, Math.min(30, Math.abs(x2 - x1) / 8));
        double control = Math.max(34, Math.abs(x2 - x1) * .45);
        int px = x1, py = y1;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments, u = 1 - t;
            int x = (int) Math.round(u*u*u*x1 + 3*u*u*t*(x1+control) + 3*u*t*t*(x2-control) + t*t*t*x2);
            int y = (int) Math.round(u*u*u*y1 + 3*u*u*t*y1 + 3*u*t*t*y2 + t*t*t*y2);
            drawSegment(graphics, px, py, x, y, color); px = x; py = y;
        }
    }

    private void drawSegment(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2-x1), Math.abs(y2-y1));
        for (int i=0;i<=steps;i++) { int x=x1+(x2-x1)*i/Math.max(1,steps), y=y1+(y2-y1)*i/Math.max(1,steps); graphics.fill(x-1,y-1,x+2,y+2,color); }
    }

    private void renderNode(GuiGraphics graphics, AssemblyGraph.Node node, int mouseX, int mouseY) {
        int screenX = nodeX(node), screenY = nodeY(node), h = nodeHeight(node);
        graphics.pose().pushPose();
        graphics.pose().translate(screenX, screenY, 0);
        graphics.pose().scale((float) zoom, (float) zoom, 1);
        int x = 0, y = 0;
        graphics.fill(x, y, x + NODE_W, y + h, 0xff303743);
        graphics.fill(x, y, x + NODE_W, y + HEADER_H, node.id == selectedNode ? 0xff526784 : 0xff414b5b);
        ResourceLocation blockId = ResourceLocation.tryParse(node.block);
        ItemStack machine = blockId == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.BLOCK.get(blockId));
        if (!machine.isEmpty()) graphics.renderItem(machine, x + 5, y + 5);
        String name = machine.isEmpty() ? shortName(node.block, 22) : machine.getHoverName().getString();
        graphics.drawString(font, shortName(name, 24), x + 25, y + 8, 0xffffffff, false);
        AssemblyLineMachine worker = node.machine == 0 ? null : menu.controller.machineAt(BlockPos.of(node.machine));
        if (worker != null && worker.energy() != null)
            graphics.drawString(font, worker.energy().getEnergyStored() + " / " + worker.energy().getMaxEnergyStored() + " FE", x + 8, y + h - 23, 0xff70cfff, false);
        graphics.drawString(font, shortName(node.status, 20), x + 8, y + h - 13, statusColor(node.status), false);
        for (int socket = 0; socket < node.inputSockets; socket++) {
            int sy = logicalSocketY(node, socket, true);
            graphics.fill(x - 4, sy - 4, x + 5, sy + 5, 0xffffc857);
            ItemStack stack = inputStack(node, socket);
            if (!stack.isEmpty()) graphics.renderItem(stack, x + 8, sy - 8);
            String fluid = node.inputFluids != null && socket < node.inputFluids.length ? node.inputFluids[socket] : "";
            boolean fluidPort = node.block.endsWith(":machine_fluid_input");
            String label = stack.isEmpty() && (fluid == null || fluid.isEmpty()) ? (fluidPort ? "Fluid In " : "Input ") + (socket + 1)
                : stack.isEmpty() ? shortName(fluid, 13) : shortName(stack.getHoverName().getString(), 13);
            graphics.drawString(font, label, x + 27, sy - 4, 0xffd7dce5, false);
            drawRemoveButton(graphics, x + NODE_W - 21, sy - 8);
            if (inputOptions(node).size() > 1) {
                graphics.fill(x + NODE_W - 23, sy - 8, x + NODE_W - 7, sy + 8, 0xff242a34);
                graphics.drawString(font, "v", x + NODE_W - 18, sy - 4, 0xffd7dce5, false);
            }
        }
        for (int socket = 0; socket < node.outputSockets; socket++) {
            int sy = logicalSocketY(node, socket, false);
            graphics.fill(x + NODE_W - 4, sy - 4, x + NODE_W + 5, sy + 5, 0xff4d94ff);
            ItemStack stack = outputStack(node, socket);
            String fluid = node.outputFluids != null && socket < node.outputFluids.length ? node.outputFluids[socket] : "";
            boolean fluidPort = node.block.endsWith(":multiblock_fluid_output");
            String label = stack.isEmpty() && (fluid == null || fluid.isEmpty()) ? (fluidPort ? "Fluid Out " : "Output ") + (socket + 1)
                : stack.isEmpty() ? shortName(fluid, 12) : shortName(stack.getHoverName().getString(), 12);
            int labelX = x + NODE_W - 72;
            graphics.drawString(font, label, labelX, sy - 4, 0xffd7dce5, false);
            drawRemoveButton(graphics, x + NODE_W - 21, sy - 8);
            if (!stack.isEmpty()) graphics.renderItem(stack, labelX - 19, sy - 8);
            boolean export = node.outputExport != null && socket < node.outputExport.length && node.outputExport[socket];
            int ex = x + NODE_W - 41, ey = sy + 6;
            graphics.fill(ex, ey, x + NODE_W - 3, ey + 10, export ? 0xffa57b12 : 0xff242a34);
            graphics.drawString(font, export ? "OUTPUT" : "HOLD", ex + 3, ey + 1, export ? 0xffffdc73 : 0xff8791a1, false);
        }
        if (node.recipe.isEmpty()) graphics.drawCenteredString(font, "Drop a JEI item or choose recipe", x + NODE_W/2, y + HEADER_H + 5, 0xff8994a5);
        graphics.pose().popPose();
    }

    private int statusColor(String status) {
        String s = status.toLowerCase();
        return s.contains("missing") || s.contains("error") ? 0xffff6b6b : s.contains("running") ? 0xff69dc8c : 0xffaeb8c8;
    }

    private int nodeX(AssemblyGraph.Node node) { return (int) Math.round(8 + panX + node.x * zoom); }
    private int nodeY(AssemblyGraph.Node node) { return (int) Math.round(42 + panY + node.y * zoom); }
    private int inputX(AssemblyGraph.Node node) { return nodeX(node); }
    private int outputX(AssemblyGraph.Node node) { return nodeX(node) + scaled(NODE_W); }
    private int logicalSocketY(AssemblyGraph.Node node, int socket, boolean input) {
        return HEADER_H + SOCKET_TOP_GAP + socket * (input ? ROW_H : OUTPUT_ROW_H);
    }
    private int socketY(AssemblyGraph.Node node, int socket, boolean input) {
        return nodeY(node) + scaled(logicalSocketY(node, socket, input));
    }
    private int nodeHeight(AssemblyGraph.Node node) {
        int socketRows = Math.max(node.inputSockets * ROW_H, node.outputSockets * OUTPUT_ROW_H);
        return HEADER_H + SOCKET_TOP_GAP + Math.max(2 * ROW_H, socketRows) + 22;
    }
    private int scaledNodeHeight(AssemblyGraph.Node node) { return scaled(nodeHeight(node)); }
    private int scaled(int value) { return (int) Math.round(value * zoom); }
    private AssemblyGraph.Node nodeAtMouse(double mouseX, double mouseY) {
        double lx = mouseX - leftPos, ly = mouseY - topPos;
        if (!insideCanvas(mouseX, mouseY)) return null;
        for (AssemblyGraph.Node node : menu.controller.graph().nodes) {
            int nx = nodeX(node), ny = nodeY(node);
            if (lx >= nx && lx <= nx + scaled(NODE_W) && ly >= ny && ly <= ny + scaledNodeHeight(node)) return node;
        }
        return null;
    }
    @Override protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        AssemblyGraph.Node hovered = nodeAtMouse(mouseX, mouseY);
        if (hovered != null && hovered.machine != 0) {
            AssemblyLineMachine worker = menu.controller.machineAt(BlockPos.of(hovered.machine));
            if (worker != null) {
                ResourceLocation blockId = ResourceLocation.tryParse(hovered.block);
                ItemStack machine = blockId == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.BLOCK.get(blockId));
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal("Machine: " + (machine.isEmpty() ? shortName(hovered.block, 30) : machine.getHoverName().getString())));
                int progress = worker.progress();
                if (progress > 0) lines.add(Component.literal("Progress: " + progress + "%"));
                if (worker.energy() != null) lines.add(Component.literal("Energy: " + worker.energy().getEnergyStored() + " / " + worker.energy().getMaxEnergyStored() + " FE"));
                if (!hovered.recipe.isEmpty()) lines.add(Component.literal("Recipe: " + shortName(hovered.recipe, 40)));
                if (!hovered.status.isEmpty() && !hovered.status.equals("Ready")) lines.add(Component.literal("Status: " + hovered.status));
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }
    private void drawRemoveButton(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 14, y + 16, 0xff71383d);
        graphics.drawCenteredString(font, "-", x + 7, y + 3, 0xffffc4c4);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // AbstractContainerScreen consumes otherwise-empty clicks inside its GUI.
        // Handle the graph canvas first; header widgets are outside this rectangle
        // and continue through normal Screen dispatch below.
        if (!insideCanvas(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button);
        double lx = mouseX - leftPos, ly = mouseY - topPos;
        if (openInputNode >= 0 && clickInputDropdown(lx, ly)) return true;
        openInputNode = openInputSocket = -1;
        for (AssemblyGraph.Node node : reversedNodes()) {
            for (int socket=0;socket<node.outputSockets;socket++) {
                int sx=outputX(node), sy=socketY(node,socket,false);
                if (button == 1 && distance(lx,ly,sx,sy)<=9) {
                    PacketDistributor.sendToServer(new AssemblyLineGraphEdit(menu.containerId, "remove_edge", node.id, -1, socket, 0, 0, 0));
                    return true;
                }
                if (distance(lx,ly,sx,sy)<=9) { connectingNode=node.id; connectingSocket=socket; rememberSelection(node.id); return true; }
                if (lx>=sx-41 && lx<=sx-3 && ly>=sy+6 && ly<=sy+16) {
                    boolean enabled=!(node.outputExport!=null && socket<node.outputExport.length && node.outputExport[socket]);
                    PacketDistributor.sendToServer(new AssemblyLineGraphEdit(menu.containerId,"export",node.id,enabled?1:0,socket,0,0,0)); return true;
                }
            }
            int nx=nodeX(node), ny=nodeY(node);
            if (lx>=nx && lx<=nx+scaled(NODE_W) && ly>=ny && ly<=ny+scaledNodeHeight(node)) {
                rememberSelection(node.id);
                for(int socket=0;socket<node.inputSockets;socket++) {
                    int sy=socketY(node,socket,true);
                    if (button == 0 && lx >= nx + scaled(NODE_W - 22) && lx <= nx + scaled(NODE_W - 5) && ly >= sy - scaled(9) && ly <= sy + scaled(9)) {
                        PacketDistributor.sendToServer(new AssemblyLineGraphEdit(menu.containerId, "remove_slot", node.id, 0, socket, 0, 0, 0));
                        return true;
                    }
                    if (button == 1 && distance(lx,ly,inputX(node),sy)<=9) {
                        PacketDistributor.sendToServer(new AssemblyLineGraphEdit(menu.containerId, "remove_edge", node.id, -2, socket, 0, 0, 0));
                        return true;
                    }
                    if(lx>=nx && lx<=nx+scaled(NODE_W) && ly>=sy-scaled(9) && ly<=sy+scaled(9) && inputOptions(node).size()>1) {
                        openInputNode=node.id; openInputSocket=socket; return true;
                    }
                }
                for (int socket = 0; socket < node.outputSockets; socket++) {
                    int sy = socketY(node, socket, false);
                    if (button == 0 && lx >= nx + scaled(NODE_W - 22) && lx <= nx + scaled(NODE_W - 5) && ly >= sy - scaled(9) && ly <= sy + scaled(9)) {
                        PacketDistributor.sendToServer(new AssemblyLineGraphEdit(menu.containerId, "remove_slot", node.id, 1, socket, 0, 0, 0));
                        return true;
                    }
                }
                if (button==0 && ly<=ny+scaled(HEADER_H)) { draggingNode=node.id; dragOffsetX=lx-nx; dragOffsetY=ly-ny; }
                return true;
            }
        }
        selectedNode=-1; panning=true; lastMouseX=mouseX; lastMouseY=mouseY; return true;
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingNode>=0) {
            AssemblyGraph.Node node=menu.controller.graph().node(draggingNode);
            if(node!=null){ node.x=(float)((mouseX-leftPos-8-panX-dragOffsetX)/zoom); node.y=(float)((mouseY-topPos-42-panY-dragOffsetY)/zoom); }
            return true;
        }
        if(panning){ panX+=mouseX-lastMouseX; panY+=mouseY-lastMouseY; lastMouseX=mouseX; lastMouseY=mouseY; return true; }
        return super.mouseDragged(mouseX,mouseY,button,dragX,dragY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double lx=mouseX-leftPos, ly=mouseY-topPos;
        if(connectingNode>=0){
            for(AssemblyGraph.Node node:reversedNodes()) for(int socket=0;socket<node.inputSockets;socket++) if(distance(lx,ly,inputX(node),socketY(node,socket,true))<=10){
                PacketDistributor.sendToServer(new AssemblyLineGraphEdit(menu.containerId,"edge",connectingNode,node.id,connectingSocket,socket,0,0));
                connectingNode=-1; connectingSocket=-1; return true;
            }
            connectingNode=-1; connectingSocket=-1; return true;
        }
        if(draggingNode>=0){ AssemblyGraph.Node node=menu.controller.graph().node(draggingNode); if(node!=null) PacketDistributor.sendToServer(new AssemblyLineGraphEdit(menu.containerId,"move",node.id,0,0,0,node.x,node.y)); draggingNode=-1; return true; }
        if(panning){ panning=false; return true; }
        return super.mouseReleased(mouseX,mouseY,button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!insideCanvas(mouseX, mouseY) || overNode(mouseX - leftPos, mouseY - topPos)) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        double old = zoom;
        zoom = Math.max(0.5, Math.min(2.0, zoom + Math.signum(scrollY) * 0.1));
        double lx = mouseX - leftPos - 8, ly = mouseY - topPos - 42;
        panX = lx - (lx - panX) * zoom / old;
        panY = ly - (ly - panY) * zoom / old;
        return true;
    }

    public List<GhostTarget> ghostTargets(ItemStack stack) {
        return ghostTargets(stack, null);
    }

    public List<GhostTarget> ghostTargets(Object ingredient) {
        return ghostTargets(ItemStack.EMPTY, ingredient);
    }
    
    public List<GhostTarget> ghostFluidTargets(FluidStack fluid) {
        List<GhostTarget> result = new ArrayList<>();
        for (AssemblyGraph.Node node : menu.controller.graph().nodes) {
            int x = leftPos + nodeX(node), y = topPos + nodeY(node);
            if (x + scaled(NODE_W) < canvasLeft || x > canvasRight || y > canvasBottom) continue;
            for (int socket = 0; socket < node.inputSockets; socket++)
                result.add(new GhostTarget(new Rect2i(x - 8, topPos + socketY(node, socket, true) - 9, 86, 18), node.id, socket, false));
            for (int socket = 0; socket < node.outputSockets; socket++)
                result.add(new GhostTarget(new Rect2i(x + scaled(NODE_W - 78), topPos + socketY(node, socket, false) - scaled(9), scaled(86), scaled(18)), node.id, socket, true));
        }
        return result;
    }

    public List<GhostTarget> ghostTargets(ItemStack stack, Object emiIngredient) {
        List<GhostTarget> result=new ArrayList<>();
        for(AssemblyGraph.Node node:menu.controller.graph().nodes) {
            int x=leftPos+nodeX(node), y=topPos+nodeY(node);
            if(x+scaled(NODE_W)<canvasLeft || x>canvasRight || y>canvasBottom) continue;
            ResourceLocation blockId=ResourceLocation.tryParse(node.block);
            for (int socket = 0; socket < node.inputSockets; socket++)
                result.add(new GhostTarget(new Rect2i(x - 8, topPos + socketY(node, socket, true) - 9, 86, 18), node.id, socket, false));
            for (int socket = 0; socket < node.outputSockets; socket++)
                result.add(new GhostTarget(new Rect2i(x + scaled(NODE_W - 78), topPos + socketY(node, socket, false) - scaled(9), scaled(86), scaled(18)), node.id, socket, true));
        }
        return result;
    }

    public void acceptGhostItem(int nodeId, int socket, boolean output, ItemStack stack) {
        AssemblyGraph.Node node=menu.controller.graph().node(nodeId);
        if(node==null || stack.isEmpty() || minecraft.level==null) return;
        ResourceLocation blockId=ResourceLocation.tryParse(node.block);
        if(blockId!=null && blockId.getPath().equals("multiblock_item_input")) {
            PacketDistributor.sendToServer(new AssemblyLineItemConfigure(menu.containerId,nodeId,Math.max(0,socket),
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
            return;
        }
        PacketDistributor.sendToServer(new net.crystalnexus.network.AssemblyLineSlotConfigure(menu.containerId,
            nodeId, socket, output, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
    }

    public void acceptGhostFluid(int nodeId, int socket, net.neoforged.neoforge.fluids.FluidStack fluid) {
        AssemblyGraph.Node node=menu.controller.graph().node(nodeId);
        if(node==null || fluid.isEmpty() || minecraft.level==null) return;
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        boolean output = node.block.endsWith(":machine_fluid_input");
        PacketDistributor.sendToServer(new net.crystalnexus.network.AssemblyLineFluidConfigure(
            menu.containerId, nodeId, socket, output, fluidId.toString()));
    }

    private ItemStack inputStack(AssemblyGraph.Node node,int socket){
        ItemStack configured=configuredInputStack(node,socket);
        if(minecraft.level==null || node.recipe==null || node.recipe.isEmpty()) return configured;
        ResourceLocation id=ResourceLocation.tryParse(node.recipe); if(id==null) return ItemStack.EMPTY;
        var recipe=minecraft.level.getRecipeManager().byKey(id); if(recipe.isEmpty() || socket>=recipe.get().value().getIngredients().size()) return configured;
        ItemStack[] choices=recipe.get().value().getIngredients().get(socket).getItems(); return choices.length==0?ItemStack.EMPTY:choices[0];
    }
    private ItemStack configuredInputStack(AssemblyGraph.Node node,int socket){ if(node.inputItems==null || socket>=node.inputItems.length) return ItemStack.EMPTY; ResourceLocation id=ResourceLocation.tryParse(node.inputItems[socket]); return id==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.get(id)); }
    private ItemStack outputStack(AssemblyGraph.Node node,int socket){ if(node.outputItems==null || socket>=node.outputItems.length || node.outputItems[socket]==null) return ItemStack.EMPTY; ResourceLocation id=ResourceLocation.tryParse(node.outputItems[socket]); return id==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.get(id)); }
    private List<ItemStack> inputOptions(AssemblyGraph.Node node){
        return List.of();
    }
    private void renderInputDropdown(GuiGraphics graphics) {
        AssemblyGraph.Node node=menu.controller.graph().node(openInputNode);
        if(node==null) return;
        List<ItemStack> options=inputOptions(node);
        if(options.size()<2) return;
        int x=nodeX(node)+27, y=socketY(node,openInputSocket,true)+9, w=118, h=18;
        for(int i=0;i<options.size();i++) {
            int yy=y+i*h;
            graphics.fill(x, yy, x+w, yy+h, 0xff242a34);
            graphics.renderItem(options.get(i), x+2, yy+1);
            graphics.drawString(font, shortName(options.get(i).getHoverName().getString(), 15), x+21, yy+5, 0xfff2f5fa, false);
        }
    }
    private boolean clickInputDropdown(double lx,double ly) {
        AssemblyGraph.Node node=menu.controller.graph().node(openInputNode);
        if(node==null) return false;
        List<ItemStack> options=inputOptions(node);
        int x=nodeX(node)+27, y=socketY(node,openInputSocket,true)+9, w=118, h=18;
        if(lx<x || lx>x+w || ly<y || ly>y+options.size()*h) return false;
        int index=(int)((ly-y)/h);
        if(index>=0 && index<options.size()) {
            ItemStack input=options.get(index);
            PacketDistributor.sendToServer(new net.crystalnexus.network.AssemblyLineSlotConfigure(menu.containerId,
                node.id, openInputSocket, false, BuiltInRegistries.ITEM.getKey(input.getItem()).toString()));
        }
        openInputNode = openInputSocket = -1;
        return true;
    }
    private boolean overNode(double lx,double ly){ for(AssemblyGraph.Node node:menu.controller.graph().nodes){int nx=nodeX(node),ny=nodeY(node); if(lx>=nx&&lx<=nx+scaled(NODE_W)&&ly>=ny&&ly<=ny+scaledNodeHeight(node)) return true;} return false; }
    private List<AssemblyGraph.Node> reversedNodes(){ List<AssemblyGraph.Node> result=new ArrayList<>(menu.controller.graph().nodes); return result.reversed(); }
    private boolean insideCanvas(double x,double y){return x>=canvasLeft&&x<canvasRight&&y>=canvasTop&&y<canvasBottom;}
    private static double distance(double x1,double y1,double x2,double y2){return Math.hypot(x2-x1,y2-y1);}
    private static String shortName(String value,int max){return value.length()<=max?value:value.substring(0,Math.max(1,max-1))+"…";}
    public record GhostTarget(Rect2i area,int nodeId,int socket,boolean output){}

    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){super.render(graphics,mouseX,mouseY,partialTick);renderTooltip(graphics,mouseX,mouseY);}
    @Override protected void renderSlot(GuiGraphics graphics, net.minecraft.world.inventory.Slot slot) { }
}
