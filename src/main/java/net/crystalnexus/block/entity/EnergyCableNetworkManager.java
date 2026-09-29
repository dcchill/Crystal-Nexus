package net.crystalnexus.block.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Level-scoped topology cache for MK2 energy cables. Cable block entities only
 * register lifecycle changes; connected components and external endpoints are
 * rebuilt after topology changes and each component is serviced once per tick.
 */
public final class EnergyCableNetworkManager {
    private static final Map<ServerLevel, LevelNetworks> LEVELS = new WeakHashMap<>();

    private EnergyCableNetworkManager() {}

    public static void register(EnergyCableMk2BlockEntity cable) {
        if (cable.getLevel() instanceof ServerLevel level) state(level).register(cable.getBlockPos());
    }

    public static void unregister(EnergyCableMk2BlockEntity cable) {
        if (cable.getLevel() instanceof ServerLevel level) state(level).unregister(cable.getBlockPos());
    }

    public static void invalidate(ServerLevel level, BlockPos pos) {
        state(level).invalidate(pos);
    }

    public static void tick(ServerLevel level) {
        LevelNetworks networks = LEVELS.get(level);
        if (networks != null) networks.tick();
    }

    public static void unload(ServerLevel level) {
        LEVELS.remove(level);
    }

    public static int receive(EnergyCableMk2BlockEntity cable, int amount, boolean simulate,
                              @Nullable Direction ingress) {
        if (!(cable.getLevel() instanceof ServerLevel level) || amount <= 0
                || ingress != null && !cable.canPullFrom(ingress)) return 0;
        LevelNetworks networks = state(level);
        networks.register(cable.getBlockPos());
        return networks.receive(cable.getBlockPos(), amount, simulate, ingress);
    }

