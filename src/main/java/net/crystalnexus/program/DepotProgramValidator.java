package net.crystalnexus.program;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DepotProgramValidator {
    public record Problem(UUID nodeId, String message) {}
    private DepotProgramValidator() {}

    public static List<Problem> validate(DepotProgram program) {
        List<Problem> problems = new ArrayList<>();
        if (program.name() == null || program.name().isBlank() || program.name().length() > 32)
            problems.add(new Problem(null, "Program name must be 1-32 characters."));
        if (program.variables().size() > DepotProgram.MAX_VARIABLES)
            problems.add(new Problem(null, "Too many variables."));
        Map<String, DepotProgram.ValueType> variables = new HashMap<>();
        for (DepotProgram.Variable variable : program.variables()) {
            String key = variable.name().trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty() || key.length() > 32) problems.add(new Problem(null, "Variable names must be 1-32 characters."));
            else if (variables.putIfAbsent(key, variable.type()) != null) problems.add(new Problem(null, "Duplicate variable: " + variable.name()));
            if (variable.initial().type() != variable.type()) problems.add(new Problem(null, "Wrong default type for " + variable.name()));
        }
        Counter counter = new Counter();
        Set<UUID> ids = new HashSet<>();
        validateNodes(program.body(), 1, variables, counter, ids, problems);
        if (counter.nodes > DepotProgram.MAX_NODES) problems.add(new Problem(null, "Program exceeds " + DepotProgram.MAX_NODES + " blocks."));
        return List.copyOf(problems);
    }

    private static void validateNodes(List<DepotProgram.Node> nodes, int depth,
            Map<String, DepotProgram.ValueType> variables, Counter counter, Set<UUID> ids, List<Problem> problems) {
        if (depth > DepotProgram.MAX_DEPTH) {
            problems.add(new Problem(null, "Program nesting exceeds " + DepotProgram.MAX_DEPTH + "."));
            return;
        }
        for (DepotProgram.Node node : nodes) {
            counter.nodes++;
            if (!ids.add(node.id())) problems.add(new Problem(node.id(), "Duplicate block id."));
            DepotProgramBlocks.Definition definition = DepotProgramBlocks.get(node.opcode());
            if (definition == null) {
                problems.add(new Problem(node.id(), "Unknown block: " + node.opcode()));
                continue;
            }
            for (DepotProgramBlocks.Input input : definition.inputs()) {
                DepotProgram.Node expression = node.inputs().get(input.name());
                if (expression == null) problems.add(new Problem(node.id(), "Missing input: " + input.name()));
                else {
                    DepotProgram.ValueType actual = expressionType(expression, variables, counter, ids, problems, depth + 1);
                    DepotProgram.ValueType expected = input.type();
                    if (node.opcode().equals("set_variable") && input.name().equals("value")) {
                        expected = variables.get(node.fields().getOrDefault("variable", "").toLowerCase(Locale.ROOT));
                    }
                    if (actual != null && expected != null && actual != expected) problems.add(new Problem(expression.id(),
                            "Expected " + expected.name().toLowerCase(Locale.ROOT) + " for " + input.name() + "."));
                }
            }
            if (node.opcode().equals("set_variable") || node.opcode().equals("change_variable")
                    || node.opcode().equals("variable") || node.opcode().startsWith("variable_")) {
                String variable = node.fields().getOrDefault("variable", "").toLowerCase(Locale.ROOT);
                if (!variables.containsKey(variable)) problems.add(new Problem(node.id(), "Unknown variable: " + variable));
                else if (node.opcode().equals("change_variable") && variables.get(variable) != DepotProgram.ValueType.NUMBER)
                    problems.add(new Problem(node.id(), "Only number variables can be changed by an amount."));
            }
            validateConfiguredFields(node, problems);
            for (String stack : definition.stacks()) {
                if (!node.stacks().containsKey(stack)) problems.add(new Problem(node.id(), "Missing nested slot: " + stack));
            }
            node.stacks().values().forEach(stack -> validateNodes(stack, depth + 1, variables, counter, ids, problems));
        }
    }

    private static void validateConfiguredFields(DepotProgram.Node node, List<Problem> problems) {
        if (node.opcode().equals("define_pattern")) {
            String outputText = node.fields().getOrDefault("output", "");
            String machineText = node.fields().getOrDefault("machine", "");
            ResourceLocation output = ResourceLocation.tryParse(outputText);
            ResourceLocation machine = ResourceLocation.tryParse(machineText);
            if (output == null || BuiltInRegistries.ITEM.get(output) == Items.AIR)
                problems.add(new Problem(node.id(), "Unknown pattern output: " + outputText));
            if (machine == null || BuiltInRegistries.BLOCK.get(machine) == Blocks.AIR)
                problems.add(new Problem(node.id(), "Unknown pattern machine: " + machineText));
            try {
                long amount = Long.parseLong(node.fields().getOrDefault("amount", "0"));
                if (amount < 1 || amount > 4096) throw new NumberFormatException();
            } catch (NumberFormatException ignored) {
                problems.add(new Problem(node.id(), "Pattern output amount must be 1-4096."));
            }
            validateCountList(node, "inputs", true, problems);
            validateCountList(node, "outputs", false, problems);
        }
        for (Map.Entry<String, DepotProgram.Node> input : node.inputs().entrySet()) {
            if (!(input.getKey().equals("amount") || input.getKey().equals("count") || input.getKey().equals("seconds"))) continue;
            DepotProgram.Node value = input.getValue();
            if (!value.opcode().equals("literal") || !value.fields().getOrDefault("type", "").equals("NUMBER")) continue;
            try {
                long number = Long.parseLong(value.fields().getOrDefault("number", "0"));
                long maximum = input.getKey().equals("seconds") ? 86_400 : 4096;
                if (number < 0 || input.getKey().equals("amount") && number == 0 || number > maximum)
                    problems.add(new Problem(value.id(), input.getKey() + " must be "
                            + (input.getKey().equals("amount") ? "1" : "0") + "-" + maximum + "."));
            } catch (NumberFormatException ignored) {
                // The literal validator reports malformed numbers.
            }
        }
    }

    private static void validateCountList(DepotProgram.Node node, String field, boolean required,
            List<Problem> problems) {
        String raw = node.fields().getOrDefault(field, "").trim();
        if (raw.isEmpty()) {
            if (required) problems.add(new Problem(node.id(), "Pattern inputs are required."));
            return;
        }
        for (String pair : raw.split("[;,]")) {
            String[] parts = pair.trim().split("=", 2);
            ResourceLocation id = parts.length == 2 ? ResourceLocation.tryParse(parts[0].trim()) : null;
            try {
                long count = parts.length == 2 ? Long.parseLong(parts[1].trim()) : 0;
                if (id == null || BuiltInRegistries.ITEM.get(id) == Items.AIR || count < 1 || count > 4096)
                    throw new NumberFormatException();
            } catch (NumberFormatException ignored) {
                problems.add(new Problem(node.id(), "Invalid pattern " + field + " entry: " + pair.trim()));
            }
        }
    }

    private static DepotProgram.ValueType expressionType(DepotProgram.Node node,
            Map<String, DepotProgram.ValueType> variables, Counter counter, Set<UUID> ids,
            List<Problem> problems, int depth) {
        counter.nodes++;
        if (!ids.add(node.id())) problems.add(new Problem(node.id(), "Duplicate block id."));
        if (depth > DepotProgram.MAX_DEPTH) return null;
        if (node.opcode().equals("literal")) {
            DepotProgram.ValueType type;
            try { type = DepotProgram.ValueType.valueOf(node.fields().getOrDefault("type", "TEXT")); }
            catch (IllegalArgumentException ignored) { problems.add(new Problem(node.id(), "Invalid literal type.")); return null; }
            String text = node.fields().getOrDefault("text", "");
            if (text.length() > DepotProgram.MAX_TEXT) problems.add(new Problem(node.id(), "Literal text is too long."));
            if (type == DepotProgram.ValueType.ITEM) {
                ResourceLocation id = ResourceLocation.tryParse(text);
                if (id == null || BuiltInRegistries.ITEM.get(id) == Items.AIR) problems.add(new Problem(node.id(), "Unknown item: " + text));
            } else if (type == DepotProgram.ValueType.BLOCK) {
                ResourceLocation id = ResourceLocation.tryParse(text);
                if (id == null || BuiltInRegistries.BLOCK.get(id) == Blocks.AIR) problems.add(new Problem(node.id(), "Unknown block: " + text));
            } else if (type == DepotProgram.ValueType.NUMBER) {
                try { Long.parseLong(node.fields().getOrDefault("number", "0")); }
                catch (NumberFormatException ignored) { problems.add(new Problem(node.id(), "Invalid number.")); }
            }
            return type;
        }
        DepotProgram.ValueType directLiteral = switch (node.opcode()) {
            case "number" -> DepotProgram.ValueType.NUMBER;
            case "boolean" -> DepotProgram.ValueType.BOOLEAN;
            case "text" -> DepotProgram.ValueType.TEXT;
            case "item" -> DepotProgram.ValueType.ITEM;
            case "machine" -> DepotProgram.ValueType.BLOCK;
            default -> null;
        };
        if (directLiteral != null) {
            validateDirectLiteral(node, directLiteral, problems);
            return directLiteral;
        }
        if (node.opcode().equals("variable") || node.opcode().startsWith("variable_")) {
            String name = node.fields().getOrDefault("variable", "").toLowerCase(Locale.ROOT);
            DepotProgram.ValueType type = variables.get(name);
            if (type == null) problems.add(new Problem(node.id(), "Unknown variable: " + name));
            DepotProgramBlocks.Definition definition = DepotProgramBlocks.get(node.opcode());
            if (type != null && definition != null && definition.output() != null && type != definition.output())
                problems.add(new Problem(node.id(), "Variable reporter type does not match " + name + "."));
            return type;
        }
        DepotProgramBlocks.Definition definition = DepotProgramBlocks.get(node.opcode());
        if (definition == null || definition.output() == null) {
            problems.add(new Problem(node.id(), "This block cannot be used as a value."));
            return null;
        }
        for (DepotProgramBlocks.Input input : definition.inputs()) {
            DepotProgram.Node child = node.inputs().get(input.name());
            if (child == null) problems.add(new Problem(node.id(), "Missing input: " + input.name()));
            else {
                DepotProgram.ValueType actual = expressionType(child, variables, counter, ids, problems, depth + 1);
                if (actual != null && actual != input.type()) problems.add(new Problem(child.id(), "Wrong input type."));
            }
        }
        return definition.output();
    }

    private static void validateDirectLiteral(DepotProgram.Node node, DepotProgram.ValueType type, List<Problem> problems) {
        String text = node.fields().getOrDefault(type == DepotProgram.ValueType.NUMBER ? "number"
                : type == DepotProgram.ValueType.BOOLEAN ? "bool" : "text", "");
        if (text.length() > DepotProgram.MAX_TEXT) problems.add(new Problem(node.id(), "Literal text is too long."));
        if (type == DepotProgram.ValueType.NUMBER) {
            try { Long.parseLong(text); } catch (NumberFormatException ignored) { problems.add(new Problem(node.id(), "Invalid number.")); }
        } else if (type == DepotProgram.ValueType.BOOLEAN && !text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) {
            problems.add(new Problem(node.id(), "Boolean values must be true or false."));
        } else if (type == DepotProgram.ValueType.ITEM) {
            ResourceLocation id = ResourceLocation.tryParse(text);
            if (id == null || BuiltInRegistries.ITEM.get(id) == Items.AIR) problems.add(new Problem(node.id(), "Unknown item: " + text));
        } else if (type == DepotProgram.ValueType.BLOCK) {
            ResourceLocation id = ResourceLocation.tryParse(text);
            if (id == null || BuiltInRegistries.BLOCK.get(id) == Blocks.AIR) problems.add(new Problem(node.id(), "Unknown block: " + text));
        }
    }

    private static final class Counter { int nodes; }
}
