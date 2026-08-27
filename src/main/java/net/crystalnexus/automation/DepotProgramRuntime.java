package net.crystalnexus.automation;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.cli.DepotCraftingService;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class DepotProgramRuntime {
    public static final int MAX_TRANSACTION_ACTIONS = 64;
    public static final int MAX_NETWORK_ACTIONS_PER_TICK = 100;
    private static final Map<DepotSavedData, RuntimeState> STATES = new WeakHashMap<>();
    private static final ThreadLocal<ExecutionContext> CURRENT = new ThreadLocal<>();

    private DepotProgramRuntime() {}

    private record ExecutionContext(DepotSavedData depot, UUID transactionId, UUID programId) {}
    private static final class TransactionState {
        int actions;
        final Set<UUID> executedPrograms = new HashSet<>();
    }
    private static final class RuntimeState {
        final ArrayDeque<DepotEvent> events = new ArrayDeque<>();
        final Map<UUID, TransactionState> transactions = new HashMap<>();
        final Map<UUID, Long> lastTimedTriggers = new HashMap<>();
        long lastTimedCheckTick = 0;
    }

    public static synchronized void itemChanged(DepotSavedData depot, ResourceLocation itemId, long delta) {
        if (depot == null || itemId == null || delta == 0) return;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) return;
        ExecutionContext current = CURRENT.get();
        UUID transactionId = current != null && current.depot() == depot ? current.transactionId() : UUID.randomUUID();
        UUID source = current != null && current.depot() == depot ? current.programId() : null;
        ItemStack stack = new ItemStack(item, (int) Math.min(Integer.MAX_VALUE, Math.abs(delta)));
        RuntimeState state = STATES.computeIfAbsent(depot, ignored -> new RuntimeState());
        state.events.addLast(new DepotEvent(delta > 0 ? DepotEvent.Type.ITEM_ADDED : DepotEvent.Type.ITEM_REMOVED,
                stack, transactionId, source));
        state.events.addLast(new DepotEvent(DepotEvent.Type.INVENTORY_CHANGED, stack, transactionId, source));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) process(player, DepotSavedData.get(player));
    }

    public static synchronized int process(ServerPlayer player, DepotSavedData depot) {
        RuntimeState state = STATES.get(depot);
        if (state == null) return 0;
        int tickActions = 0;
        long currentTick = player.getServer().getTickCount();

        // Process timed interval triggers
        if (currentTick - state.lastTimedCheckTick >= 1) {
            state.lastTimedCheckTick = currentTick;
            for (DepotProgram program : depot.getPrograms()) {
                if (program.trigger().type() != DepotProgram.TriggerType.TIMED_INTERVAL || !program.enabled()) continue;
                long lastTrigger = state.lastTimedTriggers.getOrDefault(program.id(), 0L);
                if (currentTick - lastTrigger >= program.trigger().interval()) {
                    if (conditionsPass(program, depot)) {
                        state.lastTimedTriggers.put(program.id(), currentTick);
                        for (DepotProgram.ProgramAction action : program.actions()) {
                            if (tickActions >= MAX_NETWORK_ACTIONS_PER_TICK) break;
                            tickActions++;
                            CURRENT.set(new ExecutionContext(depot, UUID.randomUUID(), program.id()));
                            try { execute(player, depot, action); }
                            finally { CURRENT.remove(); }
                        }
                    }
                }
            }
        }

        // Process event-based triggers
        if (state.events.isEmpty()) return tickActions;
        while (!state.events.isEmpty() && tickActions < MAX_NETWORK_ACTIONS_PER_TICK) {
            DepotEvent event = state.events.removeFirst();
            TransactionState transaction = state.transactions.computeIfAbsent(event.transactionId(), ignored -> new TransactionState());
            for (DepotProgram program : depot.getPrograms()) {
                if (program.trigger().type() == DepotProgram.TriggerType.TIMED_INTERVAL) continue;
                if (!program.enabled() || transaction.executedPrograms.contains(program.id()) || !triggered(program, event)
                        || !conditionsPass(program, depot)) continue;
                transaction.executedPrograms.add(program.id());
                for (DepotProgram.ProgramAction action : program.actions()) {
                    if (transaction.actions >= MAX_TRANSACTION_ACTIONS || tickActions >= MAX_NETWORK_ACTIONS_PER_TICK) break;
                    transaction.actions++;
                    tickActions++;
                    CURRENT.set(new ExecutionContext(depot, event.transactionId(), program.id()));
                    try { execute(player, depot, action); }
                    finally { CURRENT.remove(); }
                }
            }
        }
        Set<UUID> pending = new HashSet<>();
        state.events.forEach(event -> pending.add(event.transactionId()));
        state.transactions.keySet().retainAll(pending);
        if (state.events.isEmpty() && tickActions == 0) STATES.remove(depot);
        return tickActions;
    }

    private static boolean triggered(DepotProgram program, DepotEvent event) {
        return switch (program.trigger().type()) {
            case ITEM_ADDED -> event.type() == DepotEvent.Type.ITEM_ADDED
                    && program.trigger().itemId().equals(BuiltInRegistries.ITEM.getKey(event.stack().getItem()));
            case INVENTORY_CHANGED -> event.type() == DepotEvent.Type.INVENTORY_CHANGED;
            case TIMED_INTERVAL -> false; // handled separately in process()
        };
    }

    private static boolean conditionsPass(DepotProgram program, DepotSavedData depot) {
        for (DepotProgram.ProgramCondition condition : program.conditions()) {
            long count = depot.getCount(condition.itemId());
            boolean passed = switch (condition.type()) {
                case COUNT_AT_LEAST -> count >= condition.amount();
                case COUNT_LESS -> count < condition.amount();
                case EXISTS -> count > 0;
                case MISSING -> count == 0;
            };
            if (!passed) return false;
        }
        return true;
    }

    private static void execute(ServerPlayer player, DepotSavedData depot, DepotProgram.ProgramAction action) {
        Item item = BuiltInRegistries.ITEM.get(action.itemId());
        if (item == null || item == Items.AIR) return;
        switch (action.type()) {
            case SEND_ITEM -> DepotNetwork.routeItemToMachine(player, depot, action.itemId(), action.amount());
            case CRAFT -> {
                boolean duplicate = depot.getCraftingJobs().stream().anyMatch(job -> job.targetId().equals(action.itemId()));
                if (!duplicate) DepotCraftingService.craft(player, depot, item, action.amount());
            }
        }
    }
}
