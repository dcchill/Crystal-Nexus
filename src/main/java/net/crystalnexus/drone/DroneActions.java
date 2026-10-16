package net.crystalnexus.drone;

import net.crystalnexus.assembly.AssemblyGraph;
import net.crystalnexus.entity.LUNADroneEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Returns -1 while running; otherwise success, false, or failure output. */
public final class DroneActions {
    final DroneRuntime r;
    final LUNADroneEntity d;
    final DroneWork work;
    final DroneCombat combat;
    public String error = "";
    public DroneActions(DroneRuntime runtime) { r = runtime; d = r.drone; work = new DroneWork(this); combat = new DroneCombat(this); }
    CompoundTag memory() { return d.program.memory; }
    CompoundTag vars() { if (!memory().contains("vars")) memory().put("vars", new CompoundTag()); return memory().getCompound("vars"); }
    int failure(String message) { error = message; d.status = message; return 2; }
    int condition(boolean value) { vars().putDouble("result", value ? 1 : 0); return value ? 0 : 1; }
    int number(double value, CompoundTag p) { vars().putDouble("result", value); return compare(value, p.getDouble("value"), p.contains("operator") ? p.getString("operator") : ">") ? 0 : 1; }
    private static boolean compare(double a, double b, String op) {
        return switch (op) { case "=", "==" -> a == b; case "!=" -> a != b; case "<" -> a < b; case "<=" -> a <= b; case ">" -> a > b; case ">=" -> a >= b; default -> throw new IllegalArgumentException("Unknown comparison"); };
    }
    BlockPos pos(CompoundTag p) {
        if ((!p.contains("pos") || p.getBoolean("useSelected")) && memory().contains("position")) return BlockPos.of(memory().getLong("position"));
        int[] a = p.getIntArray("pos"); if (a.length != 3) throw new IllegalArgumentException("pos requires three coordinates");
        return new BlockPos(a[0], a[1], a[2]);
    }
    double range(CompoundTag p) { return p.contains("range") ? p.getDouble("range") : 3; }
    int amount(CompoundTag p) { return Math.max(1, p.contains("amount") ? p.getInt("amount") : memory().getInt("amount")); }
    String filter(CompoundTag p) { return p.getString("filter").isEmpty() ? memory().getString("filter") : p.getString("filter"); }
    String recipient(CompoundTag p) { return p.getString("player").isEmpty() ? memory().getString("recipient") : p.getString("player"); }
    boolean loaded(BlockPos p) { return r.level().isInWorldBounds(p) && r.level().hasChunkAt(p); }
    boolean near(BlockPos p) { return d.position().distanceToSqr(Vec3.atCenterOf(p)) <= 16; }
    int navigate(Vec3 target, double distance, CompoundTag p, boolean persistent) {
        var result = r.navigate(target, distance, p.contains("speed") ? p.getDouble("speed") : 1, persistent);
        if (result == DroneFlight.Result.FAILURE) return failure(r.flight.error);
        return result == DroneFlight.Result.SUCCESS && !persistent ? 0 : -1;
    }
    int home(CompoundTag p) {
        if (d.home == null) return failure("No home saved");
        if (!d.homeDimension.equals(d.level().dimension().location().toString())) return failure("Home is in another dimension");
        return navigate(Vec3.atCenterOf(d.home), 1.5, p, false);
    }
    int follow(CompoundTag p, boolean persistent) {
        ServerPlayer player = r.player(recipient(p));
        if (player == null) return failure("Player is offline or unauthorized");
        if (player.level() != d.level()) return failure("Player is in another dimension");
        return navigate(player.position().add(0, 2, 0), Math.max(1, range(p)), p, persistent);
    }
    LivingEntity target() { return memory().hasUUID("target") && r.level().getEntity(memory().getUUID("target")) instanceof LivingEntity e && e.isAlive() ? e : null; }
    int waitFor(boolean value) { return value ? 0 : d.program.elapsed > 1200 ? failure("Timed out waiting for target or resources") : -1; }
    public int step(DroneNode kind, AssemblyGraph.Node node) {
        CompoundTag p = DroneDestinations.resolve(d,node.parameters);
        return switch (kind) {
            case START -> { memory().remove("filters"); memory().remove("allowTamed"); yield 0; }
            case ON_RECALL_COMPLETE -> 0;
            case STOP -> { d.program.stop(); r.cancel(); yield -1; }
            case WAIT -> d.program.elapsed >= p.getInt("ticks") ? 0 : -1;
            case REPEAT -> repeat(node);
            case SET_VARIABLE -> { vars().putDouble(p.getString("variable"), p.getDouble("value")); yield 0; }
            case GET_VARIABLE -> number(vars().getDouble(p.getString("variable")), p);
            case IF_ELSE, COMPARE -> condition(compare(vars().getDouble(p.getString("variable")), p.getDouble("value"), p.getString("operator")));
            case AND, OR, NOT -> {
                if (d.mode == DroneMode.COMBAT && combat.filters().size() >= (kind == DroneNode.NOT ? 1 : 2)) { combat.combine(kind); yield 0; }
                boolean a = vars().getDouble(p.getString("a")) != 0, b = vars().getDouble(p.getString("b")) != 0;
                yield condition(kind == DroneNode.AND ? a && b : kind == DroneNode.OR ? a || b : !a);
            }
            case GO_TO_POSITION -> navigate(Vec3.atCenterOf(pos(p)), range(p), p, false);
            case GO_TO_BLOCK -> navigate(Vec3.atCenterOf(pos(p)).add(0, 1, 0), range(p), p, false);
            case GO_TO_ENTITY -> target() == null ? failure("No valid entity selected") : navigate(target().position().add(0, 1, 0), range(p), p, false);
            case RETURN_HOME, RETURN_AFTER_DELIVERY -> home(p);
            case FOLLOW_PLAYER, MAINTAIN_DISTANCE -> follow(p, true);
            case SET_HOME -> { if(p.contains("destinationSlot")){d.home=pos(p);d.homeDimension=d.level().dimension().location().toString();}else d.setHome(); yield 0; }
            case PATROL, RETURN_TO_PATROL -> patrol(p);
            case WAIT_UNTIL_ARRIVED -> waitFor(d.getNavigation().isDone());
            case RECALL_DRONE, RECALL_TO_PLAYER, RECALL_TO_HOME -> recall(kind, p);
            case IS_RECALLING -> condition(r.isRecalling());
            case RESUME_PREVIOUS_TASK -> r.resume() ? -1 : failure(d.status);
            case INVENTORY_FULL -> condition(work.inventoryFull());
            case INVENTORY_EMPTY -> condition(work.inventoryEmpty());
            case BATTERY_LEVEL -> number(d.battery.getEnergyStored() / 2000.0, p);
            case FLUID_TANK_LEVEL -> number(d.tank.getFluidAmount(), p);
            case TARGET_DISTANCE, DISTANCE_TO_TARGET -> number(target() == null ? Double.MAX_VALUE : d.distanceTo(target()), p);
            case IS_PLAYER_NEARBY -> { ServerPlayer player = r.player(recipient(p)); yield condition(player != null && player.level() == d.level() && d.distanceTo(player) <= range(p)); }
            case IS_UNDER_ATTACK -> condition(d.getLastHurtByMob() != null && d.tickCount - d.getLastHurtByMobTimestamp() < 100);
            case HAS_LINE_OF_SIGHT -> condition(target() != null && d.hasLineOfSight(target()));
            case DETECT_BLOCK, CHECK_BLOCK_STATE -> loaded(pos(p)) ? condition(work.matchesBlock(pos(p), filter(p))) : failure("Block chunk is unloaded");
            case FIND_NEAREST_MATCHING_BLOCK, FIND_MATCHING_BLOCK -> work.scan(p, "block");
            case DETECT_ENTITY, TARGET_NEAREST_ENTITY -> combat.select(p, false);
            case FILTER_ITEMS, FILTER_BLOCKS, FILTER_FLUID -> { memory().putString("filter", p.getString("filter")); if (p.contains("components")) memory().put("components", p.getCompound("components").copy()); yield 0; }
            case SET_TRANSFER_AMOUNT, SET_FLUID_AMOUNT -> { memory().putInt("amount", amount(p)); yield 0; }
            case EXTRACT_ITEMS, RETRIEVE_REQUESTED_ITEMS -> work.transferItems(p, true);
            case INSERT_ITEMS -> work.transferItems(p, false);
            case COLLECT_DROPPED_ITEMS, COLLECT_DROPS -> work.collect(p);
            case DROP_ITEMS -> work.drop(p);
            case FIND_INVENTORY -> work.scan(p, "item");
            case WAIT_UNTIL_INVENTORY_AVAILABLE -> waitFor(loaded(pos(p)) && near(pos(p)) && work.itemHandler(p) != null);
            case SET_MINING_AREA -> { memory().putLong("areaStart", pos(p).asLong()); memory().putLong("areaEnd", work.end(p).asLong()); yield 0; }
            case MINE_BLOCK -> work.mine(pos(p), p, false);
            case MINE_AREA -> work.area(p, false);
            case SKIP_UNBREAKABLE_BLOCKS -> { memory().putBoolean("skipUnbreakable", !p.contains("enabled") || p.getBoolean("enabled")); yield 0; }
            case RETURN_WHEN_FULL -> work.inventoryFull() ? home(p) : 0;
            case PLACE_BLOCK -> work.place(pos(p), p);
            case REPLACE_BLOCK -> {
                if (!loaded(pos(p))) yield failure("Block chunk is unloaded");
                if (!r.level().getBlockState(pos(p)).canBeReplaced()) { int result = work.mine(pos(p), p, true); if (result != 0) yield result; }
                yield work.place(pos(p), p);
            }
            case BUILD_AREA -> work.area(p, true);
            case LOAD_BLUEPRINT -> work.blueprint(p);
            case NEXT_BLUEPRINT_POSITION -> work.nextBlueprint();
            case FIND_REQUIRED_MATERIAL -> condition(work.material(p) >= 0);
            case WAIT_FOR_MATERIALS -> waitFor(work.material(p) >= 0);
            case SELECT_RECIPIENT -> {
                String value = p.getString("player"); UUID id = value.equals("owner") ? d.owner : UUID.fromString(value);
                if (!d.permitted(id)) yield failure("Recipient is unauthorized"); memory().putString("recipient", value); yield 0;
            }
            case FIND_PLAYER -> {
                ServerPlayer player = r.player(recipient(p)); if (player == null || player.level() != d.level()) yield 1;
                memory().putUUID("target", player.getUUID()); yield 0;
            }
            case WAIT_FOR_RECIPIENT -> { ServerPlayer player = r.player(recipient(p)); yield waitFor(player != null && player.level() == d.level()); }
            case DELIVER_ITEMS -> work.deliver(p);
            case CHECK_RECIPIENT_INVENTORY_SPACE -> { ServerPlayer player = r.player(recipient(p)); yield condition(player != null && player.level() == d.level() && work.hasDeliverySpace(player)); }
            case EXTRACT_FLUID -> work.transferFluid(p, true);
            case INSERT_FLUID -> work.transferFluid(p, false);
            case FIND_FLUID_TANK -> work.scan(p, "fluid");
            case CHECK_TANK_CAPACITY -> number(d.tank.getCapacity() - d.tank.getFluidAmount(), p);
            case WAIT_UNTIL_FLUID_AVAILABLE -> waitFor(work.fluidAvailable(p));
            case TARGET_HOSTILE_MOBS, TARGET_PASSIVE_MOBS, TARGET_SPECIFIC_ENTITY, EXCLUDE_ENTITY, TARGET_ENTITY_TAG,
                 TARGET_WITHIN_RADIUS, TARGET_ADULT_ENTITIES, TARGET_BABY_ENTITIES, TARGET_TAMED_ENTITIES,
                 TARGET_UNTAMED_ENTITIES, TARGET_LOW_HEALTH -> { combat.add(kind, p); yield 0; }
            case ATTACK_TARGET -> combat.attack(p, false);
            case ATTACK_UNTIL_DEAD -> combat.attack(p, true);
            case FOLLOW_TARGET, MAINTAIN_ATTACK_DISTANCE -> target() == null || !combat.safe(target(), p) ? failure("Target is invalid or outside combat range") : navigate(target().position(), Math.max(1.5, range(p)), p, true);
            case STOP_ATTACKING -> { memory().remove("target"); memory().remove("filters"); memory().remove("allowTamed"); r.resetNavigation(); yield 0; }
            case DEFEND_PLAYER -> combat.defend(p, true);
            case DEFEND_AREA -> combat.defend(p, false);
            case RETREAT -> {
                if (target() == null) yield 0;
                if (!memory().contains("retreat")) { Vec3 away = d.position().subtract(target().position()); if (away.lengthSqr() < .01) away = new Vec3(1,0,0); Vec3 dest = d.position().add(away.normalize().scale(Math.max(2, range(p)))); memory().putLong("retreat", BlockPos.containing(dest).asLong()); }
                int result = navigate(Vec3.atCenterOf(BlockPos.of(memory().getLong("retreat"))), 1, p, false); if (result >= 0) memory().remove("retreat"); yield result;
            }
            case WAIT_FOR_TARGET -> combat.select(p, true);
            case TARGET_HEALTH -> number(target() == null ? 0 : target().getHealth(), p);
            case DRONE_HEALTH -> number(d.getHealth(), p);
            case TARGET_IS_ALIVE -> condition(target() != null);
            case TARGET_IS_ATTACKING_PLAYER -> { ServerPlayer player = r.player(recipient(p)); yield condition(target() instanceof Mob m && player != null && m.getTarget() == player); }
            case TARGET_IS_ATTACKING_DRONE -> condition(target() instanceof Mob m && m.getTarget() == d);
            case ATTACK_COOLDOWN_READY -> condition(memory().getLong("attackAt") <= r.level().getGameTime());
        };
    }
    private int repeat(AssemblyGraph.Node node) {
        CompoundTag p = node.parameters; String key = "repeat_" + node.id; int count = memory().getInt(key);
        if (p.getInt("count") != 0 && count >= p.getInt("count")) { memory().remove(key); return 0; }
        if (d.graph().node(p.getInt("node")) == null) return failure("Repeat target no longer exists");
        memory().putInt(key, count + 1); d.program.schedule(p.getInt("node")); r.resetNavigation(); return -1;
    }
    private int recall(DroneNode kind, CompoundTag p) {
        boolean home = kind == DroneNode.RECALL_TO_HOME; ServerPlayer player = home ? null : r.player(recipient(p));
        if (!home && player == null) return failure("Recall player is offline or unauthorized");
        CompoundTag before = d.program.save(); d.program.advance(d.graph(), 0);
        String result = r.recall(home ? d.owner : player.getUUID(), home, p.getBoolean("resume"));
        if (!result.isEmpty()) { d.program.load(before); return failure(result); } return -1;
    }
    private int patrol(CompoundTag p) {
        int[] cards=p.getIntArray("waypointSlots");
        ListTag points = p.getList("waypoints", Tag.TAG_INT_ARRAY);
        int size=p.contains("waypointSlots")?cards.length:points.size();
        if(size<1||size>32)return failure("Patrol requires 1 to 32 destinations");
        int index=Math.floorMod(memory().getInt("patrolIndex"),size);int[] v;
        if(p.contains("waypointSlots")){
            CompoundTag bound=memory().getCompound("boundWaypoint");
            if(bound.isEmpty()){bound=DroneDestinations.card(d,cards[index]);memory().put("boundWaypoint",bound.copy());}
            if(!bound.getString("dimension").equals(d.level().dimension().location().toString()))return failure("Patrol destination is in another dimension");
            v=bound.getIntArray("pos");
        }else{memory().put("patrol",points.copy());v=((IntArrayTag)points.get(index)).getAsIntArray();}
        if(v.length!=3)return failure("Waypoint requires three coordinates");
        int result=navigate(Vec3.atCenterOf(new BlockPos(v[0],v[1],v[2])),Math.max(1,range(p)),p,false);
        if(result==0){memory().putInt("patrolIndex",(index+1)%size);memory().remove("boundWaypoint");r.resetNavigation();}
        return result == 2 ? 2 : -1;
    }
}