    private static LevelNetworks state(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, LevelNetworks::new);
    }

    private static final class LevelNetworks {
        private final ServerLevel level;
        private final Set<BlockPos> registered = new HashSet<>();
        private final Map<BlockPos, Network> byCable = new HashMap<>();
        private final List<Network> networks = new ArrayList<>();
        private boolean dirty = true;

        private LevelNetworks(ServerLevel level) { this.level = level; }

        private void register(BlockPos pos) {
            if (registered.add(pos.immutable())) dirty = true;
        }

        private void unregister(BlockPos pos) {
            if (registered.remove(pos)) dirty = true;
        }

        private void invalidate(BlockPos pos) {
            // Endpoint capability or adjacency changes can affect the component
            // even when the cable set itself did not change.
            dirty = true;
        }

        private void ensureTopology() {
            if (!dirty) return;
            dirty = false;
            byCable.clear();
            networks.clear();

            registered.removeIf(pos -> level.hasChunkAt(pos)
                    && !(level.getBlockEntity(pos) instanceof EnergyCableMk2BlockEntity));
            Set<BlockPos> remaining = new HashSet<>(registered);
            while (!remaining.isEmpty()) {
                BlockPos start = remaining.iterator().next();
                if (!level.hasChunkAt(start)
                        || !(level.getBlockEntity(start) instanceof EnergyCableMk2BlockEntity)) {
                    remaining.remove(start);
                    continue;
                }
                Set<BlockPos> component = new HashSet<>();
                Set<BlockPos> visited = new HashSet<>();
                ArrayDeque<BlockPos> queue = new ArrayDeque<>();
                queue.add(start);
                visited.add(start);
                remaining.remove(start);
                while (!queue.isEmpty()) {
                    BlockPos pos = queue.removeFirst();
                    component.add(pos);
                    for (Direction direction : Direction.values()) {
                        BlockPos next = pos.relative(direction);
                        if (!visited.contains(next) && level.hasChunkAt(next)
                                && level.getBlockEntity(next) instanceof EnergyCableMk2BlockEntity) {
                            visited.add(next.immutable());
                            remaining.remove(next);
                            queue.addLast(next);
                        }
                    }
                }

                List<Endpoint> endpoints = new ArrayList<>();
                for (BlockPos cablePos : component) {
                    for (Direction direction : Direction.values()) {
                        BlockPos neighbor = cablePos.relative(direction);
                        if (component.contains(neighbor) || !level.hasChunkAt(neighbor)) continue;
                        if (level.getBlockEntity(neighbor) instanceof EnergyCableMk2BlockEntity) continue;
                        endpoints.add(new Endpoint(cablePos, neighbor.immutable(), direction));
                    }
                }
                int componentLimit = component.stream().map(this::cable).filter(java.util.Objects::nonNull)
                    .mapToInt(EnergyCableMk2BlockEntity::maxTransfer).min().orElse(0);
                Network network = new Network(List.copyOf(component), List.copyOf(endpoints), componentLimit);
                networks.add(network);
                component.forEach(pos -> byCable.put(pos, network));
            }
        }

        private void tick() {
            ensureTopology();
            for (Network network : networks) {
                flushLegacy(network);
                pullSources(network);
            }
        }

        private void flushLegacy(Network network) {
            for (BlockPos pos : network.cables) {
                if (!(level.getBlockEntity(pos) instanceof EnergyCableMk2BlockEntity cable)) continue;
                int stored = cable.legacyEnergy();
                if (stored <= 0) continue;
                int moved = route(network, pos, pos, null, stored, false, null);
                if (moved > 0) cable.consumeLegacyEnergy(moved);
            }
        }

        private void pullSources(Network network) {
            int count = network.endpoints.size();
            if (count == 0) return;
            int start = Math.floorMod(network.sourceCursor++, count);
            for (int offset = 0; offset < count; offset++) {
                Endpoint source = network.endpoints.get((start + offset) % count);
                EnergyCableMk2BlockEntity cable = cable(source.cablePos);
                if (cable == null || !cable.canPullFrom(source.cableSide)) continue;
                IEnergyStorage storage = energyAt(source.externalPos, source.externalSide());
                if (storage == null || !storage.canExtract()) continue;
                int offered = storage.extractEnergy(cable.maxTransfer(), true);
                if (offered <= 0) continue;
                route(network, source.cablePos, source.externalPos, source.cableSide, offered, false, source);
            }
        }

        private int receive(BlockPos cablePos, int amount, boolean simulate, @Nullable Direction ingress) {
            ensureTopology();
            Network network = byCable.get(cablePos);
            if (network == null) return 0;
            BlockPos externalSource = ingress == null ? null : cablePos.relative(ingress);
            int moved = route(network, cablePos, externalSource, ingress, amount, simulate, null);
            if (moved > 0 && !simulate && ingress != null) {
                EnergyCableMk2BlockEntity cable = cable(cablePos);
                if (cable != null) cable.markAutomaticInput(ingress);
            }
            return moved;
        }

        private int route(Network network, BlockPos routeOrigin, @Nullable BlockPos sourcePos, @Nullable Direction ingress,
                          int offered, boolean simulate, @Nullable Endpoint source) {
            List<Endpoint> orderedEndpoints = network.endpointsFrom(routeOrigin);
            int count = orderedEndpoints.size();
            if (count == 0) return 0;
            int start = Math.floorMod(network.sinkCursor, count);
            for (int offset = 0; offset < count; offset++) {
                Endpoint sink = orderedEndpoints.get((start + offset) % count);
                if (sink.externalPos.equals(sourcePos)) continue;
                EnergyCableMk2BlockEntity sinkCable = cable(sink.cablePos);
                if (sinkCable == null || !sinkCable.canPushTo(sink.cableSide)) continue;
                IEnergyStorage target = energyAt(sink.externalPos, sink.externalSide());
                if (target == null || !target.canReceive()) continue;
                int limit = Math.min(Math.min(offered, sinkCable.maxTransfer()), network.transferLimit);
                if (source != null) {
                    EnergyCableMk2BlockEntity sourceCable = cable(source.cablePos);
                    if (sourceCable == null) continue;
                    limit = Math.min(limit, sourceCable.maxTransfer());
                }
                int accepted = target.receiveEnergy(limit, true);
                if (accepted <= 0) continue;
                if (simulate) return accepted;

                int moved = accepted;
                if (source != null) {
                    IEnergyStorage sourceStorage = energyAt(source.externalPos, source.externalSide());
                    if (sourceStorage == null || !sourceStorage.canExtract()) continue;
                    moved = sourceStorage.extractEnergy(accepted, false);
                    if (moved <= 0) continue;
                }
                moved = target.receiveEnergy(moved, false);
                if (moved <= 0) continue;
                network.sinkCursor = (start + offset + 1) % count;
                if (source != null) {
                    EnergyCableMk2BlockEntity sourceCable = cable(source.cablePos);
                    if (sourceCable != null) sourceCable.markAutomaticInput(source.cableSide);
                }
                sinkCable.markAutomaticOutput(sink.cableSide);
                return moved;
            }
            return 0;
        }

        private @Nullable EnergyCableMk2BlockEntity cable(BlockPos pos) {
            return level.getBlockEntity(pos) instanceof EnergyCableMk2BlockEntity cable ? cable : null;
        }

        private @Nullable IEnergyStorage energyAt(BlockPos pos, @Nullable Direction preferredSide) {
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, preferredSide);
            if (storage != null) return storage;
            storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            if (storage != null) return storage;
            for (Direction side : Direction.values()) {
                storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
                if (storage != null) return storage;
            }
            return null;
        }
    }

    private static final class Network {
        private final List<BlockPos> cables;
        private final List<Endpoint> endpoints;
        private final int transferLimit;
        private final Map<BlockPos, List<Endpoint>> endpointsByOrigin;
        private int sourceCursor;
        private int sinkCursor;

        private Network(List<BlockPos> cables, List<Endpoint> endpoints, int transferLimit) {
            this.cables = cables;
            this.endpoints = endpoints;
            this.transferLimit = transferLimit;
            this.endpointsByOrigin = new HashMap<>();
            for (BlockPos origin : cables) {
                List<Endpoint> ordered = new ArrayList<>(endpoints);
                ordered.sort(java.util.Comparator
                    .comparingInt((Endpoint endpoint) -> endpoint.cablePos.distManhattan(origin))
                    .thenComparingLong(endpoint -> endpoint.externalPos.asLong())
                    .thenComparingInt(endpoint -> endpoint.cableSide.ordinal()));
                endpointsByOrigin.put(origin, List.copyOf(ordered));
            }
        }

        private List<Endpoint> endpointsFrom(BlockPos origin) {
            return endpointsByOrigin.getOrDefault(origin, endpoints);
        }
    }

    private record Endpoint(BlockPos cablePos, BlockPos externalPos, Direction cableSide) {
        private Direction externalSide() { return cableSide.getOpposite(); }
    }
}
