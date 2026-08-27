package net.crystalnexus.automation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public record DepotProgram(UUID id, String name, boolean enabled, ProgramTrigger trigger,
        List<ProgramCondition> conditions, List<ProgramAction> actions) {
    public enum TriggerType { ITEM_ADDED, INVENTORY_CHANGED, TIMED_INTERVAL }
    public enum ConditionType { COUNT_AT_LEAST, COUNT_LESS, EXISTS, MISSING }
    public enum ActionType { CRAFT, SEND_ITEM, PROCESS }

    public record ProgramTrigger(TriggerType type, ResourceLocation itemId, int interval) {
        public ProgramTrigger { if (interval < 20) interval = 20; }
        public ProgramTrigger(TriggerType type, ResourceLocation itemId) { this(type, itemId, 0); }
    }
    public record ProgramCondition(ConditionType type, ResourceLocation itemId, long amount) {
        public ProgramCondition { amount = Math.max(0, amount); }
    }
    public record ProgramAction(ActionType type, ResourceLocation itemId, int amount, ResourceLocation machineId) {
        public ProgramAction { amount = Math.max(1, amount); }
        public ProgramAction(ActionType type, ResourceLocation itemId, int amount) { this(type, itemId, amount, null); }
    }

    public DepotProgram {
        if (id == null) id = UUID.randomUUID();
        name = name == null || name.isBlank() ? "Depot Program" : name.strip().substring(0, Math.min(64, name.strip().length()));
        conditions = List.copyOf(conditions);
        actions = List.copyOf(actions);
    }

    public DepotProgram withEnabled(boolean value) {
        return new DepotProgram(id, name, value, trigger, conditions, actions);
    }

    public String summary() {
        String when = switch (trigger.type()) {
            case ITEM_ADDED -> shortId(trigger.itemId()) + " Added";
            case INVENTORY_CHANGED -> "Inventory Changed";
            case TIMED_INTERVAL -> "Every " + formatTicks(trigger.interval()) + " ticks";
        };
        String action = actions.isEmpty() ? "Do nothing" : switch (actions.getFirst().type()) {
            case CRAFT -> "Craft " + shortId(actions.getFirst().itemId()) + " x" + actions.getFirst().amount();
            case SEND_ITEM -> "Send " + shortId(actions.getFirst().itemId()) + " x" + actions.getFirst().amount();
            case PROCESS -> "Process " + shortId(actions.getFirst().itemId()) + " @ " + shortId(actions.getFirst().machineId()) + " x" + actions.getFirst().amount();
        };
        return when + " -> " + action;
    }

    private static String formatTicks(int ticks) {
        if (ticks >= 72000) return ticks / 72000 + "h";
        if (ticks >= 3600) return ticks / 3600 + "m";
        if (ticks >= 20) return ticks / 20 + "s";
        return ticks + "t";
    }

    private static String shortId(ResourceLocation id) { return id == null ? "Item" : id.getPath(); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putBoolean("enabled", enabled);
        tag.putString("trigger", trigger.type().name());
        if (trigger.itemId() != null) tag.putString("triggerItem", trigger.itemId().toString());
        if (trigger.type() == TriggerType.TIMED_INTERVAL) tag.putInt("interval", trigger.interval());
        ListTag savedConditions = new ListTag();
        for (ProgramCondition condition : conditions) {
            CompoundTag saved = new CompoundTag();
            saved.putString("type", condition.type().name());
            saved.putString("item", condition.itemId().toString());
            saved.putLong("amount", condition.amount());
            savedConditions.add(saved);
        }
        tag.put("conditions", savedConditions);
        ListTag savedActions = new ListTag();
        for (ProgramAction action : actions) {
            CompoundTag saved = new CompoundTag();
            saved.putString("type", action.type().name());
            saved.putString("item", action.itemId().toString());
            saved.putInt("amount", action.amount());
            if (action.machineId() != null) saved.putString("machine", action.machineId().toString());
            savedActions.add(saved);
        }
        tag.put("actions", savedActions);
        return tag;
    }

    public static DepotProgram load(CompoundTag tag) {
        try {
            UUID id = tag.hasUUID("id") ? tag.getUUID("id") : UUID.randomUUID();
            TriggerType triggerType = TriggerType.valueOf(tag.getString("trigger"));
            ResourceLocation triggerItem = ResourceLocation.tryParse(tag.getString("triggerItem"));
            int interval = tag.contains("interval") ? tag.getInt("interval") : 0;
            if (triggerType == TriggerType.ITEM_ADDED && triggerItem == null) return null;
            if (triggerType == TriggerType.TIMED_INTERVAL && interval < 20) return null;
            List<ProgramCondition> conditions = new java.util.ArrayList<>();
            ListTag savedConditions = tag.getList("conditions", Tag.TAG_COMPOUND);
            for (int i = 0; i < savedConditions.size(); i++) {
                CompoundTag saved = savedConditions.getCompound(i);
                ResourceLocation item = ResourceLocation.tryParse(saved.getString("item"));
                if (item != null) conditions.add(new ProgramCondition(ConditionType.valueOf(saved.getString("type")),
                        item, saved.getLong("amount")));
            }
            List<ProgramAction> actions = new java.util.ArrayList<>();
            ListTag savedActions = tag.getList("actions", Tag.TAG_COMPOUND);
            for (int i = 0; i < savedActions.size(); i++) {
                CompoundTag saved = savedActions.getCompound(i);
                ResourceLocation item = ResourceLocation.tryParse(saved.getString("item"));
                ResourceLocation machine = ResourceLocation.tryParse(saved.getString("machine"));
                if (item != null) actions.add(new ProgramAction(ActionType.valueOf(saved.getString("type")),
                        item, saved.getInt("amount"), machine));
            }
            if (actions.isEmpty()) return null;
            return new DepotProgram(id, tag.getString("name"), tag.getBoolean("enabled"),
                    new ProgramTrigger(triggerType, triggerItem, interval), conditions, actions);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
