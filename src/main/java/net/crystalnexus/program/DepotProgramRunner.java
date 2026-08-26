package net.crystalnexus.program;

import net.crystalnexus.data.DepotSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class DepotProgramRunner {
    public static final int PER_TICK = 64;
    public static final long MAX_EXECUTED = 100_000;
    public static final long MAX_LOOPS = 10_000;
    private DepotProgramRunner() {}

    public static void tick(MinecraftServer server, UUID owner, DepotSavedData depot) {
        DepotProgramRun run = depot.getProgramRun();
        if (run == null || !run.active()) { DepotProgramIndex.get(server).remove(owner); return; }
        DepotProgram program = depot.getProgram(run.programId());
        if (program == null || program.revision() != run.revision()) {
            fail(run, null, "The running program revision no longer exists.");
            depot.setProgramRun(run);
            DepotProgramIndex.get(server).remove(owner);
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        long now = server.overworld().getGameTime();

        if (run.status() == DepotProgramRun.Status.WAITING_JOB) {
            DepotSavedData.CraftingJob job = depot.getCraftingJob();
            if (job != null && job.id() == run.waitingJobId()) return;
            if (depot.getLastCompletedJobId() == run.waitingJobId()) {
                run.waitingJobId(0); run.status(DepotProgramRun.Status.RUNNING); run.message("Crafting complete");
            } else {
                fail(run, run.currentNode(), depot.getLastCancelledJobId() == run.waitingJobId()
                        ? "The crafting job was cancelled." : "The crafting job ended unexpectedly.");
            }
        }
        if (run.status() == DepotProgramRun.Status.WAITING_TIME) {
            if (now < run.wakeTick()) return;
            run.status(DepotProgramRun.Status.RUNNING);
        }
        if (run.status() == DepotProgramRun.Status.WAITING_NETWORK) {
            if (!DepotSavedData.hasPoweredController(server.overworld(), owner)) return;
            run.status(DepotProgramRun.Status.RUNNING);
        }
        if (run.status() == DepotProgramRun.Status.WAITING_RECIPE_SYNC) {
            if (player == null) return;
            run.status(DepotProgramRun.Status.RUNNING);
        }
        if (!run.active()) { depot.setProgramRun(run); DepotProgramIndex.get(server).remove(owner); return; }

        Map<UUID, DepotProgram.Node> nodes = index(program.body());
        for (int budget = 0; budget < PER_TICK && run.active(); budget++) {
            if (run.executed() >= MAX_EXECUTED) { fail(run, run.currentNode(), "Instruction limit exceeded."); break; }
            DepotProgramRun.Instruction instruction = run.instructions().pollFirst();
            if (instruction == null) {
                run.status(DepotProgramRun.Status.COMPLETED); run.message("Program complete"); break;
            }
            DepotProgram.Node node = nodes.get(instruction.nodeId());
            if (node == null) { fail(run, instruction.nodeId(), "A referenced block is missing."); break; }
            run.currentNode(node.id());
            try {
                if (instruction.kind().equals("repeat")) {
                    if (instruction.remaining() > 0) {
                        if (run.loops() >= MAX_LOOPS) throw new ProgramError("Loop limit exceeded.");
                        run.incrementLoops();
                        run.instructions().addFirst(new DepotProgramRun.Instruction("repeat", node.id(), instruction.remaining() - 1));
                        DepotProgramRun.prepend(run.instructions(), node.stacks().getOrDefault("body", List.of()));
                    }
                    continue;
                }
                if (instruction.kind().equals("repeat_until")) {
                    if (!bool(eval(node.inputs().get("condition"), run, depot, player, server, owner))) {
                        if (run.loops() >= MAX_LOOPS) throw new ProgramError("Loop limit exceeded.");
                        run.incrementLoops();
                        run.instructions().addFirst(instruction);
                        DepotProgramRun.prepend(run.instructions(), node.stacks().getOrDefault("body", List.of()));
                        depot.setProgramRun(run);
                        return;
                    }
                    continue;
                }
                run.incrementExecuted();
                if (!execute(node, run, depot, player, server, owner)) break;
            } catch (ProgramError error) {
                fail(run, node.id(), error.getMessage());
            } catch (RuntimeException error) {
                fail(run, node.id(), "Block failed safely: " + error.getClass().getSimpleName());
            }
        }
        depot.setProgramRun(run);
        if (!run.active()) DepotProgramIndex.get(server).remove(owner);
    }

    private static boolean execute(DepotProgram.Node node, DepotProgramRun run, DepotSavedData depot,
            ServerPlayer player, MinecraftServer server, UUID owner) {
        String op = node.opcode();
        if (op.equals("if")) {
            DepotProgramRun.prepend(run.instructions(), node.stacks().getOrDefault(
                    bool(eval(node.inputs().get("condition"), run, depot, player, server, owner)) ? "then" : "else", List.of()));
            return true;
        }
        if (op.equals("repeat")) {
            long count = number(eval(node.inputs().get("count"), run, depot, player, server, owner));
            if (count < 0 || count > MAX_LOOPS) throw new ProgramError("Repeat count must be 0-" + MAX_LOOPS + ".");
            run.instructions().addFirst(new DepotProgramRun.Instruction("repeat", node.id(), count));
            return true;
        }
        if (op.equals("repeat_until")) {
            run.instructions().addFirst(new DepotProgramRun.Instruction("repeat_until", node.id(), 0));
            return true;
        }
        if (op.equals("wait")) {
            long seconds = number(eval(node.inputs().get("seconds"), run, depot, player, server, owner));
            if (seconds < 0 || seconds > 86_400) throw new ProgramError("Wait must be 0-86400 seconds.");
            run.wakeTick(server.overworld().getGameTime() + seconds * 20);
            run.status(DepotProgramRun.Status.WAITING_TIME); run.message("Waiting " + seconds + " seconds");
            return false;
        }
        if (op.equals("wait_until")) {
            if (!bool(eval(node.inputs().get("condition"), run, depot, player, server, owner))) {
                run.instructions().addFirst(new DepotProgramRun.Instruction("node", node.id(), 0));
                run.status(DepotProgramRun.Status.WAITING_TIME); run.wakeTick(server.overworld().getGameTime() + 1);
                run.message("Waiting for condition"); return false;
            }
            return true;
        }
        if (op.equals("stop")) { run.instructions().clear(); run.status(DepotProgramRun.Status.COMPLETED); run.message("Stopped"); return false; }
        if (op.equals("set_variable") || op.equals("change_variable")) {
            String name = node.fields().getOrDefault("variable", "").toLowerCase(Locale.ROOT);
            DepotProgram.Value value = eval(node.inputs().get("value"), run, depot, player, server, owner);
            if (op.equals("change_variable")) value = DepotProgram.Value.number(number(run.variables().get(name)) + number(value));
            if (!run.variables().containsKey(name)) throw new ProgramError("Unknown variable: " + name);
            run.variables().put(name, value);
            return true;
        }
        if (requiresNetwork(op) && !DepotSavedData.hasPoweredController(server.overworld(), owner)) {
            run.instructions().addFirst(new DepotProgramRun.Instruction("node", node.id(), 0));
            run.status(DepotProgramRun.Status.WAITING_NETWORK); run.message("Waiting for powered depot network"); return false;
        }
        DepotOperations.Result result = action(node, run, depot, player, server, owner);
        if (result.kind() == DepotOperations.Kind.ERROR) {
            if (player == null && (op.equals("craft") || op.equals("smelt") || op.equals("process"))) {
                run.instructions().addFirst(new DepotProgramRun.Instruction("node", node.id(), 0));
                run.status(DepotProgramRun.Status.WAITING_RECIPE_SYNC); run.message(result.message()); return false;
            }
            throw new ProgramError(result.message());
        }
        run.message(result.message());
        if (result.kind() == DepotOperations.Kind.JOB) {
            run.waitingJobId(result.jobId()); run.status(DepotProgramRun.Status.WAITING_JOB); return false;
        }
        return true;
    }

    private static DepotOperations.Result action(DepotProgram.Node node, DepotProgramRun run, DepotSavedData depot,
            ServerPlayer player, MinecraftServer server, UUID owner) {
        String op = node.opcode();
        ResourceLocation item = node.inputs().containsKey("item") ? id(eval(node.inputs().get("item"), run, depot, player, server, owner)) : null;
        int amount = node.inputs().containsKey("amount") ? quantity(eval(node.inputs().get("amount"), run, depot, player, server, owner)) : 0;
        return switch (op) {
            case "take" -> DepotOperations.take(player, depot, item, amount);
            case "deposit_item" -> DepotOperations.depositItem(player, depot, item, amount);
            case "deposit_held" -> DepotOperations.depositHeld(player, depot);
            case "deposit_inventory" -> DepotOperations.depositInventory(player, depot);
            case "craft", "smelt", "process" -> DepotOperations.craft(player, depot, item, amount, op);
            case "cancel_job" -> DepotOperations.cancel(depot);
            case "remove_pattern" -> depot.removeProcessingPattern(item) ? DepotOperations.Result.ok("Pattern removed.") : DepotOperations.Result.warning("No pattern existed.");
            case "machine_balance" -> { depot.setMachineLoadBalancing(bool(eval(node.inputs().get("enabled"), run, depot, player, server, owner))); yield DepotOperations.Result.ok("Machine balancing updated."); }
            case "set_recipe" -> { ResourceLocation recipe = ResourceLocation.tryParse(text(eval(node.inputs().get("recipe"), run, depot, player, server, owner))); if (recipe == null) yield DepotOperations.Result.error("Invalid recipe id."); depot.setPreferredRecipe(item, recipe); yield DepotOperations.Result.ok("Recipe preference saved."); }
            case "clear_recipe" -> { depot.clearPreferredRecipe(item); yield DepotOperations.Result.ok("Recipe preference cleared."); }
            case "set_machine" -> { ResourceLocation machine = id(eval(node.inputs().get("machine"), run, depot, player, server, owner)); depot.setPreferredMachine(item, machine); yield DepotOperations.Result.ok("Machine preference saved."); }
            case "clear_machine" -> { depot.clearPreferredMachine(item); yield DepotOperations.Result.ok("Machine preference cleared."); }
            case "define_pattern" -> definePattern(node, depot);
            default -> DepotOperations.Result.error("Unsupported action: " + op);
        };
    }

    private static DepotOperations.Result definePattern(DepotProgram.Node node, DepotSavedData depot) {
        ResourceLocation output = ResourceLocation.tryParse(node.fields().getOrDefault("output", ""));
        ResourceLocation machine = ResourceLocation.tryParse(node.fields().getOrDefault("machine", ""));
        long amount;
        try { amount = Long.parseLong(node.fields().getOrDefault("amount", "1")); }
        catch (NumberFormatException ignored) { return DepotOperations.Result.error("Invalid pattern output amount."); }
        Map<ResourceLocation, Long> inputs = parseCounts(node.fields().getOrDefault("inputs", ""));
        Map<ResourceLocation, Long> outputs = parseCounts(node.fields().getOrDefault("outputs", ""));
        if (output == null || amount < 1 || amount > 4096 || inputs.isEmpty()) return DepotOperations.Result.error("Invalid processing pattern.");
        if (outputs.isEmpty()) outputs = Map.of(output, amount);
        depot.setProcessingPattern(output, amount, inputs, outputs, machine == null ? List.of() : List.of(machine));
        return DepotOperations.Result.ok("Processing pattern saved.");
    }

    private static Map<ResourceLocation, Long> parseCounts(String value) {
        Map<ResourceLocation, Long> result = new java.util.LinkedHashMap<>();
        if (value.isBlank()) return result;
        for (String pair : value.split("[;,]")) {
            String[] parts = pair.trim().split("=", 2);
            ResourceLocation id = parts.length == 2 ? ResourceLocation.tryParse(parts[0].trim()) : null;
            try { if (id != null) { long count = Long.parseLong(parts[1].trim()); if (count > 0 && count <= 4096) result.merge(id, count, Long::sum); } }
            catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private static DepotProgram.Value eval(DepotProgram.Node node, DepotProgramRun run, DepotSavedData depot,
            ServerPlayer player, MinecraftServer server, UUID owner) {
        if (node == null) throw new ProgramError("Missing value block.");
        String op = node.opcode();
        if (op.equals("literal")) {
            DepotProgram.ValueType type;
            try { type = DepotProgram.ValueType.valueOf(node.fields().getOrDefault("type", "TEXT")); }
            catch (IllegalArgumentException ignored) { throw new ProgramError("Invalid literal type."); }
            return switch (type) {
                case NUMBER -> { try { yield DepotProgram.Value.number(Long.parseLong(node.fields().getOrDefault("number", "0"))); } catch (NumberFormatException ignored) { throw new ProgramError("Invalid number."); } }
                case BOOLEAN -> DepotProgram.Value.bool(Boolean.parseBoolean(node.fields().getOrDefault("bool", "false")));
                default -> DepotProgram.Value.text(type, node.fields().getOrDefault("text", ""));
            };
        }
        if (op.equals("number")) {
            try { return DepotProgram.Value.number(Long.parseLong(node.fields().getOrDefault("number", "0"))); }
            catch (NumberFormatException ignored) { throw new ProgramError("Invalid number."); }
        }
        if (op.equals("boolean")) return DepotProgram.Value.bool(Boolean.parseBoolean(node.fields().getOrDefault("bool", "false")));
        if (op.equals("text")) return DepotProgram.Value.text(DepotProgram.ValueType.TEXT, node.fields().getOrDefault("text", ""));
        if (op.equals("item")) return DepotProgram.Value.text(DepotProgram.ValueType.ITEM, node.fields().getOrDefault("text", ""));
        if (op.equals("machine")) return DepotProgram.Value.text(DepotProgram.ValueType.BLOCK, node.fields().getOrDefault("text", ""));
        if (op.equals("variable") || op.startsWith("variable_")) {
            DepotProgram.Value value = run.variables().get(node.fields().getOrDefault("variable", "").toLowerCase(Locale.ROOT));
            if (value == null) throw new ProgramError("Unknown variable.");
            return value;
        }
        if (op.equals("stored_count")) return DepotProgram.Value.number(depot.getCount(id(eval(node.inputs().get("item"), run, depot, player, server, owner))));
        if (op.equals("inventory_count")) {
            if (player == null) throw new ProgramError("Player inventory is unavailable while offline.");
            Item target = DepotOperations.item(id(eval(node.inputs().get("item"), run, depot, player, server, owner)));
            return DepotProgram.Value.number(target == null ? 0 : player.getInventory().items.stream().filter(stack -> stack.is(target)).mapToLong(ItemStack::getCount).sum());
        }
        if (op.equals("used")) return DepotProgram.Value.number(depot.getUsed());
        if (op.equals("free")) return DepotProgram.Value.number(depot.getFree());
        if (op.equals("capacity")) return DepotProgram.Value.number(depot.getCapacity());
        if (op.equals("connected")) return DepotProgram.Value.bool(DepotSavedData.hasPoweredController(server.overworld(), owner));
        if (op.equals("job_active")) return DepotProgram.Value.bool(depot.getCraftingJob() != null);
        if (op.equals("job_progress")) {
            DepotSavedData.CraftingJob job = depot.getCraftingJob();
            return DepotProgram.Value.number(job == null ? 0 : Math.min(100, (job.totalWork() - job.remainingWork()) * 100 / Math.max(1, job.totalWork())));
        }
        DepotProgram.Value left = node.inputs().containsKey("left") ? eval(node.inputs().get("left"), run, depot, player, server, owner) : null;
        DepotProgram.Value right = node.inputs().containsKey("right") ? eval(node.inputs().get("right"), run, depot, player, server, owner) : null;
        return switch (op) {
            case "add" -> DepotProgram.Value.number(saturated(number(left), number(right), '+'));
            case "subtract" -> DepotProgram.Value.number(saturated(number(left), number(right), '-'));
            case "multiply" -> DepotProgram.Value.number(saturated(number(left), number(right), '*'));
            case "divide" -> { long divisor = number(right); if (divisor == 0) throw new ProgramError("Division by zero."); yield DepotProgram.Value.number(number(left) / divisor); }
            case "modulo" -> { long divisor = number(right); if (divisor == 0) throw new ProgramError("Division by zero."); yield DepotProgram.Value.number(number(left) % divisor); }
            case "less" -> DepotProgram.Value.bool(number(left) < number(right));
            case "greater" -> DepotProgram.Value.bool(number(left) > number(right));
            case "equals" -> DepotProgram.Value.bool(number(left) == number(right));
            case "and" -> DepotProgram.Value.bool(bool(left) && bool(right));
            case "or" -> DepotProgram.Value.bool(bool(left) || bool(right));
            case "not" -> DepotProgram.Value.bool(!bool(eval(node.inputs().get("value"), run, depot, player, server, owner)));
            default -> throw new ProgramError("Unsupported value block: " + op);
        };
    }

    private static long saturated(long left, long right, char op) {
        try { return op == '+' ? Math.addExact(left, right) : op == '-' ? Math.subtractExact(left, right) : Math.multiplyExact(left, right); }
        catch (ArithmeticException ignored) { return op == '-' && left < 0 || op != '-' && left < 0 == right > 0 ? Long.MIN_VALUE : Long.MAX_VALUE; }
    }
    private static long number(DepotProgram.Value value) { if (value == null || value.type() != DepotProgram.ValueType.NUMBER) throw new ProgramError("Expected number."); return value.number(); }
    private static boolean bool(DepotProgram.Value value) { if (value == null || value.type() != DepotProgram.ValueType.BOOLEAN) throw new ProgramError("Expected boolean."); return value.bool(); }
    private static String text(DepotProgram.Value value) { if (value == null || value.type() != DepotProgram.ValueType.TEXT) throw new ProgramError("Expected text."); return value.text(); }
    private static ResourceLocation id(DepotProgram.Value value) { if (value == null || value.type() != DepotProgram.ValueType.ITEM && value.type() != DepotProgram.ValueType.BLOCK) throw new ProgramError("Expected registry id."); ResourceLocation id = ResourceLocation.tryParse(value.text()); if (id == null) throw new ProgramError("Invalid registry id."); return id; }
    private static int quantity(DepotProgram.Value value) { long amount = number(value); if (amount < 1 || amount > 4096) throw new ProgramError("Quantity must be 1-4096."); return (int) amount; }
    private static boolean requiresNetwork(String op) { return !List.of("if", "repeat", "repeat_until", "wait", "wait_until", "stop", "set_variable", "change_variable").contains(op); }

    private static Map<UUID, DepotProgram.Node> index(List<DepotProgram.Node> roots) {
        Map<UUID, DepotProgram.Node> result = new java.util.HashMap<>();
        Deque<DepotProgram.Node> open = new java.util.ArrayDeque<>(roots);
        while (!open.isEmpty()) {
            DepotProgram.Node node = open.removeFirst();
            result.put(node.id(), node);
            open.addAll(node.inputs().values());
            node.stacks().values().forEach(open::addAll);
        }
        return result;
    }
    private static void fail(DepotProgramRun run, UUID node, String message) { run.currentNode(node); run.status(DepotProgramRun.Status.ERROR); run.message(message); }
    private static final class ProgramError extends RuntimeException { ProgramError(String message) { super(message); } }
}
