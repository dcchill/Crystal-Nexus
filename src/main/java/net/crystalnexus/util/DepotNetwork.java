package net.crystalnexus.util;

import net.crystalnexus.block.CraftingUpgradeBlock;
import net.crystalnexus.block.CraftingCoreBlock;
import net.crystalnexus.block.DepotCableBlock;
import net.crystalnexus.block.entity.DepotControllerBlockEntity;
import net.crystalnexus.data.DepotSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

public final class DepotNetwork {
    private static final int MAX_CABLES = 4096;

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
                pos -> level.getBlockState(pos).getBlock() instanceof CraftingUpgradeBlock)
                + craftingCoreCapacity(level, controller.getBlockPos());
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
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> machines = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos cable = controller.getBlockPos().relative(direction);
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
                } else if (!level.getBlockState(next).is(DepotCableBlock.COMPONENTS) && hasItemHandler(level, next)) {
                    machines.add(next.immutable());
                }
            }
        }
        List<MachineEndpoint> result = new ArrayList<>(machines.size());
        machines.stream().sorted(Comparator.comparingLong(BlockPos::asLong))
                .forEach(pos -> result.add(new MachineEndpoint(level, pos)));
        return List.copyOf(result);
    }

    public static boolean hasItemHandler(ServerLevel level, BlockPos pos) {
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null) return true;
        for (Direction direction : Direction.values()) {
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction) != null) return true;
        }
        return false;
    }

    private static boolean scan(ServerLevel level, BlockPos start, Predicate<BlockPos> target) {
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
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
                if (target.test(next)) return true;
                if (level.getBlockState(next).getBlock() instanceof DepotCableBlock && visited.add(next)) open.addLast(next);
            }
        }
        return false;
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
