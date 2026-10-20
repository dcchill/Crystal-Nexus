package net.crystalnexus.drone;

import net.crystalnexus.assembly.AssemblyGraph;
import net.minecraft.nbt.CompoundTag;
import java.util.Locale;

public enum DroneNode {
    START, IF_ELSE, AND, OR, NOT, REPEAT, WAIT, STOP, SET_VARIABLE, GET_VARIABLE, COMPARE,
    GO_TO_POSITION, GO_TO_BLOCK, GO_TO_ENTITY, RETURN_HOME, FOLLOW_PLAYER, SET_HOME, PATROL,
    WAIT_UNTIL_ARRIVED, MAINTAIN_DISTANCE,
    RECALL_DRONE, RECALL_TO_PLAYER, RECALL_TO_HOME, IS_RECALLING, ON_RECALL_COMPLETE, RESUME_PREVIOUS_TASK,
    DETECT_ENTITY, DETECT_BLOCK, FIND_NEAREST_MATCHING_BLOCK, INVENTORY_FULL, INVENTORY_EMPTY,
    BATTERY_LEVEL, FLUID_TANK_LEVEL, TARGET_DISTANCE, IS_PLAYER_NEARBY, IS_UNDER_ATTACK, HAS_LINE_OF_SIGHT,
    EXTRACT_ITEMS(DroneMode.TRANSPORT), INSERT_ITEMS(DroneMode.TRANSPORT), FILTER_ITEMS(DroneMode.TRANSPORT),
    SET_TRANSFER_AMOUNT(DroneMode.TRANSPORT), COLLECT_DROPPED_ITEMS, DROP_ITEMS(DroneMode.TRANSPORT),
    FIND_INVENTORY(DroneMode.TRANSPORT), WAIT_UNTIL_INVENTORY_AVAILABLE(DroneMode.TRANSPORT),
    SET_MINING_AREA(DroneMode.MINING), FIND_MATCHING_BLOCK(DroneMode.MINING), MINE_BLOCK(DroneMode.MINING),
    MINE_AREA(DroneMode.MINING), FILTER_BLOCKS(DroneMode.MINING), COLLECT_DROPS,
    SKIP_UNBREAKABLE_BLOCKS(DroneMode.MINING), RETURN_WHEN_FULL(DroneMode.MINING),
    PLACE_BLOCK(DroneMode.BUILDER), REPLACE_BLOCK(DroneMode.BUILDER), BUILD_AREA(DroneMode.BUILDER),
    LOAD_BLUEPRINT(DroneMode.BUILDER), NEXT_BLUEPRINT_POSITION(DroneMode.BUILDER), CHECK_BLOCK_STATE(DroneMode.BUILDER),
    FIND_REQUIRED_MATERIAL(DroneMode.BUILDER), WAIT_FOR_MATERIALS(DroneMode.BUILDER),
    SELECT_RECIPIENT(DroneMode.COURIER), FIND_PLAYER(DroneMode.COURIER), DELIVER_ITEMS(DroneMode.COURIER),
    WAIT_FOR_RECIPIENT(DroneMode.COURIER), RETURN_AFTER_DELIVERY(DroneMode.COURIER),
    CHECK_RECIPIENT_INVENTORY_SPACE(DroneMode.COURIER), RETRIEVE_REQUESTED_ITEMS(DroneMode.COURIER),
    EXTRACT_FLUID(DroneMode.FLUID_TRANSPORT), INSERT_FLUID(DroneMode.FLUID_TRANSPORT), FILTER_FLUID(DroneMode.FLUID_TRANSPORT),
    SET_FLUID_AMOUNT(DroneMode.FLUID_TRANSPORT), FIND_FLUID_TANK(DroneMode.FLUID_TRANSPORT),
    CHECK_TANK_CAPACITY(DroneMode.FLUID_TRANSPORT), WAIT_UNTIL_FLUID_AVAILABLE(DroneMode.FLUID_TRANSPORT),
    TARGET_HOSTILE_MOBS(DroneMode.COMBAT), TARGET_PASSIVE_MOBS(DroneMode.COMBAT), TARGET_SPECIFIC_ENTITY(DroneMode.COMBAT),
    EXCLUDE_ENTITY(DroneMode.COMBAT), TARGET_ENTITY_TAG(DroneMode.COMBAT), TARGET_WITHIN_RADIUS(DroneMode.COMBAT),
    TARGET_ADULT_ENTITIES(DroneMode.COMBAT), TARGET_BABY_ENTITIES(DroneMode.COMBAT), TARGET_TAMED_ENTITIES(DroneMode.COMBAT),
    TARGET_UNTAMED_ENTITIES(DroneMode.COMBAT), TARGET_LOW_HEALTH(DroneMode.COMBAT), TARGET_NEAREST_ENTITY(DroneMode.COMBAT),
    ATTACK_TARGET(DroneMode.COMBAT), ATTACK_UNTIL_DEAD(DroneMode.COMBAT), FOLLOW_TARGET(DroneMode.COMBAT),
    MAINTAIN_ATTACK_DISTANCE(DroneMode.COMBAT), STOP_ATTACKING(DroneMode.COMBAT), DEFEND_PLAYER(DroneMode.COMBAT),
    DEFEND_AREA(DroneMode.COMBAT), RETREAT(DroneMode.COMBAT), RETURN_TO_PATROL(DroneMode.COMBAT), WAIT_FOR_TARGET(DroneMode.COMBAT),
    TARGET_HEALTH(DroneMode.COMBAT), DRONE_HEALTH(DroneMode.COMBAT), TARGET_IS_ALIVE(DroneMode.COMBAT),
    TARGET_IS_ATTACKING_PLAYER(DroneMode.COMBAT), TARGET_IS_ATTACKING_DRONE(DroneMode.COMBAT),
    DISTANCE_TO_TARGET(DroneMode.COMBAT), ATTACK_COOLDOWN_READY(DroneMode.COMBAT);

