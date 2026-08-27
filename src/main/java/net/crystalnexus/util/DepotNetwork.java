package net.crystalnexus.util;

import net.crystalnexus.block.CraftingUpgradeBlock;
import net.crystalnexus.block.CraftingCoreBlock;
import net.crystalnexus.block.DepotCableBlock;
import net.crystalnexus.block.entity.DepotControllerBlockEntity;
import net.crystalnexus.block.entity.DepotCableBlockEntity;
import net.crystalnexus.block.entity.DepotCableConnectionConfig;
import net.crystalnexus.data.DepotSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

public final class DepotNetwork {
    private static final int MAX_CABLES = 4096;
    private static final Map<ServerLevel, Integer> TOPOLOGY_VERSIONS = new WeakHashMap<>();
    private static final Map<ServerLevel, Map<BlockPos, CachedTopology>> TOPOLOGIES = new WeakHashMap<>();

    private DepotNetwork() {
    }

    public record MachineEndpoint(ServerLevel level, BlockPos pos) {
        public MachineEndpoint {
            pos = pos.immutable();
        }
    }

    public static boolean hasCraftingProcessor(ServerPlayer player) {
        return craftingProcessorCount(player) > 0;
    }

    public static int craftingProcessorCount(ServerPlayer player) {
        DepotControllerBlockEntity controller = DepotSavedData.getController(player.serverLevel(), player.getUUID());
        if (controller == null || !controller.isPowered() || !(controller.getLevel() instanceof ServerLevel level)) return 0;
        return count(level, controller.getBlockPos(),
                pos -> level.getBlockState(pos).getBlock() instanceof CraftingUpgradeBlock);
    }

    public record DepotMachineEndpoint(ServerLevel level, BlockPos cablePos, Direction side,
            int priority, DepotCableConnectionConfig config) {
        public DepotMachineEndpoint {
            cablePos = cablePos.immutable();
        }
        public BlockPos machinePos() { return cablePos.relative(side); }
    }

    public record DepotTransferResult(int movedCount) {}

