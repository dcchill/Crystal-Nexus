package net.crystalnexus.assembly;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import java.util.*;

/** Server-owned, directed acyclic assembly program. UI coordinates are persisted with the program. */
public final class AssemblyGraph {
    public static final int MAX_NODES = 128;
    public static final int MAX_EDGES = 512;
    public int revision;
    public boolean enabled;
    public final List<Node> nodes = new ArrayList<>();
    public final List<Edge> edges = new ArrayList<>();

    public static final class Node {
        public final int id;
        public long machine;
        public String block;
        public float x, y;
        public String recipe = "";
        public String status = "Detected";
        public int inputSockets = 1;
        public int[] inputSlots = new int[0];
        public String[] inputItems = new String[] { "" };
        public String[] inputFluids = new String[] { "" };
        public int outputSockets = 1;
        public int[] outputSlots = new int[] { 1 };
        public String[] outputItems = new String[] { "" };
        public String[] outputFluids = new String[0];
        /** Explicit terminal routing. A disconnected output is exported only when its flag is set. */
        public boolean[] outputExport = new boolean[] { true };
        public Node(int id, long machine, String block, float x, float y) {
            this.id = id; this.machine = machine; this.block = block; this.x = x; this.y = y;
        }
    }
    public record Edge(int from, int output, int to, int input) {}

    public Node node(int id) { return nodes.stream().filter(n -> n.id == id).findFirst().orElse(null); }
    public int nextId() { return nodes.stream().mapToInt(n -> n.id).max().orElse(-1) + 1; }
    public void syncRevision() { revision++; }

