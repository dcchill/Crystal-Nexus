package net.crystalnexus.drone;

import net.crystalnexus.assembly.AssemblyGraph;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import java.util.*;

/** Cooperative server tasks; each inventory or world transaction finishes before the next task runs. */
public final class DroneProgram {
    public int node = -1;
    public int elapsed;
    public boolean running;
    public CompoundTag memory = new CompoundTag();
    public CompoundTag suspended = new CompoundTag();
    public static final class Task {
        private int node,elapsed;
        private CompoundTag memory;
        private Task(int node,int elapsed,CompoundTag memory){this.node=node;this.elapsed=elapsed;this.memory=memory;}
    }
    private final List<Task> tasks=new ArrayList<>();
    private Task selected;


    public static String validate(AssemblyGraph graph, DroneMode mode) {
        String error = graph.validate();
        if (!error.isEmpty()) return error;
        int starts = 0, events = 0;
        for (var n : graph.nodes) {
            try {
                DroneNode kind = DroneNode.of(n);
                if (!kind.available(mode)) return "Node is incompatible with " + mode;
                if (kind == DroneNode.START) starts++;
                if (kind == DroneNode.ON_RECALL_COMPLETE) events++;
                if (n.machine != 0 || n.inputSockets != 1 || n.outputSockets != 3) return "Invalid drone sockets";
                if (kind == DroneNode.REPEAT && graph.node(n.parameters.getInt("node")) == null) return "Repeat target does not exist";
                if (!validParameters(n.parameters)) return "Invalid node parameters";
                var fields=DroneNodeSettings.fields(kind);
                if(fields.contains("destinationSlot")&&!n.parameters.contains("destinationSlot")&&!n.parameters.contains("pos")&&!n.parameters.getBoolean("useSelected"))return kind.label()+": choose a destination card";
                if(fields.contains("endDestinationSlot")&&!n.parameters.contains("endDestinationSlot")&&!n.parameters.contains("end"))return kind.label()+": choose a second corner";
                if(fields.contains("waypointSlots")&&!n.parameters.contains("waypointSlots")&&!n.parameters.contains("waypoints"))return kind.label()+": choose patrol destinations";
            } catch (IllegalArgumentException e) { return "Unknown drone node"; }
        }
        if (starts != 1) return "Program requires exactly one Start";
        if (events > 1) return "Only one On Recall Complete entry is allowed";
        var outputs = new HashSet<String>();
        for (var edge : graph.edges) {
            if (edge.input() != 0 || edge.output() > 2) return "Invalid drone connection";
            boolean next=edge.output()==0&&!DroneNodeSettings.condition(DroneNode.of(graph.node(edge.from())));
            if(!next&&!outputs.add(edge.from()+":"+edge.output()))return "Only Next can connect to multiple instructions";
            var kind = DroneNode.of(graph.node(edge.to()));
            if (kind == DroneNode.START || kind == DroneNode.ON_RECALL_COMPLETE) return "Entry nodes cannot have incoming connections";
        }
        return "";
    }

    public static boolean validParameters(CompoundTag p) {
        if (p.size() > 32 || p.toString().length() > 8192) return false;
        for(String key:new String[]{"destinationSlot","endDestinationSlot"})
            if(p.contains(key)&&(!p.contains(key,Tag.TAG_INT)||p.getInt(key)<0||p.getInt(key)>=9))return false;
        if(p.contains("waypointSlots")){
            if(!p.contains("waypointSlots",Tag.TAG_INT_ARRAY))return false;
            int[] slots=p.getIntArray("waypointSlots");if(slots.length<1||slots.length>32)return false;
            for(int slot:slots)if(slot<0||slot>=9)return false;
        }
        for (String key : new String[]{"pos", "end"}) {
            if (!p.contains(key)) continue;
            int[] v = p.getIntArray(key);
            if (v.length != 3 || Math.abs((long)v[0]) > 29999984 || Math.abs((long)v[2]) > 29999984 || v[1] < -2048 || v[1] > 2048) return false;
        }
        for (String key : new String[]{"range", "value", "speed"})
            if (p.contains(key) && !Double.isFinite(p.getDouble(key))) return false;
        return (!p.contains("range") || p.getDouble("range") >= 0 && p.getDouble("range") <= 64)
            && (!p.contains("speed") || p.getDouble("speed") > 0 && p.getDouble("speed") <= 2)
            && p.getInt("ticks") >= 0 && p.getInt("ticks") <= 72000
            && p.getInt("count") >= 0 && p.getInt("count") <= 1000000
            && p.getInt("amount") >= 0 && p.getInt("amount") <= 16000;
    }

