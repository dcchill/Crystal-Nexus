package net.crystalnexus.client.gui;

import net.crystalnexus.assembly.AssemblyGraph;
import net.crystalnexus.drone.*;
import net.crystalnexus.item.LinkCardData;
import net.crystalnexus.network.DroneControl;
import net.crystalnexus.world.inventory.DroneMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class DroneScreen extends AbstractContainerScreen<DroneMenu> {
    private record Choice(String title,String detail,Runnable select) {}
    private record Field(String key,int x,int y,int width) {}
    private AssemblyGraph graph;
    private DroneMode mode;
    private DroneEditorLayout layout;
    private int selected=-1,connecting=-1,output,dragging=-1,fieldScroll;
    private float panX=24,panY=24,zoom=1;
    private boolean advanced,inspector=true,autoResume;
    private final DroneEditorSaveState saves=new DroneEditorSaveState();
    private String error="",notice="";
    private final Map<Integer,Map<String,String>> drafts=new HashMap<>();
    private final Map<String,String> invalid=new HashMap<>();
    private final List<Field> renderedFields=new ArrayList<>();
    private int canvasX,canvasY,canvasW,canvasH,panelX,panelY,panelW,panelH;
    private boolean popup,nodesPopup;
    private String popupTitle="",query="",category="Common";
    private List<Choice> choices=List.of();
    private int scroll,highlight,px,py,pw,ph,listTop,listBottom;
    private EditBox search;

    public DroneScreen(DroneMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Override protected void init(){
        layout=DroneEditorLayout.fit(width,height);imageWidth=layout.width();imageHeight=layout.height();super.init();
        mode=DroneMode.read(menu.state.getString("mode"));
        if(graph==null)graph=AssemblyGraph.load(menu.state.getCompound("graph"),minecraft.level.registryAccess());
        button("Program",12,25,84,()->tab(false));button("Cargo & Destinations",100,25,Math.min(142,imageWidth-112),()->tab(true));
        if(!menu.cargoTab&&!(selected>=0&&inspector&&layout.overlayInspector()&&imageHeight<300)){
            button("Save & Run",12,50,92,()->save(true));
            button(menu.state.getBoolean("running")?"Pause":"Continue",108,50,64,()->send(menu.state.getBoolean("running")?"pause":"continue")).active=!popup&&menu.state.getInt("node")>=0;
            button("Recall",176,50,54,()->send("recall"));
            button("Actions",layout.toolbarRows()==1?234:12,layout.toolbarRows()==1?50:74,66,this::actions);
            button("+ Add Node",layout.toolbarRows()==1?304:82,layout.toolbarRows()==1?50:74,90,this::nodeBrowser);
            if(layout.toolbarRows()==2&&selected>=0)button(inspector?"Hide settings":"Settings",176,74,Math.min(100,imageWidth-188),()->{inspector=!inspector;rebuild();});
        }
        canvasX=leftPos+8;canvasY=topPos+layout.canvasTop();canvasW=imageWidth-16;canvasH=imageHeight-layout.canvasTop()-26;
        panelW=layout.inspectorWidth();panelX=leftPos+imageWidth-panelW-8;panelY=canvasY;panelH=canvasH;
        if(layout.overlayInspector()&&imageHeight<300){panelY=topPos+52;panelH=imageHeight-78;}
        if(!menu.cargoTab&&selected>=0&&inspector){if(!layout.overlayInspector())canvasW-=panelW+8;settings();}
        if(popup)popupWidgets();
    }
    private void rebuild(){clearWidgets();renderedFields.clear();init();}
    private Button button(String text,int x,int y,int w,Runnable run){
        var b=Button.builder(Component.literal(text),v->run.run()).bounds(leftPos+x,topPos+y,w,20).build();b.active=!popup;addRenderableWidget(b);return b;
    }
    private void tab(boolean cargo){menu.cargoTab=cargo;popup=false;CompoundTag p=new CompoundTag();p.putBoolean("cargo",cargo);PacketDistributor.sendToServer(new DroneControl(menu.containerId,"tab",p));rebuild();}
    private void send(String action){CompoundTag p=new CompoundTag();p.putBoolean("resume",autoResume);PacketDistributor.sendToServer(new DroneControl(menu.containerId,action,p));}
    private void save(boolean run){
        if(saves.pendingRevision>=0)return;
        if(!invalid.isEmpty()){error="Correct the highlighted setting before saving.";saves.closing=false;return;}
        String problem=DroneProgram.validate(graph,mode);if(!problem.isEmpty()){error=problem;saves.closing=false;return;}
        saves.begin(graph.revision);CompoundTag data=graph.save(minecraft.level.registryAccess());data.putInt("editRequest",saves.request);
        PacketDistributor.sendToServer(new DroneControl(menu.containerId,run?"graph_run":"graph",data));
        notice="Waiting for server to save";error="";
    }
    private void actions(){
        open("Actions",List.of(new Choice("Save Only","Store changes without starting.",()->save(false)),
            new Choice("Start Over","Restart the saved program at Start.",()->send("start")),
            new Choice("Stop","Stop the current program and navigation.",()->send("stop")),
            new Choice("Return Home","Recall to the saved home position.",()->send("home")),
            new Choice("Recall All","Recall all your owned drones.",()->send("recall_all")),
            new Choice("Resume Previous Task","Restore the task interrupted by recall.",()->send("resume")),
            new Choice("Auto Resume: "+(autoResume?"On":"Off"),"Resume the old task after a recall completes.",()->autoResume=!autoResume),
            new Choice(advanced?"Hide Advanced Settings":"Show Advanced Settings","Failure branches, coordinates, faces and component matching.",()->advanced=!advanced),
            new Choice("Fit Graph","Bring every node into view.",this::fitGraph),
            new Choice("Clear Selected Connections","Remove outgoing connections from the selected node.",()->{if(selected>=0){graph.edges.removeIf(e->e.from()==selected);changed();}}),
            new Choice("Reload Saved Program","Discard local edits and load the server's saved program.",()->{graph=AssemblyGraph.load(menu.state.getCompound("graph"),minecraft.level.registryAccess());drafts.clear();invalid.clear();saves.reset();selected=-1;error="";notice="Saved program loaded";})));
    }
    private void nodeBrowser(){nodesPopup=true;popup=true;popupTitle="Add Node";query="";scroll=highlight=0;rebuild();}
    private void open(String title,List<Choice> options){nodesPopup=false;popup=true;popupTitle=title;choices=options;query="";scroll=highlight=0;rebuild();}
    private void closePopup(){popup=false;search=null;rebuild();}
    private List<Choice> results(){
        if(nodesPopup)return DroneNodeSettings.search(mode,category,query).stream().map(kind->new Choice(DroneNodeSettings.title(kind),DroneNodeSettings.description(kind),()->add(kind))).toList();
        String q=query.toLowerCase(Locale.ROOT);return choices.stream().filter(c->(c.title+" "+c.detail).toLowerCase(Locale.ROOT).contains(q)).toList();
    }
    private void popupWidgets(){
        pw=Math.min(460,imageWidth-16);ph=Math.min(480,imageHeight-8);px=leftPos+(imageWidth-pw)/2;py=topPos+4;
        int searchY=py+(nodesPopup?84:32);listTop=searchY+26;listBottom=py+ph-24;
        search=new EditBox(font,px+10,searchY,pw-20,20,Component.literal("Search"));search.setMaxLength(120);search.setValue(query);
        search.setSuggestion(query.isEmpty()?(nodesPopup?"Search all nodes...":"Search..."):null);
        search.setResponder(value->{query=value;search.setSuggestion(value.isEmpty()?(nodesPopup?"Search all nodes...":"Search..."):null);scroll=highlight=0;});addRenderableWidget(search);setInitialFocus(search);
    }
    private void add(DroneNode kind){
        if(graph.nodes.size()>=AssemblyGraph.MAX_NODES){error="Maximum 128 nodes";return;}
        float x=selected()==null?(24-panX)/zoom:selected().x+baseWidth(selected())+30,y=selected()==null?(24-panY)/zoom:selected().y;
        var node=DroneNodeSettings.create(kind,graph.nextId(),x,y);
        boolean occupied=true;while(occupied){occupied=false;for(var existing:graph.nodes)
            if(x<existing.x+baseWidth(existing)+10&&x+baseWidth(node)+10>existing.x&&y<existing.y+baseHeight(existing)+10&&y+baseHeight(node)+10>existing.y){y=existing.y+baseHeight(existing)+20;occupied=true;break;}}
        node.y=y;graph.nodes.add(node);selected=node.id;fieldScroll=0;inspector=true;changed();
        panX=24-x*zoom;panY=24-y*zoom;notice="Choose settings, then connect this node.";
    }
    private AssemblyGraph.Node selected(){return graph.node(selected);}
    private void changed(){saves.changed();if(saves.pendingRevision<0)notice="";}
    private void set(String key,Tag value){var n=selected();if(n==null)return;if(value==null)n.parameters.remove(key);else n.parameters.put(key,value);drafts.computeIfAbsent(n.id,k->new HashMap<>()).remove(key);invalid.remove(n.id+":"+key);changed();}
    private String cardName(int slot){if(slot<0||slot>=9)return "Choose a card";ItemStack card=menu.cards.getStackInSlot(slot);return (slot+1)+". "+(card.isEmpty()?"Empty destination":LinkCardData.name(card));}
    private String destination(CompoundTag p,String key){
        if(p.contains(key))return cardName(p.getInt(key));
        String old=key.equals("endDestinationSlot")?"end":"pos";
        if(old.equals("pos")&&p.getBoolean("useSelected"))return "Detected target";
        if(p.contains(old))return "Legacy: "+format(old,p.get(old));return "Choose a destination card";
    }
    private void destinations(String key){
        List<Choice> list=new ArrayList<>();
        for(int i=0;i<9;i++){int slot=i;ItemStack card=menu.cards.getStackInSlot(i);list.add(new Choice(cardName(i),card.isEmpty()?"Install a Link Card on the Cargo & Destinations tab.":LinkCardData.summary(card),()->{
            set(key,IntTag.valueOf(slot));set(key.equals("endDestinationSlot")?"end":"pos",null);if(key.equals("destinationSlot"))set("useSelected",null);
        }));}
        if(key.equals("destinationSlot")&&!DroneNodeSettings.fields(DroneNode.of(selected())).contains("endDestinationSlot")&&DroneNode.of(selected())!=DroneNode.SET_HOME)
            list.add(new Choice("Use Detected Target","Use the position selected by a Find node.",()->{set(key,null);set("pos",null);set("useSelected",ByteTag.ONE);}));
        if(advanced)list.add(new Choice("Use Coordinates","Manually enter coordinates in Advanced settings.",()->{
            String old=key.equals("endDestinationSlot")?"end":"pos";set(key,null);set("useSelected",null);
            if(!selected().parameters.contains(old)){int[] here=menu.state.getIntArray("position");set(old,new IntArrayTag(here.length==3?here:new int[]{0,64,0}));}
        }));
        open("Assign Link Card",list);
    }
    private void route(){
        int[] cards=selected().parameters.getIntArray("waypointSlots");List<Choice> list=new ArrayList<>();
        for(int i=0;i<cards.length;i++){int index=i;list.add(new Choice("Stop "+(i+1)+": "+cardName(cards[i]),"Select to move or remove this stop.",()->routeStop(index)));}
        list.add(new Choice("+ Add Destination","Choose another installed Link Card.",()->{
            List<Choice> available=new ArrayList<>();for(int i=0;i<9;i++){int slot=i;available.add(new Choice(cardName(i),"Append this card to the patrol route.",()->{
                int[] old=selected().parameters.getIntArray("waypointSlots");if(old.length>=32){error="Maximum 32 patrol stops";return;}
                int[] next=Arrays.copyOf(old,old.length+1);next[old.length]=slot;set("waypointSlots",new IntArrayTag(next));set("waypoints",null);
            }));}open("Add Patrol Destination",available);
        }));open("Patrol Destinations",list);
    }
    private void routeStop(int index){
        int[] old=selected().parameters.getIntArray("waypointSlots");List<Choice> options=new ArrayList<>();
        if(index>0)options.add(new Choice("Move Up","Move this stop earlier in the route.",()->{int t=old[index-1];old[index-1]=old[index];old[index]=t;set("waypointSlots",new IntArrayTag(old));}));
        if(index+1<old.length)options.add(new Choice("Move Down","Move this stop later in the route.",()->{int t=old[index+1];old[index+1]=old[index];old[index]=t;set("waypointSlots",new IntArrayTag(old));}));
        options.add(new Choice("Remove Stop","Keep the physical card installed.",()->{int[] next=new int[old.length-1];System.arraycopy(old,0,next,0,index);System.arraycopy(old,index+1,next,index,old.length-index-1);set("waypointSlots",next.length==0?null:new IntArrayTag(next));}));
        open("Stop "+(index+1),options);
    }
    private void playerPicker(){
        List<Choice> list=new ArrayList<>();for(Tag raw:menu.state.getList("players",Tag.TAG_COMPOUND)){CompoundTag player=(CompoundTag)raw;String id=player.getString("id");list.add(new Choice(player.getString("name"),id.equals("owner")?"Your drone's owner":"Authorized follow, recall or delivery target",()->set("player",StringTag.valueOf(id))));}open("Choose Player",list);
    }
    private String playerName(String id){for(Tag raw:menu.state.getList("players",Tag.TAG_COMPOUND)){CompoundTag p=(CompoundTag)raw;if(p.getString("id").equals(id))return p.getString("name");}return id.equals("owner")||id.isEmpty()?"Owner":"Unavailable player";}
    private List<String> fieldKeys(){
        if(selected()==null)return List.of();var kind=DroneNode.of(selected());List<String> keys=new ArrayList<>(DroneNodeSettings.fields(kind));
        if(advanced){
            keys.add("waitUntilComplete");
            if(keys.contains("destinationSlot")&&!selected().parameters.contains("destinationSlot")&&selected().parameters.contains("pos"))keys.add("pos");
            if(keys.contains("endDestinationSlot")&&!selected().parameters.contains("endDestinationSlot")&&selected().parameters.contains("end"))keys.add("end");
            if(kind==DroneNode.FILTER_ITEMS)keys.add("components");
            if(switch(kind){case EXTRACT_ITEMS,INSERT_ITEMS,RETRIEVE_REQUESTED_ITEMS,EXTRACT_FLUID,INSERT_FLUID,FIND_INVENTORY,FIND_FLUID_TANK->true;default->false;})keys.add("side");
        }
        return keys;
    }
    private int visibleFields(){return Math.max(1,(panelH-103)/42);}
    private void settings(){
        var node=selected();var kind=DroneNode.of(node);List<String> keys=fieldKeys();fieldScroll=Math.max(0,Math.min(fieldScroll,Math.max(0,keys.size()-visibleFields())));
        int x=panelX-leftPos+10,w=panelW-20;
        for(int i=fieldScroll;i<Math.min(keys.size(),fieldScroll+visibleFields());i++){
            String key=keys.get(i);int y=panelY-topPos+66+(i-fieldScroll)*42;renderedFields.add(new Field(key,panelX+10,topPos+y,w));
            switch(key){
                case "destinationSlot","endDestinationSlot"->button(fit(destination(node.parameters,key),w-12),x,y+12,w,()->destinations(key));
                case "waypointSlots"->button(node.parameters.contains("waypoints")&&!node.parameters.contains(key)?"Legacy route - assign cards":node.parameters.getIntArray(key).length+" stops - edit route",x,y+12,w,this::route);
                case "player"->button(fit(playerName(node.parameters.getString(key)),w-12),x,y+12,w,this::playerPicker);
                case "operator"->button(node.parameters.getString(key),x,y+12,w,()->open("Comparison",Arrays.stream(new String[]{">=",">","=","!=","<","<="}).map(op->new Choice(op,"Compare the sensor or variable with the value.",()->set(key,StringTag.valueOf(op)))).toList()));
                case "node"->button(node.parameters.getInt(key)+": "+(graph.node(node.parameters.getInt(key))==null?"Choose a node":DroneNodeSettings.title(DroneNode.of(graph.node(node.parameters.getInt(key))))),x,y+12,w,()->open("Repeat From",graph.nodes.stream().filter(n->n.id!=node.id).map(n->new Choice(n.id+": "+DroneNodeSettings.title(DroneNode.of(n)),"Continue execution from this node.",()->set(key,IntTag.valueOf(n.id)))).toList()));
                case "resume","enabled","waitUntilComplete"->button(node.parameters.getBoolean(key)?"On":"Off",x,y+12,w,()->{set(key,ByteTag.valueOf(!node.parameters.getBoolean(key)));rebuild();});
                case "side"->button(node.parameters.contains(key)?node.parameters.getString(key):"From Link Card",x,y+12,w,()->{
                    List<Choice> list=new ArrayList<>();list.add(new Choice("From Link Card","Use the face recorded when the card was linked.",()->set(key,null)));for(Direction side:Direction.values())list.add(new Choice(side.getName(),"Access this face of the container or tank.",()->set(key,StringTag.valueOf(side.getName()))));open("Access Face",list);
                });
                default->textField(node,key,x,y+12,w);
            }
        }
        button(advanced?"Advanced: On":"Advanced",x,panelY-topPos+panelH-47,w/2-2,()->{advanced=!advanced;rebuild();});
        button("Delete Node",x+w/2+2,panelY-topPos+panelH-47,w/2-2,()->{int id=selected;graph.nodes.removeIf(n->n.id==id);graph.edges.removeIf(e->e.from()==id||e.to()==id);drafts.remove(id);invalid.keySet().removeIf(k->k.startsWith(id+":"));selected=-1;changed();rebuild();});
        button("Close Settings",x,panelY-topPos+panelH-23,w,()->{inspector=false;rebuild();});
    }
    private void textField(AssemblyGraph.Node node,String key,int x,int y,int w){
        boolean numeric=Set.of("ticks","count","amount","range","speed","value").contains(key);
        int fieldWidth=numeric?w-44:w;
        var field=new EditBox(font,leftPos+x,topPos+y,fieldWidth,20,Component.literal(DroneNodeSettings.label(DroneNode.of(node),key)));field.setMaxLength(8192);
        String value=drafts.getOrDefault(node.id,Map.of()).getOrDefault(key,format(key,node.parameters.get(key)));field.setValue(value);
        if(key.equals("filter"))field.setHint(Component.literal("Item ID / #tag, or drop an item"));
        field.setEditable(!popup);field.setResponder(text->{drafts.computeIfAbsent(node.id,k->new HashMap<>()).put(key,text);changed();
            try{Tag parsed=parse(key,text,node.parameters.get(key));CompoundTag candidate=node.parameters.copy();candidate.put(key,parsed);
                if(!DroneProgram.validParameters(candidate))throw new IllegalArgumentException("Outside allowed range");
                node.parameters=candidate;invalid.remove(node.id+":"+key);
            }catch(Exception e){invalid.put(node.id+":"+key,DroneNodeSettings.label(DroneNode.of(node),key)+": "+e.getMessage());}
        });addRenderableWidget(field);
        if(numeric){button("-",x+w-42,y,20,()->adjust(node,key,-1));button("+",x+w-20,y,20,()->adjust(node,key,1));}
    }
    private void adjust(AssemblyGraph.Node node,String key,int direction){
        try{String raw=drafts.getOrDefault(node.id,Map.of()).getOrDefault(key,format(key,node.parameters.get(key)));
            double value=Double.parseDouble(raw)+direction*(key.equals("speed")?.1:1);value=Math.round(value*1000)/1000.0;
            double min=key.equals("speed")?.1:key.equals("amount")?1:key.equals("value")?-Double.MAX_VALUE:0;
            value=Math.max(min,value);Tag parsed=(key.equals("count")||key.equals("amount"))?IntTag.valueOf((int)value):parse(key,Double.toString(value),node.parameters.get(key));
            CompoundTag p=node.parameters.copy();p.put(key,parsed);if(!DroneProgram.validParameters(p))return;
            set(key,parsed);rebuild();
        }catch(Exception e){error="Enter a valid number before adjusting it.";}
    }
    private static String format(String key,Tag tag){
        if(tag==null)return key.equals("components")?"{}":"";
        if(key.equals("ticks"))return Double.toString(((NumericTag)tag).getAsDouble()/20);
        if(tag instanceof IntArrayTag array){int[] v=array.getAsIntArray();return Arrays.stream(v).mapToObj(Integer::toString).collect(java.util.stream.Collectors.joining(" "));}
        if(tag instanceof ListTag list){List<String> values=new ArrayList<>();for(Tag t:list){if(t instanceof CompoundTag b)values.add(format("offset",b.get("offset"))+"="+b.getString("item"));else values.add(format("pos",t));}return String.join("; ",values);}
        return tag.getAsString();
    }
    private static int[] coordinates(String value){String[] pieces=value.trim().split("[ ,]+");if(pieces.length!=3)throw new IllegalArgumentException("Enter x y z");return Arrays.stream(pieces).mapToInt(Integer::parseInt).toArray();}
    private static Tag parse(String key,String value,Tag previous)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        if(key.equals("pos")||key.equals("end"))return new IntArrayTag(coordinates(value));
        if(key.equals("components"))return TagParser.parseTag(value);
        if(key.equals("blocks")){ListTag list=new ListTag();for(String line:value.split(";")){if(line.isBlank())continue;String[] pair=line.trim().split("=",2);if(pair.length!=2)throw new IllegalArgumentException("Use dx dy dz=item");CompoundTag b=new CompoundTag();b.putIntArray("offset",coordinates(pair[0]));b.putString("item",pair[1].trim());list.add(b);}return list;}
        if(Set.of("filter","variable","a","b").contains(key))return StringTag.valueOf(value.trim());
        if(key.equals("ticks")){double seconds=Double.parseDouble(value);if(!Double.isFinite(seconds)||seconds<0||seconds>3600)throw new IllegalArgumentException("Use 0 to 3600 seconds");return IntTag.valueOf((int)Math.round(seconds*20));}
        if(Set.of("count","amount").contains(key))return IntTag.valueOf(Integer.parseInt(value.trim()));
        return DoubleTag.valueOf(Double.parseDouble(value.trim()));
    }
    @Override public void onClose(){
        if(!invalid.isEmpty()){save(false);saves.closing=false;return;}
        if(!saves.dirty&&saves.pendingRevision<0){super.onClose();return;}
        saves.closing=true;
        if(saves.pendingRevision<0)save(false);
    }
    @Override protected void containerTick(){
        super.containerTick();DroneMode next=DroneMode.read(menu.state.getString("mode"));int revision=menu.state.getCompound("graph").getInt("revision");
        if(next!=mode){error=saves.dirty?"Mode changed; local changes were not saved":"";graph=AssemblyGraph.load(menu.state.getCompound("graph"),minecraft.level.registryAccess());drafts.clear();invalid.clear();saves.reset();selected=-1;popup=false;rebuild();return;}
        if(menu.state.contains("submittedRevision")&&saves.acknowledge(menu.state.getInt("submittedRevision"),menu.state.getInt("editRequest"),menu.state.getBoolean("editAccepted"))){
            if(!menu.state.getBoolean("editAccepted")){error=menu.state.getString("editError");notice="";}
            else {
                graph.revision=revision;
                if(!saves.dirty){graph=AssemblyGraph.load(menu.state.getCompound("graph"),minecraft.level.registryAccess());drafts.clear();invalid.clear();}
                notice="Program saved";rebuild();
                if(saves.canClose()){super.onClose();return;}
                if(saves.closing)onClose();
            }
        }
        else if(revision!=graph.revision){if(!saves.dirty&&saves.pendingRevision<0){graph=AssemblyGraph.load(menu.state.getCompound("graph"),minecraft.level.registryAccess());drafts.clear();invalid.clear();notice="Program saved";rebuild();}else error="Saved program changed. Use Actions > Reload Saved Program.";}
    }
    private int nodeX(AssemblyGraph.Node node){return canvasX+(int)(panX+node.x*zoom);}
    private int nodeY(AssemblyGraph.Node node){return canvasY+(int)(panY+node.y*zoom);}
    private int baseWidth(AssemblyGraph.Node node){return Math.max(220,Math.min(360,font.width(DroneNodeSettings.title(DroneNode.of(node)))+50));}
    private int nodeWidth(AssemblyGraph.Node node){return (int)(baseWidth(node)*zoom);}
    private List<FormattedCharSequence> titleLines(AssemblyGraph.Node node){return font.split(Component.literal(DroneNodeSettings.title(DroneNode.of(node))),baseWidth(node)-48);}
    private int headerHeight(AssemblyGraph.Node node){return Math.max(24,12+titleLines(node).size()*font.lineHeight);}
    private List<FormattedCharSequence> descriptionLines(AssemblyGraph.Node node){
        String description=DroneNodeSettings.description(DroneNode.of(node)),summary=nodeSummary(node);
        return font.split(Component.literal(description.equals(summary)?description:description+"\n"+summary),baseWidth(node)-16);
    }
    private int socketOffset(AssemblyGraph.Node node){return headerHeight(node)+16+descriptionLines(node).size()*font.lineHeight;}
    private int[] outputs(AssemblyGraph.Node node){List<Integer> list=new ArrayList<>();list.add(0);if(advanced||DroneNodeSettings.condition(DroneNode.of(node))||graph.edges.stream().anyMatch(e->e.from()==node.id&&e.output()==1))list.add(1);if(advanced||graph.edges.stream().anyMatch(e->e.from()==node.id&&e.output()==2))list.add(2);return list.stream().mapToInt(Integer::intValue).toArray();}
    private int socketY(AssemblyGraph.Node node,int out){int[] visible=outputs(node);for(int i=0;i<visible.length;i++)if(visible[i]==out)return nodeY(node)+(int)((socketOffset(node)+i*18)*zoom);return inputY(node);}
    private int inputY(AssemblyGraph.Node node){return nodeY(node)+(int)(socketOffset(node)*zoom);}
    private int baseHeight(AssemblyGraph.Node node){return socketOffset(node)+10+outputs(node).length*18;}
    private int nodeHeight(AssemblyGraph.Node node){return (int)(baseHeight(node)*zoom);}
    private void fitGraph(){
        if(graph.nodes.isEmpty())return;float minX=Float.MAX_VALUE,minY=minX,maxX=-minX,maxY=-minX;
        for(var n:graph.nodes){minX=Math.min(minX,n.x);minY=Math.min(minY,n.y);maxX=Math.max(maxX,n.x+baseWidth(n));maxY=Math.max(maxY,n.y+baseHeight(n));}
        zoom=DroneEditorLayout.clampZoom(Math.min(1.2F,Math.min((canvasW-24)/(maxX-minX),(canvasH-24)/(maxY-minY))));panX=12-minX*zoom;panY=12-minY*zoom;
    }
    private String nodeSummary(AssemblyGraph.Node node){var p=node.parameters;if(DroneNodeSettings.fields(DroneNode.of(node)).contains("destinationSlot"))return destination(p,"destinationSlot");if(p.contains("player"))return playerName(p.getString("player"));return DroneNodeSettings.description(DroneNode.of(node));}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        ControllerGuiStyle.panel(g,leftPos,topPos,imageWidth,imageHeight);
        if(menu.cargoTab){
            for(var slot:menu.slots)if(slot.isActive())ControllerGuiStyle.inset(g,leftPos+slot.x-1,topPos+slot.y-1,18,18);
            return;
        }
        ControllerGuiStyle.inset(g,canvasX,canvasY,canvasW,canvasH);g.enableScissor(canvasX,canvasY,canvasX+canvasW,canvasY+canvasH);
        for(int x=canvasX+Math.floorMod((int)panX,24);x<canvasX+canvasW;x+=24)for(int y=canvasY+Math.floorMod((int)panY,24);y<canvasY+canvasH;y+=24)g.fill(x,y,x+1,y+1,0xff777777);
        for(var edge:graph.edges){var from=graph.node(edge.from());var to=graph.node(edge.to());if(from!=null&&to!=null)AssemblyGraphDrawing.cable(g,nodeX(from)+nodeWidth(from),socketY(from,edge.output()),nodeX(to),inputY(to),edge.output()==2?0xffb55d5d:0xff3c76b8);}
        for(var node:graph.nodes){
            g.pose().pushPose();g.pose().translate(nodeX(node),nodeY(node),0);g.pose().scale(zoom,zoom,1);
            int w=baseWidth(node),h=baseHeight(node),header=headerHeight(node),input=socketOffset(node);
            boolean active=menu.state.getBoolean("running")&&mode.name().equals(menu.state.getString("mode"))&&graph.revision==menu.state.getCompound("graph").getInt("revision")&&(menu.state.contains("activeNodes")?Arrays.stream(menu.state.getIntArray("activeNodes")).anyMatch(id->id==node.id):node.id==menu.state.getInt("node"));
            if(active){
                int pulse=90+(int)(35*(1+Math.sin(net.minecraft.Util.getMillis()/180.0)));
                for(int radius=7;radius>=1;radius--)g.fill(-radius,-radius,w+radius,h+radius,((pulse/(radius+1))<<24)|0x42ef85);
            }
            ControllerGuiStyle.panel(g,0,0,w,h);
            g.fill(3,3,w-3,header-1,active?0xff86e4a7:node.id==selected?0xffa5c1dc:0xffb3b3b3);
            var titles=titleLines(node);for(int i=0;i<titles.size();i++)g.drawString(font,titles.get(i),8,7+i*font.lineHeight,0xff202020,false);
            g.drawString(font,"#"+node.id,w-36,9,active?0xff145b2c:0xff505050,false);
            var descriptions=descriptionLines(node);for(int i=0;i<descriptions.size();i++)g.drawString(font,descriptions.get(i),8,header+7+i*font.lineHeight,0xff404040,false);
            g.fill(-4,input-4,4,input+4,0xffffc857);g.drawString(font,"In",9,input-4,0xff606060,false);
            int sy=input;for(int out:outputs(node)){String label=out==2?"If failed":DroneNodeSettings.condition(DroneNode.of(node))?(out==0?"True":"False"):out==0?"Next":"Other";
                long links=graph.edges.stream().filter(e->e.from()==node.id&&e.output()==out).count();if(links>1)label+=" ("+links+")";
                g.drawString(font,label,w-font.width(label)-12,sy-4,out==2?0xff8d3030:0xff303030,false);g.fill(w-4,sy-4,w+4,sy+4,out==2?0xffbb5555:0xff477db5);sy+=18;
            }
            if(active){g.hLine(0,w,0,0xff42ef85);g.hLine(0,w,h,0xff42ef85);g.vLine(0,0,h,0xff42ef85);g.vLine(w,0,h,0xff42ef85);}
            g.pose().popPose();
        }
        if(connecting>=0&&graph.node(connecting)!=null){var from=graph.node(connecting);AssemblyGraphDrawing.cable(g,nodeX(from)+nodeWidth(from),socketY(from,output),mx,my,0xffffc857);}
        g.disableScissor();
        if(selected()!=null&&inspector){ControllerGuiStyle.panel(g,panelX,panelY,panelW,panelH);var kind=DroneNode.of(selected());
            g.drawString(font,fit(DroneNodeSettings.title(kind),panelW-20),panelX+10,panelY+10,0xff202020,false);
            var lines=font.split(Component.literal(DroneNodeSettings.description(kind)),panelW-20);for(int i=0;i<Math.min(3,lines.size());i++)g.drawString(font,lines.get(i),panelX+10,panelY+25+i*10,0xff505050,false);
            for(var field:renderedFields){g.drawString(font,fit(DroneNodeSettings.label(kind,field.key),field.width),field.x,field.y,invalid.containsKey(selected+":"+field.key)?0xffa02020:0xff404040,false);}
            if(fieldKeys().size()>visibleFields()){
                int track=panelH-116,thumb=Math.max(8,track*visibleFields()/fieldKeys().size());
                g.fill(panelX+panelW-7,panelY+64,panelX+panelW-4,panelY+64+track,0xff777777);
                int offset=(track-thumb)*fieldScroll/Math.max(1,fieldKeys().size()-visibleFields());
                g.fill(panelX+panelW-7,panelY+64+offset,panelX+panelW-4,panelY+64+offset+thumb,0xff404040);
            }
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,fit(mode.toString().replace('_',' ')+"  |  "+menu.state.getInt("energy")/2000+"% battery  |  "+menu.state.getString("status"),imageWidth-(saves.dirty?90:24)),12,10,0xff303030,false);
        if(saves.dirty)g.drawString(font,"Unsaved",imageWidth-63,10,0xff805500,false);
        if(menu.cargoTab){g.drawString(font,"Cargo"+(mode==DroneMode.FLUID_TRANSPORT?" | "+menu.state.getCompound("tank").getCompound("Fluid").getInt("amount")+" mB":""),18,49,0xff303030,false);g.drawString(font,"Destination cards",18,80,0xff303030,false);g.drawString(font,"Inventory",18,124,0xff303030,false);
            for(int i=0;i<9;i++)g.drawString(font,Integer.toString(i+1),22+i*18,112,0xff404040,false);
            int x=212;int w=imageWidth-x-12;if(w>80){g.drawString(font,"Changes apply to the next task.",x,61,0xff303030,false);for(int i=0;i<9;i++)g.drawString(font,fit(cardName(i),w),x,79+i*14,0xff404040,false);}
        }
        String message=!invalid.isEmpty()?invalid.values().iterator().next():!error.isEmpty()?error:!notice.isEmpty()?notice:menu.cargoTab?"Install linked cards here; choose their numbered slots in Program.":"Drag headers. Next supports multiple inputs; reconnect to unlink. Scroll to zoom; middle-drag to pan.";
        g.drawString(font,fit(message,imageWidth-24),12,imageHeight-(menu.cargoTab?12:16),!invalid.isEmpty()||!error.isEmpty()?0xffa02020:0xff404040,false);
    }
    private void renderPopup(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0x99000000);ControllerGuiStyle.panel(g,px,py,pw,ph);g.drawString(font,popupTitle,px+10,py+11,0xff202020,false);g.drawString(font,"X",px+pw-19,py+11,0xff303030,false);
        if(nodesPopup){String[] cats={"Common","Movement","Mode actions","Conditions","Logic","Events"};int cell=(pw-20)/3;for(int i=0;i<cats.length;i++){int x=px+10+(i%3)*cell,y=py+31+(i/3)*23;g.fill(x,y,x+cell-3,y+20,cats[i].equals(category)?0xffa5c1dc:0xffb3b3b3);g.drawString(font,cats[i],x+5,y+6,0xff303030,false);}}
        List<Choice> list=results();int rows=Math.max(1,(listBottom-listTop)/36);scroll=Math.max(0,Math.min(scroll,Math.max(0,list.size()-rows)));highlight=Math.max(0,Math.min(highlight,Math.max(0,list.size()-1)));
        g.enableScissor(px+8,listTop,px+pw-8,listBottom);
        for(int i=scroll;i<Math.min(list.size(),scroll+rows);i++){int y=listTop+(i-scroll)*36;var choice=list.get(i);boolean hover=mx>=px+10&&mx<px+pw-10&&my>=y&&my<y+34;
            g.fill(px+10,y,px+pw-10,y+34,hover||i==highlight?0xffb7cce0:0xffdddddd);g.drawString(font,fit(choice.title,pw-32),px+16,y+5,0xff202020,false);g.drawString(font,fit(choice.detail,pw-32),px+16,y+20,0xff505050,false);
        }
        if(list.isEmpty())g.drawString(font,"No matching choices",px+16,listTop+10,0xff505050,false);g.disableScissor();
        g.drawString(font,fit("Enter: choose   Esc: back   Scroll for more",pw-20),px+10,py+ph-15,0xff505050,false);search.render(g,mx,my,partial);
    }
    private boolean inCanvas(double x,double y){return x>=canvasX&&x<canvasX+canvasW&&y>=canvasY&&y<canvasY+canvasH;}
    private boolean inSettings(double x,double y){return selected()!=null&&inspector&&x>=panelX&&x<panelX+panelW&&y>=panelY&&y<panelY+panelH;}
    private String fit(String text,int pixels){return font.plainSubstrByWidth(text,Math.max(0,pixels));}
    private void choose(Choice choice){popup=false;nodesPopup=false;search=null;choice.select.run();rebuild();}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(popup){
            if(x>=px+pw-28&&x<=px+pw&&y>=py&&y<py+28){closePopup();return true;}
            if(nodesPopup&&x>=px+10&&x<px+pw-10&&y>=py+31&&y<py+77){int col=(int)(x-px-10)/((pw-20)/3),row=(int)(y-py-31)/23;int i=col+row*3;if(i>=0&&i<6){category=new String[]{"Common","Movement","Mode actions","Conditions","Logic","Events"}[i];query="";scroll=highlight=0;rebuild();}return true;}
            if(y>=listTop&&y<listBottom&&x>=px+10&&x<px+pw-10){int index=scroll+(int)(y-listTop)/36;var list=results();if(index<list.size())choose(list.get(index));return true;}
            if(search!=null)search.mouseClicked(x,y,button);return true;
        }
        if(!menu.cargoTab&&inSettings(x,y)){
            if(!menu.getCarried().isEmpty())for(var field:renderedFields)if(field.key.equals("filter")&&x>=field.x&&x<field.x+field.width&&y>=field.y+12&&y<field.y+34){set("filter",StringTag.valueOf(BuiltInRegistries.ITEM.getKey(menu.getCarried().getItem()).toString()));rebuild();return true;}
            return super.mouseClicked(x,y,button);
        }
        if(!menu.cargoTab&&inCanvas(x,y)){
            for(var n:graph.nodes.reversed()){int nx=nodeX(n),ny=nodeY(n),w=nodeWidth(n);
                if(button==0&&Math.abs(x-(nx+w))<9*zoom){for(int out:outputs(n))if(Math.abs(y-socketY(n,out))<9*zoom){connecting=n.id;output=out;return true;}}
                if(button==0&&connecting>=0&&Math.abs(x-nx)<9*zoom&&Math.abs(y-inputY(n))<9*zoom){
                    if(connecting!=n.id){
                        var edge=new AssemblyGraph.Edge(connecting,output,n.id,0);
                        if(graph.edges.contains(edge)){graph.edges.remove(edge);changed();}
                        else{
                            boolean multiple=output==0&&!DroneNodeSettings.condition(DroneNode.of(graph.node(connecting)));
                            var old=multiple?List.<AssemblyGraph.Edge>of():graph.edges.stream().filter(e->e.from()==connecting&&e.output()==output).toList();
                            graph.edges.removeAll(old);graph.edges.add(edge);String issue=graph.validate();
                            if(!issue.isEmpty()){graph.edges.remove(edge);graph.edges.addAll(old);error=issue;}else changed();
                        }
                    }
                    connecting=-1;return true;
                }
                if(x>=nx&&x<nx+w&&y>=ny&&y<ny+nodeHeight(n)){selected=n.id;inspector=true;fieldScroll=0;dragging=button==0&&y<ny+headerHeight(n)*zoom?n.id:-1;rebuild();return true;}
            }
            if(button==0&&connecting>=0){connecting=-1;return true;}if(button==0){selected=-1;rebuild();return true;}
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(popup)return true;if(!menu.cargoTab&&dragging>=0){var n=graph.node(dragging);n.x+=dx/zoom;n.y+=dy/zoom;changed();return true;}if(!menu.cargoTab&&button==2&&inCanvas(x,y)&&!inSettings(x,y)){panX+=dx;panY+=dy;return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){dragging=-1;if(popup)return true;return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(popup){scroll=Math.max(0,scroll-(int)sy);highlight=scroll;return true;}
        if(!menu.cargoTab&&inSettings(x,y)){fieldScroll=Math.max(0,fieldScroll-(int)sy);rebuild();return true;}
        if(!menu.cargoTab&&inCanvas(x,y)){float next=DroneEditorLayout.clampZoom(zoom*(float)Math.pow(1.1,sy));panX=(float)(x-canvasX-(x-canvasX-panX)*next/zoom);panY=(float)(y-canvasY-(y-canvasY-panY)*next/zoom);zoom=next;return true;}
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(popup){if(key==GLFW.GLFW_KEY_ESCAPE){closePopup();return true;}var list=results();
            if(key==GLFW.GLFW_KEY_DOWN||key==GLFW.GLFW_KEY_UP){highlight=Math.max(0,Math.min(list.size()-1,highlight+(key==GLFW.GLFW_KEY_DOWN?1:-1)));int rows=Math.max(1,(listBottom-listTop)/36);if(highlight<scroll)scroll=highlight;if(highlight>=scroll+rows)scroll=highlight-rows+1;return true;}
            if(key==GLFW.GLFW_KEY_ENTER&&highlight>=0&&highlight<list.size()){choose(list.get(highlight));return true;}if(search!=null)return search.keyPressed(key,scan,modifiers);return true;
        }
        if(key==GLFW.GLFW_KEY_ESCAPE&&connecting>=0){connecting=-1;return true;}return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean charTyped(char c,int modifiers){if(popup)return search!=null&&search.charTyped(c,modifiers);return super.charTyped(c,modifiers);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        super.render(g,mx,my,partial);if(popup){g.flush();g.pose().pushPose();g.pose().translate(0,0,400);renderPopup(g,mx,my,partial);g.flush();g.pose().popPose();return;}renderTooltip(g,mx,my);
        if(menu.cargoTab&&hoveredSlot!=null&&hoveredSlot.index>=9&&hoveredSlot.index<18&&hoveredSlot.hasItem())g.renderTooltip(font,font.split(Component.literal(cardName(hoveredSlot.index-9)+"\n"+LinkCardData.summary(hoveredSlot.getItem())),260),mx,my);
        if(!menu.cargoTab&&!inSettings(mx,my)&&inCanvas(mx,my))for(var n:graph.nodes.reversed())if(mx>=nodeX(n)&&mx<nodeX(n)+nodeWidth(n)&&my>=nodeY(n)&&my<nodeY(n)+headerHeight(n)*zoom){g.renderTooltip(font,Component.literal(DroneNodeSettings.title(DroneNode.of(n))),mx,my);break;}
    }
}
