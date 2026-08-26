package net.crystalnexus.program;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DepotProgramRun {
    public enum Status { RUNNING, WAITING_JOB, WAITING_TIME, WAITING_NETWORK, WAITING_RECIPE_SYNC, COMPLETED, CANCELLED, ERROR }
    public record Instruction(String kind, UUID nodeId, long remaining) {}

    private final UUID programId;
    private final int revision;
    private final Deque<Instruction> instructions;
    private final Map<String, DepotProgram.Value> variables;
    private Status status;
    private UUID currentNode;
    private String message;
    private long wakeTick;
    private int waitingJobId;
    private long executed;
    private long loops;

    public DepotProgramRun(UUID programId, int revision, Deque<Instruction> instructions,
            Map<String, DepotProgram.Value> variables, Status status, UUID currentNode, String message,
            long wakeTick, int waitingJobId, long executed, long loops) {
        this.programId = programId;
        this.revision = revision;
        this.instructions = instructions;
        this.variables = variables;
        this.status = status;
        this.currentNode = currentNode;
        this.message = message == null ? "" : message;
        this.wakeTick = wakeTick;
        this.waitingJobId = waitingJobId;
        this.executed = executed;
        this.loops = loops;
    }

    public static DepotProgramRun start(DepotProgram program) {
        Deque<Instruction> queue = new ArrayDeque<>();
        prepend(queue, program.body());
        Map<String, DepotProgram.Value> variables = new LinkedHashMap<>();
        program.variables().forEach(variable -> variables.put(variable.name().toLowerCase(java.util.Locale.ROOT), variable.initial()));
        return new DepotProgramRun(program.id(), program.revision(), queue, variables, Status.RUNNING,
                null, "Running", 0, 0, 0, 0);
    }

    public static void prepend(Deque<Instruction> queue, List<DepotProgram.Node> nodes) {
        for (int i = nodes.size() - 1; i >= 0; i--) queue.addFirst(new Instruction("node", nodes.get(i).id(), 0));
    }

    public UUID programId() { return programId; }
    public int revision() { return revision; }
    public Deque<Instruction> instructions() { return instructions; }
    public Map<String, DepotProgram.Value> variables() { return variables; }
    public Status status() { return status; }
    public void status(Status value) { status = value; }
    public UUID currentNode() { return currentNode; }
    public void currentNode(UUID value) { currentNode = value; }
    public String message() { return message; }
    public void message(String value) { message = value == null ? "" : value; }
    public long wakeTick() { return wakeTick; }
    public void wakeTick(long value) { wakeTick = value; }
    public int waitingJobId() { return waitingJobId; }
    public void waitingJobId(int value) { waitingJobId = value; }
    public long executed() { return executed; }
    public void incrementExecuted() { executed++; }
    public long loops() { return loops; }
    public void incrementLoops() { loops++; }
    public boolean active() {
        return status == Status.RUNNING || status == Status.WAITING_JOB || status == Status.WAITING_TIME
                || status == Status.WAITING_NETWORK || status == Status.WAITING_RECIPE_SYNC;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("program", programId);
        tag.putInt("revision", revision);
        tag.putString("status", status.name());
        if (currentNode != null) tag.putUUID("current", currentNode);
        tag.putString("message", message);
        tag.putLong("wake", wakeTick);
        tag.putInt("job", waitingJobId);
        tag.putLong("executed", executed);
        tag.putLong("loops", loops);
        ListTag queued = new ListTag();
        instructions.forEach(instruction -> {
            CompoundTag saved = new CompoundTag();
            saved.putString("kind", instruction.kind());
            saved.putUUID("node", instruction.nodeId());
            saved.putLong("remaining", instruction.remaining());
            queued.add(saved);
        });
        tag.put("instructions", queued);
        CompoundTag values = new CompoundTag();
        variables.forEach((key, value) -> values.put(key, value.save()));
        tag.put("variables", values);
        return tag;
    }

    public static DepotProgramRun load(CompoundTag tag) {
        Deque<Instruction> instructions = new ArrayDeque<>();
        ListTag queued = tag.getList("instructions", Tag.TAG_COMPOUND);
        for (int i = 0; i < queued.size(); i++) {
            CompoundTag saved = queued.getCompound(i);
            if (saved.hasUUID("node")) instructions.addLast(new Instruction(saved.getString("kind"),
                    saved.getUUID("node"), saved.getLong("remaining")));
        }
        Map<String, DepotProgram.Value> variables = new LinkedHashMap<>();
        CompoundTag values = tag.getCompound("variables");
        values.getAllKeys().forEach(key -> variables.put(key, DepotProgram.Value.load(values.getCompound(key))));
        Status status;
        try { status = Status.valueOf(tag.getString("status")); }
        catch (IllegalArgumentException ignored) { status = Status.ERROR; }
        return new DepotProgramRun(tag.getUUID("program"), tag.getInt("revision"), instructions, variables,
                status, tag.hasUUID("current") ? tag.getUUID("current") : null, tag.getString("message"),
                tag.getLong("wake"), tag.getInt("job"), tag.getLong("executed"), tag.getLong("loops"));
    }
}