    public void start(AssemblyGraph graph) {
        tasks.clear();selected=null;
        node=graph.nodes.stream().filter(n->DroneNode.of(n)==DroneNode.START).mapToInt(n->n.id).findFirst().orElse(-1);
        elapsed=0;memory=new CompoundTag();suspended=new CompoundTag();running=node>=0;
    }
    private void sync(){if(selected!=null){selected.node=node;selected.elapsed=elapsed;selected.memory=memory;}}
    private void ensureTask(){if(selected==null&&node>=0){selected=new Task(node,elapsed,memory);tasks.add(selected);}sync();}
    private void selectFirst(){
        selected=tasks.isEmpty()?null:tasks.getFirst();
        if(selected==null){node=-1;elapsed=0;}
        else{node=selected.node;elapsed=selected.elapsed;memory=selected.memory;}
    }
    public List<Task> beginTick(){ensureTask();return List.copyOf(tasks);}
    public boolean activate(Task task){
        sync();if(!tasks.contains(task))return false;
        selected=task;node=task.node;elapsed=task.elapsed;memory=task.memory;return true;
    }
    public void finishTick(){sync();selectFirst();}
    public int[] activeNodes(){ensureTask();return running?tasks.stream().mapToInt(t->t.node).toArray():new int[0];}
    public void advance(AssemblyGraph graph,int output){
        int current=node;
        int[] next=graph.edges.stream().filter(e->e.from()==current&&e.output()==output).mapToInt(AssemblyGraph.Edge::to).toArray();
        if(next.length==0&&output==1&&graph.node(current)!=null&&!DroneNodeSettings.condition(DroneNode.of(graph.node(current))))
            next=graph.edges.stream().filter(e->e.from()==current&&e.output()==0).mapToInt(AssemblyGraph.Edge::to).toArray();
        if(output==2&&next.length==0){stop();return;}
        schedule(next);
    }
    public void schedule(int... next){
        ensureTask();CompoundTag inherited=memory.copy();clearProgress(inherited);tasks.remove(selected);selected=null;memory=inherited;
        for(int id:next)if(tasks.stream().noneMatch(t->t.node==id))tasks.add(new Task(id,0,inherited.copy()));
        if(tasks.size()>AssemblyGraph.MAX_NODES)throw new IllegalArgumentException("Too many active drone tasks");
        running=!tasks.isEmpty();selectFirst();
    }
    private static void clearProgress(CompoundTag memory){
        for(String key:new String[]{"scan","scanBest","work","workBlock","areaIndex","retreat","patrolIndex","patrol","pendingNodes"})memory.remove(key);
        DroneDestinations.clear(memory);
    }
    public void enter(int next){node=next;elapsed=0;clearProgress(memory);sync();}
    public void event(int entry){tasks.clear();selected=null;enter(entry);running=true;}
    public void stop(){tasks.clear();selected=null;node=-1;elapsed=0;running=false;clearProgress(memory);}
    public void suspend(DroneMode mode,int revision){
        if(suspended.isEmpty()){suspended=execution();suspended.putString("mode",mode.name());suspended.putInt("revision",revision);}
        running=false;
    }
    public boolean resume(DroneMode mode,AssemblyGraph graph){
        if(suspended.isEmpty()||!mode.name().equals(suspended.getString("mode"))||graph.revision!=suspended.getInt("revision")||graph.node(suspended.getInt("node"))==null)return false;
        var saved=suspended.getList("tasks",Tag.TAG_COMPOUND);if(saved.size()>AssemblyGraph.MAX_NODES)return false;
        var ids=new HashSet<Integer>();for(Tag raw:saved){int id=((CompoundTag)raw).getInt("node");if(graph.node(id)==null||!ids.add(id))return false;}
        for(int id:suspended.getCompound("memory").getIntArray("pendingNodes"))if(graph.node(id)==null)return false;
        restore(suspended);suspended=new CompoundTag();return true;
    }
    private CompoundTag execution(){
        ensureTask();CompoundTag p=new CompoundTag();p.putInt("node",node);p.putInt("elapsed",elapsed);p.putBoolean("running",running);p.put("memory",memory.copy());
        var saved=new net.minecraft.nbt.ListTag();for(var task:tasks){var t=new CompoundTag();t.putInt("node",task.node);t.putInt("elapsed",task.elapsed);t.put("memory",task.memory.copy());saved.add(t);}p.put("tasks",saved);return p;
    }
    private void restore(CompoundTag p){
        tasks.clear();selected=null;node=p.getInt("node");elapsed=p.getInt("elapsed");running=p.getBoolean("running");memory=p.getCompound("memory").copy();
        if(p.contains("tasks")){
            var saved=p.getList("tasks",Tag.TAG_COMPOUND);if(saved.size()>AssemblyGraph.MAX_NODES)throw new IllegalArgumentException("Too many saved drone tasks");
            var ids=new HashSet<Integer>();for(Tag raw:saved){var t=(CompoundTag)raw;int id=t.getInt("node");if(id<0||!ids.add(id))throw new IllegalArgumentException("Invalid saved drone task");tasks.add(new Task(id,Math.max(0,t.getInt("elapsed")),t.getCompound("memory").copy()));}
            selectFirst();
        }else if(node>=0){
            int[] pending=memory.getIntArray("pendingNodes");memory.remove("pendingNodes");ensureTask();
            for(int id:pending)if(id>=0&&tasks.size()<AssemblyGraph.MAX_NODES&&tasks.stream().noneMatch(t->t.node==id)){var local=memory.copy();clearProgress(local);tasks.add(new Task(id,0,local));}
        }
    }
    public CompoundTag save(){CompoundTag p=execution();p.put("suspended",suspended.copy());return p;}
    public void load(CompoundTag p){restore(p);suspended=p.getCompound("suspended").copy();}
}