    public final DroneMode mode;
    DroneNode() { mode = null; }
    DroneNode(DroneMode mode) { this.mode = mode; }
    public boolean available(DroneMode current) { return mode == null || mode == current || current == DroneMode.BUILDER && this == EXTRACT_ITEMS || current == DroneMode.TRANSPORT && mode == DroneMode.COURIER; }
    public String id() { return "crystalnexus:drone/" + name().toLowerCase(Locale.ROOT); }
    public String label() {
        StringBuilder s = new StringBuilder();
        for (String word : name().toLowerCase(Locale.ROOT).split("_")) {
            if (!s.isEmpty()) s.append(' ');
            s.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return s.toString();
    }
    public static DroneNode of(AssemblyGraph.Node node) {
        if (!node.block.startsWith("crystalnexus:drone/")) throw new IllegalArgumentException("Not a drone node");
        return valueOf(node.block.substring(19).toUpperCase(Locale.ROOT));
    }
    public AssemblyGraph.Node create(int id, float x, float y) {
        var node = new AssemblyGraph.Node(id, 0, id(), x, y);
        node.outputSockets = 3;
        node.parameters = defaults();
        return node;
    }
    public CompoundTag defaults() {
        CompoundTag p = new CompoundTag();
        switch (this) {
            case WAIT -> p.putInt("ticks", 20);
            case REPEAT -> { p.putInt("node", 0); p.putInt("count", 0); }
            case SET_VARIABLE, GET_VARIABLE, IF_ELSE, COMPARE -> { p.putString("variable", "result"); p.putDouble("value", 1); p.putString("operator", ">="); }
            case AND, OR, NOT -> { p.putString("a", "result"); p.putString("b", "other"); }
            case RECALL_DRONE, RECALL_TO_PLAYER, RECALL_TO_HOME -> { p.putString("player", "owner"); p.putBoolean("resume", false); }
            default -> {
                p.putIntArray("pos", new int[]{0, 64, 0});
                p.putString("filter", "");
                p.putString("player", "owner");
                p.putInt("amount", 64);
                p.putDouble("range", 3);
            }
        }
        switch (this) {
            case SET_MINING_AREA, MINE_AREA, BUILD_AREA, DEFEND_AREA -> p.putIntArray("end", new int[]{0,64,0});
            case PATROL -> { var points = new net.minecraft.nbt.ListTag(); points.add(new net.minecraft.nbt.IntArrayTag(new int[]{0,64,0})); p.put("waypoints", points); }
            case LOAD_BLUEPRINT -> { var blocks = new net.minecraft.nbt.ListTag(); var block = new CompoundTag(); block.putIntArray("offset",new int[]{0,0,0}); block.putString("item","minecraft:stone"); blocks.add(block); p.put("blocks",blocks); }
            case BATTERY_LEVEL, FLUID_TANK_LEVEL, TARGET_HEALTH, DRONE_HEALTH, TARGET_DISTANCE, DISTANCE_TO_TARGET, CHECK_TANK_CAPACITY -> { p.putDouble("value",50); p.putString("operator",">="); }
            case SKIP_UNBREAKABLE_BLOCKS -> p.putBoolean("enabled",true);
            default -> {}
        }
        switch (this) {
            case MINE_BLOCK, PLACE_BLOCK, REPLACE_BLOCK, EXTRACT_ITEMS, INSERT_ITEMS, EXTRACT_FLUID, INSERT_FLUID, RETRIEVE_REQUESTED_ITEMS -> p.putBoolean("useSelected",true);
            case FOLLOW_PLAYER, GO_TO_POSITION, GO_TO_BLOCK, GO_TO_ENTITY, PATROL, MAINTAIN_DISTANCE -> p.putDouble("speed",1);
            default -> {}
        }
        if (this == FILTER_ITEMS) p.put("components",new CompoundTag());
        return p;
    }
    public String description() {
        return switch (this) {
            case FOLLOW_PLAYER -> "Persistent follow. Pause/recall/stop to interrupt; distance is range.";
            case RECALL_DRONE, RECALL_TO_PLAYER -> "Return to owner or authorized UUID. resume optionally restores the interrupted task.";
            case RECALL_TO_HOME -> "Recall to saved home. Cross-dimension travel is refused.";
            case ON_RECALL_COMPLETE -> "Event entry. Runs once after a successful recall; output 0 continues.";
            case RESUME_PREVIOUS_TASK -> "Restore a saved task only if its mode and program revision still match.";
            case REPEAT -> "Jump to node ID count times; count 0 repeats until interrupted.";
            case LOAD_BLUEPRINT -> "blocks: list of {offset:[I;x,y,z],item:\"minecraft:stone\"}; pos is the origin.";
            case SET_MINING_AREA, MINE_AREA, BUILD_AREA -> "pos and end are inclusive corners, at most 4096 blocks. filter is ID or #tag.";
            default -> "Output 0: success/true; 1: false; 2: failure. pos: coordinates; player: owner/UUID; filter: ID/#tag; side: capability face.";
        };
    }
}
