package net.crystalnexus.program;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Declarative palette metadata. Runtime behavior remains server-side in DepotProgramRunner. */
public final class DepotProgramBlocks {
    public enum Category { DEPOT, CRAFTING, PROCESSING, CONTROL, VARIABLES, OPERATORS, VALUES }
    public enum Shape { COMMAND, CONTROL, REPORTER, BOOLEAN, CAP }
    public record Input(String name, DepotProgram.ValueType type) {}
    public record Definition(String opcode, Category category, Shape shape, int color, String label,
            DepotProgram.ValueType output, List<Input> inputs, List<String> stacks, String tooltip) {}

    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();

    static {
        command("take", Category.DEPOT, 0xFF2C9C9C, "take [item] x [amount]", in("item", DepotProgram.ValueType.ITEM), in("amount", DepotProgram.ValueType.NUMBER));
        command("deposit_item", Category.DEPOT, 0xFF2C9C9C, "deposit [item] x [amount]", in("item", DepotProgram.ValueType.ITEM), in("amount", DepotProgram.ValueType.NUMBER));
        command("deposit_held", Category.DEPOT, 0xFF2C9C9C, "deposit held item");
        command("deposit_inventory", Category.DEPOT, 0xFF2C9C9C, "deposit inventory");
        command("craft", Category.CRAFTING, 0xFFFF8C32, "craft [item] x [amount]", in("item", DepotProgram.ValueType.ITEM), in("amount", DepotProgram.ValueType.NUMBER));
        command("smelt", Category.CRAFTING, 0xFFFF8C32, "smelt [item] x [amount]", in("item", DepotProgram.ValueType.ITEM), in("amount", DepotProgram.ValueType.NUMBER));
        command("process", Category.PROCESSING, 0xFF9B59D0, "process [item] x [amount]", in("item", DepotProgram.ValueType.ITEM), in("amount", DepotProgram.ValueType.NUMBER));
        command("cancel_job", Category.CRAFTING, 0xFFFF8C32, "cancel active job");
        command("remove_pattern", Category.PROCESSING, 0xFF9B59D0, "remove pattern [item]", in("item", DepotProgram.ValueType.ITEM));
        command("define_pattern", Category.PROCESSING, 0xFF9B59D0, "define machine pattern");
        command("machine_balance", Category.PROCESSING, 0xFF9B59D0, "machine balancing [enabled]", in("enabled", DepotProgram.ValueType.BOOLEAN));
        command("set_recipe", Category.CRAFTING, 0xFFFF8C32, "prefer recipe [recipe] for [item]", in("item", DepotProgram.ValueType.ITEM), in("recipe", DepotProgram.ValueType.TEXT));
        command("clear_recipe", Category.CRAFTING, 0xFFFF8C32, "clear recipe for [item]", in("item", DepotProgram.ValueType.ITEM));
        command("set_machine", Category.PROCESSING, 0xFF9B59D0, "prefer machine [machine] for [item]", in("item", DepotProgram.ValueType.ITEM), in("machine", DepotProgram.ValueType.BLOCK));
        command("clear_machine", Category.PROCESSING, 0xFF9B59D0, "clear machine for [item]", in("item", DepotProgram.ValueType.ITEM));
        define(new Definition("if", Category.CONTROL, Shape.CONTROL, 0xFFD89B26, "if [condition]", null,
                List.of(in("condition", DepotProgram.ValueType.BOOLEAN)), List.of("then", "else"), "Run one nested branch."));
        define(new Definition("repeat", Category.CONTROL, Shape.CONTROL, 0xFFD89B26, "repeat [count]", null,
                List.of(in("count", DepotProgram.ValueType.NUMBER)), List.of("body"), "Repeat the nested blocks."));
        define(new Definition("repeat_until", Category.CONTROL, Shape.CONTROL, 0xFFD89B26, "repeat until [condition]", null,
                List.of(in("condition", DepotProgram.ValueType.BOOLEAN)), List.of("body"), "Yield each tick until true."));
        command("wait", Category.CONTROL, 0xFFD89B26, "wait [seconds] seconds", in("seconds", DepotProgram.ValueType.NUMBER));
        command("wait_until", Category.CONTROL, 0xFFD89B26, "wait until [condition]", in("condition", DepotProgram.ValueType.BOOLEAN));
        define(new Definition("stop", Category.CONTROL, Shape.CAP, 0xFFD89B26, "stop program", null, List.of(), List.of(), "Finish this run."));
        command("set_variable", Category.VARIABLES, 0xFFD44D78, "set variable", in("value", DepotProgram.ValueType.NUMBER));
        command("change_variable", Category.VARIABLES, 0xFFD44D78, "change variable by [value]", in("value", DepotProgram.ValueType.NUMBER));

        reporter("number", Category.VALUES, 0xFF4E91D9, "number", DepotProgram.ValueType.NUMBER);
        reporter("boolean", Category.VALUES, 0xFF4E91D9, "boolean", DepotProgram.ValueType.BOOLEAN);
        reporter("text", Category.VALUES, 0xFF4E91D9, "text", DepotProgram.ValueType.TEXT);
        reporter("item", Category.VALUES, 0xFF4E91D9, "item id", DepotProgram.ValueType.ITEM);
        reporter("machine", Category.VALUES, 0xFF4E91D9, "machine id", DepotProgram.ValueType.BLOCK);
        reporter("variable_number", Category.VARIABLES, 0xFFD44D78, "number variable", DepotProgram.ValueType.NUMBER);
        reporter("variable_boolean", Category.VARIABLES, 0xFFD44D78, "boolean variable", DepotProgram.ValueType.BOOLEAN);
        reporter("variable_text", Category.VARIABLES, 0xFFD44D78, "text variable", DepotProgram.ValueType.TEXT);
        reporter("variable_item", Category.VARIABLES, 0xFFD44D78, "item variable", DepotProgram.ValueType.ITEM);
        reporter("variable_block", Category.VARIABLES, 0xFFD44D78, "machine variable", DepotProgram.ValueType.BLOCK);
        reporter("stored_count", Category.DEPOT, 0xFF2C9C9C, "stored [item]", DepotProgram.ValueType.NUMBER, in("item", DepotProgram.ValueType.ITEM));
        reporter("inventory_count", Category.DEPOT, 0xFF2C9C9C, "inventory [item]", DepotProgram.ValueType.NUMBER, in("item", DepotProgram.ValueType.ITEM));
        reporter("used", Category.DEPOT, 0xFF2C9C9C, "depot used", DepotProgram.ValueType.NUMBER);
        reporter("free", Category.DEPOT, 0xFF2C9C9C, "depot free", DepotProgram.ValueType.NUMBER);
        reporter("capacity", Category.DEPOT, 0xFF2C9C9C, "depot capacity", DepotProgram.ValueType.NUMBER);
        reporter("connected", Category.DEPOT, 0xFF2C9C9C, "network connected", DepotProgram.ValueType.BOOLEAN);
        reporter("job_active", Category.CRAFTING, 0xFFFF8C32, "job active", DepotProgram.ValueType.BOOLEAN);
        reporter("job_progress", Category.CRAFTING, 0xFFFF8C32, "job progress", DepotProgram.ValueType.NUMBER);
        for (String op : List.of("add", "subtract", "multiply", "divide", "modulo"))
            reporter(op, Category.OPERATORS, 0xFF52A84F, op, DepotProgram.ValueType.NUMBER,
                    in("left", DepotProgram.ValueType.NUMBER), in("right", DepotProgram.ValueType.NUMBER));
        for (String op : List.of("less", "greater", "equals"))
            reporter(op, Category.OPERATORS, 0xFF52A84F, op, DepotProgram.ValueType.BOOLEAN,
                    in("left", DepotProgram.ValueType.NUMBER), in("right", DepotProgram.ValueType.NUMBER));
        reporter("and", Category.OPERATORS, 0xFF52A84F, "and", DepotProgram.ValueType.BOOLEAN,
                in("left", DepotProgram.ValueType.BOOLEAN), in("right", DepotProgram.ValueType.BOOLEAN));
        reporter("or", Category.OPERATORS, 0xFF52A84F, "or", DepotProgram.ValueType.BOOLEAN,
                in("left", DepotProgram.ValueType.BOOLEAN), in("right", DepotProgram.ValueType.BOOLEAN));
        reporter("not", Category.OPERATORS, 0xFF52A84F, "not", DepotProgram.ValueType.BOOLEAN,
                in("value", DepotProgram.ValueType.BOOLEAN));
    }

    private DepotProgramBlocks() {}
    private static Input in(String name, DepotProgram.ValueType type) { return new Input(name, type); }
    private static void command(String opcode, Category category, int color, String label, Input... inputs) {
        define(new Definition(opcode, category, Shape.COMMAND, color, label, null, List.of(inputs), List.of(), label));
    }
    private static void reporter(String opcode, Category category, int color, String label,
            DepotProgram.ValueType output, Input... inputs) {
        define(new Definition(opcode, category, output == DepotProgram.ValueType.BOOLEAN ? Shape.BOOLEAN : Shape.REPORTER,
                color, label, output, List.of(inputs), List.of(), label));
    }
    private static void define(Definition definition) { DEFINITIONS.put(definition.opcode(), definition); }
    public static Definition get(String opcode) { return DEFINITIONS.get(opcode); }
    public static List<Definition> all() { return List.copyOf(DEFINITIONS.values()); }
}