    /** Returns an actionable validation message, or an empty string when executable. */
    public String validate() {
        if (nodes.size() > MAX_NODES) return "Too many graph nodes";
        if (edges.size() > MAX_EDGES) return "Too many graph connections";
        Set<Integer> ids = new HashSet<>(); Set<Long> machines = new HashSet<>();
        for (Node n : nodes) {
            if (!ids.add(n.id) || n.id < 0 || n.block == null || n.block.length() > 256
                ) return "Invalid graph node";
            if (n.recipe == null || n.status == null || n.inputSlots == null || n.inputItems == null || n.outputSlots == null
                || n.inputFluids == null || n.outputItems == null || n.outputFluids == null || n.outputExport == null) return "Invalid graph metadata";
            if (n.machine != 0 && !machines.add(n.machine)) return "A machine is assigned more than once";
            if (!Float.isFinite(n.x) || !Float.isFinite(n.y) || Math.abs(n.x) > 100000 || Math.abs(n.y) > 100000) return "Invalid node position";
        }
        Set<Edge> unique = new HashSet<>(); Map<Integer,List<Integer>> out = new HashMap<>();
        for (Edge e : edges) {
            Node from = node(e.from), to = node(e.to);
            if (!unique.add(e) || from == null || to == null || e.from == e.to || e.output < 0 || e.input < 0 || e.output > 31 || e.input > 31) return "Invalid graph connection";
            if (from.outputSockets > 0 && e.output >= from.outputSockets) return "Output socket is not available";
            if (to.inputSockets > 0 && e.input >= to.inputSockets) return "Input socket is not available";
            out.computeIfAbsent(e.from, ignored -> new ArrayList<>()).add(e.to);
        }
        Map<Integer,Integer> marks = new HashMap<>();
        for (Node n : nodes) if (cycle(n.id, out, marks)) return "Graph connections cannot contain a cycle";
        return "";
    }
    private boolean cycle(int id, Map<Integer,List<Integer>> out, Map<Integer,Integer> marks) {
        if (marks.getOrDefault(id, 0) == 1) return true;
        if (marks.getOrDefault(id, 0) == 2) return false;
        marks.put(id, 1); for (int next : out.getOrDefault(id, List.of())) if (cycle(next, out, marks)) return true;
        marks.put(id, 2); return false;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag(); tag.putInt("revision", revision); tag.putBoolean("enabled", enabled);
        ListTag ns = new ListTag(); for (Node n : nodes) { CompoundTag t = new CompoundTag(); t.putInt("id", n.id); t.putLong("machine", n.machine); t.putString("block", safe(n.block)); t.putFloat("x", n.x); t.putFloat("y", n.y); t.putString("recipe", safe(n.recipe)); t.putString("status", safe(n.status)); t.putInt("inputSockets", n.inputSockets); t.putIntArray("inputSlots", ints(n.inputSlots)); putStrings(t, "inputItems", n.inputItems); putStrings(t, "inputFluids", n.inputFluids); t.putInt("outputSockets", n.outputSockets); t.putIntArray("outputSlots", ints(n.outputSlots)); putStrings(t, "outputItems", n.outputItems); putStrings(t, "outputFluids", n.outputFluids); t.putByteArray("outputExport", booleans(n.outputExport)); ns.add(t); } tag.put("nodes", ns);
        ListTag es = new ListTag(); for (Edge e : edges) { CompoundTag t = new CompoundTag(); t.putInt("from",e.from()); t.putInt("output",e.output()); t.putInt("to",e.to()); t.putInt("input",e.input()); es.add(t); } tag.put("edges", es); return tag;
    }
    public static AssemblyGraph load(CompoundTag tag, HolderLookup.Provider provider) {
        AssemblyGraph graph = new AssemblyGraph(); graph.revision = tag.getInt("revision"); graph.enabled = tag.getBoolean("enabled");
        for (Tag raw : tag.getList("nodes", Tag.TAG_COMPOUND)) { CompoundTag t=(CompoundTag)raw; graph.nodes.add(new Node(t.getInt("id"),t.getLong("machine"),t.getString("block"),t.getFloat("x"),t.getFloat("y"))); Node n=graph.nodes.getLast(); n.recipe=t.getString("recipe"); n.status=t.getString("status"); n.inputSockets=t.getInt("inputSockets"); n.inputSlots=t.contains("inputSlots") ? t.getIntArray("inputSlots") : defaultInputs(n.inputSockets); n.inputItems=getStrings(t,"inputItems",n.inputSockets); n.inputFluids=getStrings(t,"inputFluids",n.inputSockets); n.outputSockets=t.contains("outputSockets") ? t.getInt("outputSockets") : 1; n.outputSlots=t.contains("outputSlots") ? t.getIntArray("outputSlots") : new int[]{1}; n.outputItems=getStrings(t,"outputItems",n.outputSockets); n.outputFluids=getStrings(t,"outputFluids",0); n.outputExport=getBooleans(t,"outputExport",n.outputSockets); }
        for (Tag raw : tag.getList("edges", Tag.TAG_COMPOUND)) { CompoundTag t=(CompoundTag)raw; graph.edges.add(new Edge(t.getInt("from"),t.getInt("output"),t.getInt("to"),t.getInt("input"))); }
        if (!graph.validate().isEmpty()) throw new IllegalArgumentException("Invalid assembly graph: " + graph.validate()); return graph;
    }
    private static int[] defaultInputs(int count) { int[] result = new int[count]; for (int i=0;i<count;i++) result[i]=i; return result; }
    private static String safe(String value) { return value == null ? "" : value; }
    private static int[] ints(int[] values) { return values == null ? new int[0] : values; }
    private static void putStrings(CompoundTag tag, String key, String[] values) { ListTag list=new ListTag(); if(values!=null) for(String value:values) list.add(StringTag.valueOf(safe(value))); tag.put(key,list); }
    private static String[] getStrings(CompoundTag tag,String key,int fallback) { if(!tag.contains(key)) return new String[fallback]; ListTag list=tag.getList(key,Tag.TAG_STRING); String[] result=new String[list.size()]; for(int i=0;i<result.length;i++) result[i]=list.getString(i); return result; }
    private static byte[] booleans(boolean[] values) { if(values==null) return new byte[0]; byte[] result=new byte[values.length]; for(int i=0;i<values.length;i++) result[i]=(byte)(values[i]?1:0); return result; }
    private static boolean[] getBooleans(CompoundTag tag,String key,int count) { boolean[] result=new boolean[count]; Arrays.fill(result,true); if(!tag.contains(key)) return result; byte[] values=tag.getByteArray(key); for(int i=0;i<Math.min(count,values.length);i++) result[i]=values[i]!=0; return result; }
}
