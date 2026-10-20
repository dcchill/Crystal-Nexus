package net.crystalnexus.drone;

import net.crystalnexus.assembly.AssemblyGraph;
import net.minecraft.nbt.*;
import java.util.*;

/** Explicit editor fields; saved legacy metadata remains untouched unless edited. */
public final class DroneNodeSettings {
    private DroneNodeSettings() {}
    public static String title(DroneNode n){return switch(n){case GO_TO_BLOCK->"Fly to Destination";case GO_TO_POSITION->"Fly to Position";case EXTRACT_ITEMS->"Pick Up Items";case INSERT_ITEMS->"Drop Off Items";case EXTRACT_FLUID->"Pick Up Fluid";case INSERT_FLUID->"Drop Off Fluid";default->n.label();};}
    public static List<DroneNode> search(DroneMode mode,String category,String query){
        String[] words=query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return Arrays.stream(DroneNode.values()).filter(n->n.available(mode))
            .filter(n->!query.isBlank()||category.equals("All")||category.equals("Common")&&common(n)||category(n).equals(category))
            .filter(n->{String text=(title(n)+" "+n.label()+" "+description(n)+" "+category(n)+(category(n).equals("Movement")?" move travel destination":"")).toLowerCase(Locale.ROOT);return Arrays.stream(words).allMatch(text::contains);})
            .sorted(Comparator.comparing(DroneNodeSettings::title)).toList();
    }
    public static List<String> fields(DroneNode n) {
        return switch(n) {
            case GO_TO_POSITION,GO_TO_BLOCK -> List.of("destinationSlot","range","speed");
            case SET_HOME -> List.of("destinationSlot");
            case DETECT_BLOCK,CHECK_BLOCK_STATE -> List.of("destinationSlot","filter");
            case FIND_NEAREST_MATCHING_BLOCK,FIND_MATCHING_BLOCK,FIND_INVENTORY,FIND_FLUID_TANK -> List.of("destinationSlot","range","filter");
            case EXTRACT_ITEMS,INSERT_ITEMS,RETRIEVE_REQUESTED_ITEMS,EXTRACT_FLUID,INSERT_FLUID -> List.of("destinationSlot","amount","filter");
            case MINE_BLOCK,PLACE_BLOCK,REPLACE_BLOCK -> List.of("destinationSlot","filter");
            case WAIT_UNTIL_INVENTORY_AVAILABLE,WAIT_UNTIL_FLUID_AVAILABLE -> List.of("destinationSlot");
            case SET_MINING_AREA,MINE_AREA,BUILD_AREA -> List.of("destinationSlot","endDestinationSlot","filter");
            case DEFEND_AREA -> List.of("destinationSlot","endDestinationSlot","range");
            case LOAD_BLUEPRINT -> List.of("destinationSlot","blocks");
            case PATROL,RETURN_TO_PATROL -> List.of("waypointSlots","range","speed");
            case FOLLOW_PLAYER,MAINTAIN_DISTANCE -> List.of("player","range","speed");
            case GO_TO_ENTITY,FOLLOW_TARGET,MAINTAIN_ATTACK_DISTANCE,RETREAT -> List.of("range","speed");
            case SELECT_RECIPIENT,FIND_PLAYER,WAIT_FOR_RECIPIENT,DELIVER_ITEMS,CHECK_RECIPIENT_INVENTORY_SPACE,DEFEND_PLAYER -> List.of("player");
            case RECALL_DRONE,RECALL_TO_PLAYER -> List.of("player","resume");
            case RECALL_TO_HOME -> List.of("resume");
            case IS_PLAYER_NEARBY -> List.of("player","range");
            case TARGET_IS_ATTACKING_PLAYER -> List.of("player");
            case SET_VARIABLE -> List.of("variable","value");
            case GET_VARIABLE,IF_ELSE,COMPARE -> List.of("variable","operator","value");
            case AND,OR -> List.of("a","b");
            case NOT -> List.of("a");
            case REPEAT -> List.of("node","count");
            case WAIT -> List.of("ticks");
            case FILTER_ITEMS,FILTER_BLOCKS,FILTER_FLUID,TARGET_SPECIFIC_ENTITY,EXCLUDE_ENTITY,TARGET_ENTITY_TAG -> List.of("filter");
            case SET_TRANSFER_AMOUNT,SET_FLUID_AMOUNT,DROP_ITEMS -> List.of("amount");
            case COLLECT_DROPPED_ITEMS,COLLECT_DROPS,DETECT_ENTITY,TARGET_NEAREST_ENTITY,WAIT_FOR_TARGET -> List.of("range","filter");
            case FIND_REQUIRED_MATERIAL,WAIT_FOR_MATERIALS -> List.of("filter");
            case BATTERY_LEVEL,FLUID_TANK_LEVEL,TARGET_DISTANCE,DISTANCE_TO_TARGET,TARGET_HEALTH,DRONE_HEALTH,CHECK_TANK_CAPACITY -> List.of("operator","value");
            case TARGET_WITHIN_RADIUS -> List.of("range");
            case TARGET_LOW_HEALTH -> List.of("value");
            case SKIP_UNBREAKABLE_BLOCKS -> List.of("enabled");
            default -> List.of();
        };
    }
    public static AssemblyGraph.Node create(DroneNode kind,int id,float x,float y) {
        var node=kind.create(id,x,y);CompoundTag p=new CompoundTag();
        for(String key:fields(kind)) {
            if(key.equals("destinationSlot")||key.equals("endDestinationSlot")||key.equals("waypointSlots"))continue;
            Tag value=node.parameters.get(key);if(value!=null)p.put(key,value.copy());
            else if(key.equals("speed"))p.putDouble(key,1);
        }
        node.parameters=p;return node;
    }
    public static boolean condition(DroneNode n) {
        return switch(n) {
            case IF_ELSE,AND,OR,NOT,GET_VARIABLE,COMPARE,IS_RECALLING,DETECT_ENTITY,DETECT_BLOCK,
                 FIND_NEAREST_MATCHING_BLOCK,INVENTORY_FULL,INVENTORY_EMPTY,BATTERY_LEVEL,FLUID_TANK_LEVEL,
                 TARGET_DISTANCE,IS_PLAYER_NEARBY,IS_UNDER_ATTACK,HAS_LINE_OF_SIGHT,FIND_INVENTORY,FIND_MATCHING_BLOCK,
                 NEXT_BLUEPRINT_POSITION,CHECK_BLOCK_STATE,FIND_REQUIRED_MATERIAL,FIND_PLAYER,CHECK_RECIPIENT_INVENTORY_SPACE,
                 FIND_FLUID_TANK,CHECK_TANK_CAPACITY,TARGET_NEAREST_ENTITY,TARGET_HEALTH,DRONE_HEALTH,TARGET_IS_ALIVE,
                 TARGET_IS_ATTACKING_PLAYER,TARGET_IS_ATTACKING_DRONE,DISTANCE_TO_TARGET,ATTACK_COOLDOWN_READY -> true;
            default -> false;
        };
    }
    public static String category(DroneNode n) {
        if(n==DroneNode.ON_RECALL_COMPLETE)return "Events";
        if(n.mode!=null)return "Mode actions";
        if(condition(n))return "Conditions";
        return switch(n){
            case START,STOP,WAIT,REPEAT,SET_VARIABLE,AND,OR,NOT,COMPARE,GET_VARIABLE -> "Logic";
            case COLLECT_DROPPED_ITEMS,COLLECT_DROPS -> "Mode actions";
            default -> "Movement";
        };
    }
    public static boolean common(DroneNode n) {
        return switch(n){case GO_TO_BLOCK,RETURN_HOME,RECALL_DRONE,WAIT,REPEAT,STOP,EXTRACT_ITEMS,INSERT_ITEMS,MINE_BLOCK,MINE_AREA,
            PLACE_BLOCK,BUILD_AREA,DELIVER_ITEMS,FOLLOW_PLAYER,EXTRACT_FLUID,INSERT_FLUID,DEFEND_PLAYER,DEFEND_AREA -> true;default -> false;};
    }
    public static String label(DroneNode kind,String key) {
        if(key.equals("waitUntilComplete"))return "Wait until node completes";
        return switch(key){case "destinationSlot" -> fields(kind).contains("endDestinationSlot")?"First corner":"Destination";
            case "endDestinationSlot"->"Second corner";case "waypointSlots"->"Patrol destinations";case "player"->"Player";
            case "ticks"->"Wait (seconds)";case "count"->"Repeats (0 = forever)";case "node"->"Repeat from node";
            case "amount"->kind.name().contains("FLUID")?"Amount (mB)":"Amount (items)";
            case "range"->"Distance (blocks)";case "value"->kind==DroneNode.BATTERY_LEVEL?"Battery (%)":"Value";
            case "speed"->"Flight speed";case "filter"->"Item / block / entity filter";case "a"->"First variable";case "b"->"Second variable";
            case "components"->"Component matching";case "side"->"Access face";case "pos"->"Coordinates: x y z";case "end"->"Second corner: x y z";
            default->Character.toUpperCase(key.charAt(0))+key.substring(1);};
    }
    public static String description(DroneNode n) {
        return switch(n){
            case START->"Begin this program.";
            case GO_TO_BLOCK,GO_TO_POSITION->"Fly to an installed destination card.";
            case EXTRACT_ITEMS->"Collect items from the selected container.";case INSERT_ITEMS->"Unload cargo into the selected container.";
            case COLLECT_DROPPED_ITEMS,COLLECT_DROPS->"Pick up nearby drops; continue Next when none are ready.";
            case EXTRACT_FLUID->"Collect fluid from the selected tank.";case INSERT_FLUID->"Unload fluid into the selected tank.";
            case MINE_BLOCK->"Instantly mine using a cargo pickaxe with the required harvest level.";case MINE_AREA->"Mine between two destination cards.";
            case PLACE_BLOCK->"Place a block from cargo at a destination.";case BUILD_AREA->"Fill the space between two destination cards.";
            case DELIVER_ITEMS->"Fly to a permitted player and deliver cargo.";case FOLLOW_PLAYER->"Keep following a permitted player until stopped.";
            case RECALL_DRONE,RECALL_TO_PLAYER->"Return to a permitted player and hover nearby.";
            case RECALL_TO_HOME,RETURN_HOME->"Return to the drone's saved home.";case WAIT->"Wait before continuing to the next action.";
            case REPEAT->"Repeat from an earlier node.";case STOP->"Stop this program.";case SET_HOME->"Set home using a destination card.";
            case PATROL,RETURN_TO_PATROL->"Visit installed destination cards in order, repeatedly.";
            case ON_RECALL_COMPLETE->"Run this branch when recall finishes.";case RESUME_PREVIOUS_TASK->"Continue the task interrupted by recall.";
            default->condition(n)?"Check a condition and follow its True or False branch.":"Configure "+n.label().toLowerCase(Locale.ROOT)+" for this program.";};
    }
}
