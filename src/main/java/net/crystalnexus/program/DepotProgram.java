package net.crystalnexus.program;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-owned, versioned representation shared by persistence, networking and the editor. */
public record DepotProgram(UUID id, String name, int revision, List<Variable> variables, List<Node> body) {
    public static final int SCHEMA = 1;
    public static final int MAX_PROGRAMS = 32;
    public static final int MAX_VARIABLES = 32;
    public static final int MAX_NODES = 256;
    public static final int MAX_DEPTH = 16;
    public static final int MAX_TEXT = 128;

    public DepotProgram {
        variables = List.copyOf(variables);
        body = List.copyOf(body);
    }

    public enum ValueType { NUMBER, BOOLEAN, TEXT, ITEM, BLOCK }

    public record Value(ValueType type, long number, boolean bool, String text) {
        public Value { text = text == null ? "" : text; }
        public static Value number(long value) { return new Value(ValueType.NUMBER, value, false, ""); }
        public static Value bool(boolean value) { return new Value(ValueType.BOOLEAN, 0, value, ""); }
        public static Value text(ValueType type, String value) { return new Value(type, 0, false, value); }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", type.name());
            tag.putLong("number", number);
            tag.putBoolean("bool", bool);
            tag.putString("text", text);
            return tag;
        }

        static Value load(CompoundTag tag) {
            ValueType type;
            try { type = ValueType.valueOf(tag.getString("type")); }
            catch (IllegalArgumentException ignored) { type = ValueType.TEXT; }
            return new Value(type, tag.getLong("number"), tag.getBoolean("bool"), tag.getString("text"));
        }
    }

    public record Variable(String name, ValueType type, Value initial) {
        public Variable { name = name == null ? "" : name; }
    }

    /** Inputs are expression nodes; fields hold non-expression configuration such as variable names. */
    public record Node(UUID id, String opcode, Map<String, String> fields,
            Map<String, Node> inputs, Map<String, List<Node>> stacks) {
        public Node {
            fields = Map.copyOf(fields);
            inputs = Map.copyOf(inputs);
            Map<String, List<Node>> copied = new LinkedHashMap<>();
            stacks.forEach((key, value) -> copied.put(key, List.copyOf(value)));
            stacks = Map.copyOf(copied);
        }

        public static Node statement(String opcode) {
            return new Node(UUID.randomUUID(), opcode, Map.of(), Map.of(), Map.of());
        }

        public static Node literal(Value value) {
            return new Node(UUID.randomUUID(), "literal", Map.of("type", value.type().name(),
                    "number", Long.toString(value.number()), "bool", Boolean.toString(value.bool()),
                    "text", value.text()), Map.of(), Map.of());
        }
    }

    public static DepotProgram empty(String name) {
        return new DepotProgram(UUID.randomUUID(), name, 0, List.of(), List.of());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", SCHEMA);
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putInt("revision", revision);
        ListTag vars = new ListTag();
        for (Variable variable : variables) {
            CompoundTag saved = new CompoundTag();
            saved.putString("name", variable.name());
            saved.putString("type", variable.type().name());
            saved.put("initial", variable.initial().save());
            vars.add(saved);
        }
        tag.put("variables", vars);
        tag.put("body", saveNodes(body));
        return tag;
    }

    public static DepotProgram load(CompoundTag tag) {
        if (tag.getInt("schema") != SCHEMA) throw new IllegalArgumentException("Unsupported depot program schema.");
        UUID id = tag.hasUUID("id") ? tag.getUUID("id") : UUID.randomUUID();
        List<Variable> variables = new ArrayList<>();
        ListTag vars = tag.getList("variables", Tag.TAG_COMPOUND);
        if (vars.size() > MAX_VARIABLES) throw new IllegalArgumentException("Too many depot program variables.");
        for (int i = 0; i < vars.size(); i++) {
            CompoundTag saved = vars.getCompound(i);
            try {
                variables.add(new Variable(saved.getString("name"), ValueType.valueOf(saved.getString("type")),
                        Value.load(saved.getCompound("initial"))));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return new DepotProgram(id, tag.getString("name"), tag.getInt("revision"), variables,
                loadNodes(tag.getList("body", Tag.TAG_COMPOUND), 1, new Counter()));
    }

    private static ListTag saveNodes(List<Node> nodes) {
        ListTag list = new ListTag();
        for (Node node : nodes) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", node.id());
            tag.putString("opcode", node.opcode());
            CompoundTag fields = new CompoundTag();
            node.fields().forEach(fields::putString);
            tag.put("fields", fields);
            CompoundTag inputs = new CompoundTag();
            node.inputs().forEach((key, value) -> inputs.put(key, saveNodes(List.of(value)).getCompound(0)));
            tag.put("inputs", inputs);
            CompoundTag stacks = new CompoundTag();
            node.stacks().forEach((key, value) -> stacks.put(key, saveNodes(value)));
            tag.put("stacks", stacks);
            list.add(tag);
        }
        return list;
    }

    private static List<Node> loadNodes(ListTag list, int depth, Counter counter) {
        if (depth > MAX_DEPTH || counter.nodes + list.size() > MAX_NODES)
            throw new IllegalArgumentException("Depot program exceeds structural limits.");
        List<Node> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            counter.nodes++;
            CompoundTag tag = list.getCompound(i);
            Map<String, String> fields = new LinkedHashMap<>();
            CompoundTag savedFields = tag.getCompound("fields");
            savedFields.getAllKeys().forEach(key -> fields.put(key, savedFields.getString(key)));
            Map<String, Node> inputs = new LinkedHashMap<>();
            CompoundTag savedInputs = tag.getCompound("inputs");
            savedInputs.getAllKeys().forEach(key -> {
                ListTag one = new ListTag();
                one.add(savedInputs.getCompound(key));
                List<Node> loaded = loadNodes(one, depth + 1, counter);
                if (!loaded.isEmpty()) inputs.put(key, loaded.getFirst());
            });
            Map<String, List<Node>> stacks = new LinkedHashMap<>();
            CompoundTag savedStacks = tag.getCompound("stacks");
            savedStacks.getAllKeys().forEach(key -> stacks.put(key,
                    loadNodes(savedStacks.getList(key, Tag.TAG_COMPOUND), depth + 1, counter)));
            result.add(new Node(tag.hasUUID("id") ? tag.getUUID("id") : UUID.randomUUID(),
                    tag.getString("opcode"), fields, inputs, stacks));
        }
        return List.copyOf(result);
    }

    private static final class Counter { int nodes; }
}