    /** The single Depot -> machine routing operation used by CLI and programs. */
    public static DepotTransferResult routeItemToMachine(ServerPlayer player, DepotSavedData depot,
            ResourceLocation itemId, int amount) {
        Item item = itemId == null ? null : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR || amount <= 0) return new DepotTransferResult(0);
        int remaining = (int) Math.min(amount, depot.getCount(itemId));
        int moved = 0;
        ItemStack filterStack = new ItemStack(item);
        for (DepotMachineEndpoint endpoint : machineEndpoints(player)) {
            if (remaining <= 0) break;
            if (!endpoint.level().hasChunkAt(endpoint.cablePos())
                    || !DepotCableBlock.isDefaultMode(endpoint.level().getBlockState(endpoint.cablePos()))
                    || !endpoint.config().accepts(filterStack)) continue;
            IItemHandler handler = endpoint.level().getCapability(Capabilities.ItemHandler.BLOCK,
                    endpoint.machinePos(), endpoint.side().getOpposite());
            if (handler == null) handler = endpoint.level().getCapability(Capabilities.ItemHandler.BLOCK,
                    endpoint.machinePos(), null);
            if (handler == null) continue;
            int inserted = moveToHandler(depot, handler, itemId, item, remaining);
            moved += inserted;
            remaining -= inserted;
        }
        return new DepotTransferResult(moved);
    }

    /** Automatically exports only the exact items listed on this cable's connected faces. */
    public static int exportListedFromCable(ServerLevel level, BlockPos cablePos, DepotSavedData depot, int limit) {
        if (depot == null || limit <= 0
                || !(level.getBlockEntity(cablePos) instanceof DepotCableBlockEntity cable)) return 0;
        int remaining = limit;
        List<Map.Entry<Direction, DepotCableConnectionConfig>> connections = cable.connections().entrySet().stream()
                .sorted(Map.Entry.<Direction, DepotCableConnectionConfig>comparingByValue(
                        Comparator.comparingInt(DepotCableConnectionConfig::priority).reversed()))
                .toList();
        for (Map.Entry<Direction, DepotCableConnectionConfig> connection : connections) {
            if (remaining <= 0) break;
            Direction side = connection.getKey();
            BlockPos target = cablePos.relative(side);
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, side.getOpposite());
            if (handler == null) handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, null);
            if (handler == null) continue;
            for (ItemStack filter : connection.getValue().itemFilters()) {
                if (remaining <= 0) break;
                if (filter.isEmpty()) continue;
                ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(filter.getItem());
                int moved = moveToHandler(depot, handler, itemId, filter.getItem(), remaining);
                remaining -= moved;
            }
        }
        return limit - remaining;
    }

    private static int moveToHandler(DepotSavedData depot, IItemHandler handler,
            ResourceLocation itemId, Item item, int amount) {
        int remaining = (int) Math.min(amount, depot.getCount(itemId));
        int moved = 0;
        while (remaining > 0) {
            int offered = Math.min(remaining, item.getDefaultMaxStackSize());
            int insertable = offered - ItemHandlerHelper.insertItemStacked(handler,
                    new ItemStack(item, offered), true).getCount();
            if (insertable <= 0) break;
            int extracted = (int) depot.remove(itemId, insertable);
            if (extracted <= 0) break;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler,
                    new ItemStack(item, extracted), false);
            int inserted = extracted - remainder.getCount();
            moved += inserted;
            remaining -= inserted;
            if (!remainder.isEmpty()) depot.addLocal(itemId, remainder.getCount());
            if (inserted <= 0 || inserted < insertable) break;
        }
        return moved;
    }

    private record CachedTopology(int version, List<MachineEndpoint> machines,
            List<DepotMachineEndpoint> endpoints) {}

    public static synchronized void invalidate(ServerLevel level) {
        TOPOLOGY_VERSIONS.merge(level, 1, (oldValue, one) -> oldValue == Integer.MAX_VALUE ? 0 : oldValue + 1);
        TOPOLOGIES.remove(level);
    }

    /** One base process plus one concurrent process for each valid connected core block. */
    public static int craftingJobCapacity(ServerPlayer player) {
        DepotControllerBlockEntity controller = DepotSavedData.getController(player.serverLevel(), player.getUUID());
        if (controller == null || !controller.isPowered() || !(controller.getLevel() instanceof ServerLevel level)
                || craftingProcessorCount(player) <= 0) return 0;
        return 1 + craftingCoreCapacity(level, controller.getBlockPos());
    }

    /** Each core block in a valid cabled horizontal 1x1–2x2 core adds one crafting lane. */
    private static int craftingCoreCapacity(ServerLevel level, BlockPos controllerPos) {
        Set<BlockPos> remaining = collect(level, controllerPos,
                pos -> level.getBlockState(pos).getBlock() instanceof CraftingCoreBlock);
        int capacity = 0;
        while (!remaining.isEmpty()) {
            BlockPos first = remaining.iterator().next();
            Set<BlockPos> cluster = coreCluster(level, first);
            remaining.removeAll(cluster);
            capacity += validCoreCluster(cluster) ? cluster.size() : 0;
        }
        return capacity;
    }

    public static int craftingCoreSize(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof CraftingCoreBlock)) return 0;
        Set<BlockPos> cluster = coreCluster(level, pos);
        return validCoreCluster(cluster) ? cluster.size() : 0;
    }


    private static Set<BlockPos> coreCluster(ServerLevel level, BlockPos pos) {
        Set<BlockPos> cluster = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(pos);
        // Traverse the complete touching cluster. Truncating at five blocks could
        // accidentally split an oversized cluster into a valid 1x1 remainder.
        while (!open.isEmpty() && cluster.size() <= MAX_CABLES) {
            BlockPos current = open.removeFirst();
            if (!cluster.add(current)) continue;
            for (Direction direction : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockPos next = current.relative(direction);
                if (level.hasChunkAt(next) && level.getBlockState(next).getBlock() instanceof CraftingCoreBlock
                        && !cluster.contains(next)) open.add(next);
            }
        }
        return cluster;
    }

    private static boolean validCoreCluster(Set<BlockPos> cluster) {
        if (cluster.isEmpty()) return false;
        int minX = cluster.stream().mapToInt(BlockPos::getX).min().orElse(0);
        int maxX = cluster.stream().mapToInt(BlockPos::getX).max().orElse(0);
        int minZ = cluster.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        int maxZ = cluster.stream().mapToInt(BlockPos::getZ).max().orElse(0);
        return cluster.size() <= 4 && maxX - minX < 2 && maxZ - minZ < 2;
    }

    public static @Nullable UUID componentOwner(ServerLevel level, BlockPos componentPos) {
        UUID[] owner = {null};
        scan(level, componentPos, pos -> {
            if (!(level.getBlockEntity(pos) instanceof DepotControllerBlockEntity controller)
                    || controller.getOwner() == null || !controller.isPowered()) return false;
            if (!DepotSavedData.get(level, controller.getOwner()).isController(level, pos)) return false;
            owner[0] = controller.getOwner();
            return true;
        });
        return owner[0];
    }

    public static int poweredComponentCount(ServerLevel level, BlockPos controllerPos) {
        return collect(level, controllerPos, pos -> level.getBlockState(pos).is(DepotCableBlock.COMPONENTS)).size();
    }

    public static boolean isCraftingProcessorConnected(ServerLevel level, BlockPos upgradePos) {
        return craftingProcessorOwner(level, upgradePos) != null;
    }

    public static @Nullable UUID craftingProcessorOwner(ServerLevel level, BlockPos upgradePos) {
        UUID[] owner = {null};
        scan(level, upgradePos, pos -> {
            if (!(level.getBlockEntity(pos) instanceof DepotControllerBlockEntity controller)
                    || controller.getOwner() == null || !controller.isPowered()) return false;
            if (!DepotSavedData.get(level, controller.getOwner()).isController(level, pos)) return false;
            owner[0] = controller.getOwner();
            return true;
        });
        return owner[0];
    }

    public static boolean isComponentConnected(ServerLevel level, BlockPos componentPos, UUID owner) {
        return owner != null && scan(level, componentPos, pos -> {
            if (!(level.getBlockEntity(pos) instanceof DepotControllerBlockEntity controller)
                    || !owner.equals(controller.getOwner()) || !controller.isPowered()) return false;
            return DepotSavedData.get(level, owner).isController(level, pos);
        });
    }

    public static List<MachineEndpoint> processingMachines(ServerPlayer player) {
        DepotControllerBlockEntity controller = DepotSavedData.getController(player.serverLevel(), player.getUUID());
        if (controller == null || !controller.isPowered() || !(controller.getLevel() instanceof ServerLevel level)) {
            return List.of();
        }
        return topology(level, controller.getBlockPos()).machines();
    }

    public static List<DepotMachineEndpoint> machineEndpoints(ServerPlayer player) {
        DepotControllerBlockEntity controller = DepotSavedData.getController(player.serverLevel(), player.getUUID());
        if (controller == null || !controller.isPowered() || !(controller.getLevel() instanceof ServerLevel level)) {
            return List.of();
        }
        return topology(level, controller.getBlockPos()).endpoints();
    }

    public static List<DepotMachineEndpoint> machineEndpoints(ServerLevel level, BlockPos controllerPos) {
        return topology(level, controllerPos).endpoints();
    }

    private static synchronized CachedTopology topology(ServerLevel level, BlockPos controllerPos) {
        int version = TOPOLOGY_VERSIONS.getOrDefault(level, 0);
        Map<BlockPos, CachedTopology> levelCache = TOPOLOGIES.computeIfAbsent(level, ignored -> new HashMap<>());
        BlockPos key = controllerPos.immutable();
        CachedTopology cached = levelCache.get(key);
        if (cached != null && cached.version() == version) return cached;

        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> machines = new HashSet<>();
        List<DepotMachineEndpoint> endpoints = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos cable = controllerPos.relative(direction);
            if (level.hasChunkAt(cable) && level.getBlockState(cable).getBlock() instanceof DepotCableBlock) {
                open.add(cable);
                visited.add(cable);
            }
        }
        while (!open.isEmpty() && visited.size() <= MAX_CABLES) {
            BlockPos cable = open.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = cable.relative(direction);
                if (!level.hasChunkAt(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof DepotCableBlock) {
                    if (visited.add(next)) open.addLast(next);
                } else if (isMachineConnection(level, cable, direction)) {
                    machines.add(next.immutable());
                    if (level.getBlockEntity(cable) instanceof DepotCableBlockEntity cableEntity) {
                        DepotCableConnectionConfig config = cableEntity.connection(direction);
                        if (config == null) {
                            cableEntity.refreshConnections();
                            config = cableEntity.connection(direction);
                        }
                        if (config != null) endpoints.add(new DepotMachineEndpoint(level, cable, direction,
                                config.priority(), config));
                    }
                }
            }
        }
        List<MachineEndpoint> result = new ArrayList<>(machines.size());
        machines.stream().sorted(Comparator.comparingLong(BlockPos::asLong))
                .forEach(pos -> result.add(new MachineEndpoint(level, pos)));
        endpoints.sort(Comparator.comparingInt(DepotMachineEndpoint::priority).reversed()
                .thenComparingLong(endpoint -> endpoint.cablePos().asLong())
                .thenComparingInt(endpoint -> endpoint.side().ordinal()));
        CachedTopology rebuilt = new CachedTopology(version, List.copyOf(result), List.copyOf(endpoints));
        levelCache.put(key, rebuilt);
        return rebuilt;
    }

    public static boolean isMachineConnection(ServerLevel level, BlockPos cablePos, Direction side) {
        BlockPos target = cablePos.relative(side);
        if (!level.hasChunkAt(target)) return false;
        BlockState state = level.getBlockState(target);
        if (state.getBlock() instanceof DepotCableBlock || state.is(DepotCableBlock.COMPONENTS)) return false;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, target, side.getOpposite()) != null
                || level.getCapability(Capabilities.ItemHandler.BLOCK, target, null) != null;
    }

    public static boolean hasItemHandler(ServerLevel level, BlockPos pos) {
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null) return true;
        for (Direction direction : Direction.values()) {
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction) != null) return true;
        }
        return false;
    }

    private static boolean scan(ServerLevel level, BlockPos start, Predicate<BlockPos> target) {
        return findConnectedBlock(level, start, target) != null;
    }

    /**
     * Finds the first loaded block matching {@code target} next to this depot's
     * cable network. The traversal is bounded in the same way as all other
     * depot network scans.
     */
    public static @Nullable BlockPos findConnectedBlock(ServerLevel level, BlockPos start,
            Predicate<BlockPos> target) {
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        if (level.hasChunkAt(start) && level.getBlockState(start).getBlock() instanceof DepotCableBlock) {
            open.add(start.immutable());
            visited.add(start.immutable());
        }
        for (Direction direction : Direction.values()) {
            BlockPos cable = start.relative(direction);
            if (level.hasChunkAt(cable) && level.getBlockState(cable).getBlock() instanceof DepotCableBlock) {
                open.add(cable);
                visited.add(cable);
            }
        }

        // ponytail: bounded loaded-chunk scan; raise MAX_CABLES only if legitimate networks exceed it.
        while (!open.isEmpty() && visited.size() <= MAX_CABLES) {
            BlockPos pos = open.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!level.hasChunkAt(next)) continue;
                if (target.test(next)) return next.immutable();
                if (level.getBlockState(next).getBlock() instanceof DepotCableBlock && visited.add(next)) open.addLast(next);
            }
        }
        return null;
    }

    private static int count(ServerLevel level, BlockPos start, Predicate<BlockPos> target) {
        return collect(level, start, target).size();
    }

    private static Set<BlockPos> collect(ServerLevel level, BlockPos start, Predicate<BlockPos> target) {
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> targets = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos cable = start.relative(direction);
            if (level.hasChunkAt(cable) && level.getBlockState(cable).getBlock() instanceof DepotCableBlock) {
                open.add(cable);
                visited.add(cable);
            }
        }
        while (!open.isEmpty() && visited.size() <= MAX_CABLES) {
            BlockPos pos = open.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!level.hasChunkAt(next)) continue;
                if (target.test(next)) targets.add(next.immutable());
                if (level.getBlockState(next).getBlock() instanceof DepotCableBlock && visited.add(next)) open.addLast(next);
            }
        }
        return targets;
    }
}
