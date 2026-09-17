package net.crystalnexus.client.gui;

import net.crystalnexus.assembly.AssemblyGraph;
import net.crystalnexus.assembly.AssemblyLineMachine;
import net.crystalnexus.network.AssemblyLineGraphEdit;
import net.crystalnexus.network.AssemblyLineItemConfigure;
import net.crystalnexus.network.AssemblyLineJeiRecipeSelect;
import net.crystalnexus.network.AssemblyLineRecipeSelect;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Full-screen, server-owned assembly program editor. JEI drops configure recipes; they never create items. */
public final class AssemblyLineScreen extends AbstractContainerScreen<AssemblyLineMenu> {
    private static final int NODE_W = 190, HEADER_H = 25, ROW_H = 18;
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
    private Button recipeButton;

    public AssemblyLineScreen(AssemblyLineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 640;
        imageHeight = 360;
    }

    public int editorWidth() { return imageWidth; }
    public int editorHeight() { return imageHeight; }

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
        recipeButton = addRenderableWidget(Button.builder(Component.literal("Next recipe"), button -> selectNextRecipe())
            .bounds(leftPos + 140, topPos + 14, 92, 20).build());
        recipeButton.visible = false;
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
        graphics.drawString(font, title, 244, 19, 0xfff2f5fa, false);
        graphics.drawString(font, "Controller: " + menu.controller.energy.getEnergyStored() + " FE", 340, 19, 0xff70cfff, false);
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
        graphics.drawString(font, "Drag headers • drag output sockets to inputs • click OUTPUT to route byproducts • drop JEI items on sockets", 12, imageHeight - 15, 0xff7f8a9b, false);
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
        int x = nodeX(node), y = nodeY(node), h = nodeHeight(node);
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
            int sy = socketY(node, socket, true);
            graphics.fill(x - 4, sy - 4, x + 5, sy + 5, 0xffffc857);
            ItemStack stack = inputStack(node, socket);
            if (!stack.isEmpty()) graphics.renderItem(stack, x + 8, sy - 8);
            graphics.drawString(font, stack.isEmpty() ? "Input " + (socket + 1) : shortName(stack.getHoverName().getString(), 13), x + 27, sy - 4, 0xffd7dce5, false);
            if (inputOptions(node).size() > 1) {
                graphics.fill(x + NODE_W - 23, sy - 8, x + NODE_W - 7, sy + 8, 0xff242a34);
                graphics.drawString(font, "v", x + NODE_W - 18, sy - 4, 0xffd7dce5, false);
            }
        }
        for (int socket = 0; socket < node.outputSockets; socket++) {
            int sy = socketY(node, socket, false);
            graphics.fill(x + NODE_W - 4, sy - 4, x + NODE_W + 5, sy + 5, 0xff4d94ff);
            ItemStack stack = outputStack(node, socket);
            String label = stack.isEmpty() ? "Output " + (socket + 1) : shortName(stack.getHoverName().getString(), 12);
            int labelX = x + NODE_W - 72;
            graphics.drawString(font, label, labelX, sy - 4, 0xffd7dce5, false);
            if (!stack.isEmpty()) graphics.renderItem(stack, labelX - 19, sy - 8);
            boolean export = node.outputExport != null && socket < node.outputExport.length && node.outputExport[socket];
            int ex = x + NODE_W - 41, ey = sy + 6;
            graphics.fill(ex, ey, x + NODE_W - 3, ey + 10, export ? 0xffa57b12 : 0xff242a34);
            graphics.drawString(font, export ? "OUTPUT" : "HOLD", ex + 3, ey + 1, export ? 0xffffdc73 : 0xff8791a1, false);
        }
        if (node.recipe.isEmpty()) graphics.drawCenteredString(font, "Drop a JEI item or choose recipe", x + NODE_W/2, y + HEADER_H + 5, 0xff8994a5);
    }

    private int statusColor(String status) {
        String s = status.toLowerCase();
        return s.contains("missing") || s.contains("error") ? 0xffff6b6b : s.contains("running") ? 0xff69dc8c : 0xffaeb8c8;
    }

    private int nodeX(AssemblyGraph.Node node) { return (int) Math.round(8 + panX + node.x * zoom); }
    private int nodeY(AssemblyGraph.Node node) { return (int) Math.round(42 + panY + node.y * zoom); }
    private int inputX(AssemblyGraph.Node node) { return nodeX(node); }
    private int outputX(AssemblyGraph.Node node) { return nodeX(node) + NODE_W; }
    private int socketY(AssemblyGraph.Node node, int socket, boolean input) { return nodeY(node) + HEADER_H + 14 + socket * ROW_H; }
    private int nodeHeight(AssemblyGraph.Node node) { return HEADER_H + Math.max(2, Math.max(node.inputSockets, node.outputSockets)) * ROW_H + 22; }

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
                if (distance(lx,ly,sx,sy)<=9) { connectingNode=node.id; connectingSocket=socket; selectedNode=node.id; return true; }
                if (lx>=sx-41 && lx<=sx-3 && ly>=sy+6 && ly<=sy+16) {
                    boolean enabled=!(node.outputExport!=null && socket<node.outputExport.length && node.outputExport[socket]);
                    PacketDistributor.sendToServer(new AssemblyLineGraphEdit(menu.containerId,"export",node.id,enabled?1:0,socket,0,0,0)); return true;
                }
            }
            int nx=nodeX(node), ny=nodeY(node);
            if (lx>=nx && lx<=nx+NODE_W && ly>=ny && ly<=ny+nodeHeight(node)) {
                selectedNode=node.id; recipeButton.visible=true;
                for(int socket=0;socket<node.inputSockets;socket++) {
                    int sy=socketY(node,socket,true);
                    if(lx>=nx && lx<=nx+NODE_W && ly>=sy-9 && ly<=sy+9 && inputOptions(node).size()>1) {
                        openInputNode=node.id; openInputSocket=socket; return true;
                    }
                }
                if (button==0 && ly<=ny+HEADER_H) { draggingNode=node.id; dragOffsetX=lx-nx; dragOffsetY=ly-ny; }
                return true;
            }
        }
        selectedNode=-1; recipeButton.visible=false; panning=true; lastMouseX=mouseX; lastMouseY=mouseY; return true;
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

    private void selectNextRecipe() {
        AssemblyGraph.Node node=menu.controller.graph().node(selectedNode);
        if(node==null || minecraft.level==null) return;
        if(node.recipe.startsWith("crystalnexus:jei/")) {
            PacketDistributor.sendToServer(new AssemblyLineJeiRecipeSelect(menu.containerId,node.id,"",""));
            return;
        }
        AssemblyLineMachine machine=menu.controller.machineAt(BlockPos.of(node.machine));
        if(machine==null) return;
        List<RecipeHolder<?>> recipes=minecraft.level.getRecipeManager().getRecipes().stream().filter(machine::supports).toList();
        if(recipes.isEmpty()) return;
        int current=-1; for(int i=0;i<recipes.size();i++) if(recipes.get(i).id().toString().equals(node.recipe)) current=i;
        RecipeHolder<?> next=recipes.get(Math.floorMod(current+1,recipes.size()));
        PacketDistributor.sendToServer(new AssemblyLineRecipeSelect(menu.containerId,node.id,next.id().toString()));
    }

    public List<GhostTarget> ghostTargets(ItemStack stack) {
        return ghostTargets(stack, null);
    }

    public List<GhostTarget> ghostTargets(Object ingredient) {
        return ghostTargets(ItemStack.EMPTY, ingredient);
    }
    
    public List<GhostTarget> ghostFluidTargets(FluidStack fluid) {
        List<GhostTarget> result=new ArrayList<>();
        for(AssemblyGraph.Node node:menu.controller.graph().nodes) {
            int x=leftPos+nodeX(node), y=topPos+nodeY(node);
            if(x+NODE_W<canvasLeft || x>canvasRight || y>canvasBottom) continue;
            ResourceLocation blockId=ResourceLocation.tryParse(node.block);
            // Fluid inputs accept fluids
            if(blockId!=null && blockId.getPath().equals("machine_fluid_input")) {
                result.add(new GhostTarget(new Rect2i(x,y,NODE_W,nodeHeight(node)),node.id,-1));
            } else {
                // Machines accept fluids too for recipe input
                result.add(new GhostTarget(new Rect2i(x,y,NODE_W,nodeHeight(node)),node.id,-1));
            }
        }
        return result;
    }

    public List<GhostTarget> ghostTargets(ItemStack stack, Object emiIngredient) {
        List<GhostTarget> result=new ArrayList<>();
        for(AssemblyGraph.Node node:menu.controller.graph().nodes) {
            int x=leftPos+nodeX(node), y=topPos+nodeY(node);
            if(x+NODE_W<canvasLeft || x>canvasRight || y>canvasBottom) continue;
            ResourceLocation blockId=ResourceLocation.tryParse(node.block);
            if(blockId!=null && blockId.getPath().equals("multiblock_item_input")) {
                for(int socket=0;socket<node.outputSockets;socket++) result.add(new GhostTarget(
                    new Rect2i(x+NODE_W-82,topPos+socketY(node,socket,false)-9,86,18),node.id,socket));
            } else if(blockId!=null && blockId.getPath().equals("machine_fluid_input")) {
                // Fluid input ports accept both items (for fluid container items) and direct fluids
                result.add(new GhostTarget(new Rect2i(x,y,NODE_W,nodeHeight(node)),node.id,-1));
            } else {
                // All other nodes (machines) accept items and fluids as recipe ingredients
                result.add(new GhostTarget(new Rect2i(x,y,NODE_W,nodeHeight(node)),node.id,-1));
            }
        }
        return result;
    }

    public void acceptGhostItem(int nodeId, int socket, ItemStack stack) {
        AssemblyGraph.Node node=menu.controller.graph().node(nodeId);
        if(node==null || stack.isEmpty() || minecraft.level==null) return;
        ResourceLocation blockId=ResourceLocation.tryParse(node.block);
        if(blockId!=null && blockId.getPath().equals("multiblock_item_input")) {
            PacketDistributor.sendToServer(new AssemblyLineItemConfigure(menu.containerId,nodeId,Math.max(0,socket),
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
            return;
        }
        // For fluid inputs or fluid recipes, send the item (container) - the server will handle fluid recipes
        PacketDistributor.sendToServer(new AssemblyLineJeiRecipeSelect(menu.containerId,nodeId,
            BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),""));
    }

    public void acceptGhostFluid(int nodeId, int socket, net.neoforged.neoforge.fluids.FluidStack fluid) {
        AssemblyGraph.Node node=menu.controller.graph().node(nodeId);
        if(node==null || fluid.isEmpty() || minecraft.level==null) return;
        // For fluid ingredients, send the fluid as a resource location
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        PacketDistributor.sendToServer(new AssemblyLineJeiRecipeSelect(menu.containerId,nodeId, fluidId.toString(),""));
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
        if(minecraft.level==null || node.recipe==null || !node.recipe.startsWith("crystalnexus:jei/")) return List.of();
        ItemStack output=outputStack(node,0); if(output.isEmpty()) return List.of();
        Map<ResourceLocation,ItemStack> result=new LinkedHashMap<>();
        minecraft.level.getRecipeManager().getRecipes().forEach(holder -> {
            if(!ItemStack.isSameItemSameComponents(holder.value().getResultItem(minecraft.level.registryAccess()), output)) return;
            if(holder.value().getIngredients().isEmpty()) return;
            ItemStack[] choices=holder.value().getIngredients().getFirst().getItems();
            if(choices.length>0&&!choices[0].isEmpty()) result.putIfAbsent(BuiltInRegistries.ITEM.getKey(choices[0].getItem()), choices[0]);
        });
        return List.copyOf(result.values());
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
            ItemStack input=options.get(index), output=outputStack(node,0);
            PacketDistributor.sendToServer(new AssemblyLineJeiRecipeSelect(menu.containerId,node.id,
                BuiltInRegistries.ITEM.getKey(output.getItem()).toString(), BuiltInRegistries.ITEM.getKey(input.getItem()).toString()));
        }
        openInputNode = openInputSocket = -1;
        return true;
    }
    private boolean overNode(double lx,double ly){ for(AssemblyGraph.Node node:menu.controller.graph().nodes){int nx=nodeX(node),ny=nodeY(node); if(lx>=nx&&lx<=nx+NODE_W&&ly>=ny&&ly<=ny+nodeHeight(node)) return true;} return false; }
    private List<AssemblyGraph.Node> reversedNodes(){ List<AssemblyGraph.Node> result=new ArrayList<>(menu.controller.graph().nodes); return result.reversed(); }
    private boolean insideCanvas(double x,double y){return x>=canvasLeft&&x<canvasRight&&y>=canvasTop&&y<canvasBottom;}
    private static double distance(double x1,double y1,double x2,double y2){return Math.hypot(x2-x1,y2-y1);}
    private static String shortName(String value,int max){return value.length()<=max?value:value.substring(0,Math.max(1,max-1))+"…";}
    public record GhostTarget(Rect2i area,int nodeId,int socket){}

    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){super.render(graphics,mouseX,mouseY,partialTick);renderTooltip(graphics,mouseX,mouseY);}
    @Override protected void renderSlot(GuiGraphics graphics, net.minecraft.world.inventory.Slot slot) { }
}
