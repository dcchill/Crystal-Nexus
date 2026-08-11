package net.crystalnexus.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.block.DepotCableBlock;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.integration.DepotStorageBridge;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Optional AE2 integration. This class is only loaded when AE2 is present. */
public final class Ae2DepotIntegration {
    private static final Map<DepotSavedData, Ae2StorageBridge> BRIDGES = new WeakHashMap<>();

    private Ae2DepotIntegration() {
    }

    public static void sync(ServerLevel level, BlockPos depotControllerPos, DepotSavedData depot) {
        BlockPos ae2ControllerPos = DepotNetwork.findConnectedBlock(level, depotControllerPos,
                pos -> DepotCableBlock.isAe2Controller(level.getBlockState(pos)));
        IGridNode node = ae2ControllerPos == null ? null : findActiveNode(level, ae2ControllerPos);
        if (node == null) {
            disconnect(depot);
            return;
        }

        Ae2StorageBridge bridge = BRIDGES.get(depot);
        if (bridge == null || !bridge.isFor(node)) {
            bridge = new Ae2StorageBridge(node);
            BRIDGES.put(depot, bridge);
            depot.setStorageBridge(bridge);
        } else if (!depot.hasStorageBridge(bridge)) {
            depot.setStorageBridge(bridge);
        }

        // Empty the local depot buffer into AE2 over time. Anything AE2 cannot
        // currently accept stays local and remains visible to the depot.
        for (DepotSavedData.Entry entry : depot.localEntries()) {
            if (!depot.accepts(entry.itemId())) continue;
            long inserted = bridge.insert(entry.itemId(), entry.count());
            if (inserted > 0) depot.removeLocal(entry.itemId(), inserted);
        }
    }

    public static void disconnect(DepotSavedData depot) {
        BRIDGES.remove(depot);
        depot.setStorageBridge(null);
    }

    private static IGridNode findActiveNode(ServerLevel level, BlockPos controllerPos) {
        IInWorldGridNodeHost host = GridHelper.getNodeHost(level, controllerPos);
        if (host == null) return null;
        for (Direction side : Direction.values()) {
            IGridNode node = host.getGridNode(side);
            if (node != null && node.isActive()) return node;
        }
        return null;
    }

    private static final class Ae2StorageBridge implements DepotStorageBridge {
        private final IGridNode node;

        private Ae2StorageBridge(IGridNode node) {
            this.node = node;
        }

        private boolean isFor(IGridNode candidate) {
            try {
                if (!isConnected() || candidate == null || !candidate.isActive()) return false;
                return node.getGrid() == candidate.getGrid();
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        @Override
        public boolean isConnected() {
            try {
                return node.isActive();
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        @Override
        public long getCount(ResourceLocation itemId) {
            AEItemKey key = itemKey(itemId);
            if (key == null || !isConnected()) return 0L;
            try {
                return node.getGrid().getStorageService().getCachedInventory().get(key);
            } catch (RuntimeException ignored) {
                return 0L;
            }
        }

        @Override
        public long insert(ResourceLocation itemId, long amount) {
            AEItemKey key = itemKey(itemId);
            if (key == null || amount <= 0 || !isConnected()) return 0L;
            try {
                IGrid grid = node.getGrid();
                MEStorage storage = grid.getStorageService().getInventory();
                return StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, amount,
                        IActionSource.empty(), Actionable.MODULATE);
            } catch (RuntimeException exception) {
                return 0L;
            }
        }

        @Override
        public long extract(ResourceLocation itemId, long amount) {
            AEItemKey key = itemKey(itemId);
            if (key == null || amount <= 0 || !isConnected()) return 0L;
            try {
                IGrid grid = node.getGrid();
                MEStorage storage = grid.getStorageService().getInventory();
                return StorageHelper.poweredExtraction(grid.getEnergyService(), storage, key, amount,
                        IActionSource.empty(), Actionable.MODULATE);
            } catch (RuntimeException exception) {
                return 0L;
            }
        }

        @Override
        public Map<ResourceLocation, Long> snapshot() {
            Map<ResourceLocation, Long> result = new HashMap<>();
            if (!isConnected()) return result;
            try {
                for (var stack : node.getGrid().getStorageService().getCachedInventory()) {
                    if (!(stack.getKey() instanceof AEItemKey itemKey) || itemKey.hasComponents()
                            || stack.getLongValue() <= 0) continue;
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemKey.getItem());
                    if (itemId != null) {
                        result.merge(itemId, stack.getLongValue(), Ae2DepotIntegration::saturatedAdd);
                    }
                }
            } catch (RuntimeException exception) {
                CrystalnexusMod.LOGGER.debug("AE2 depot storage became unavailable during synchronization", exception);
            }
            return result;
        }

        private static AEItemKey itemKey(ResourceLocation itemId) {
            if (itemId == null) return null;
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == Items.AIR && !itemId.equals(BuiltInRegistries.ITEM.getKey(Items.AIR))) {
                return null;
            }
            return AEItemKey.of(item);
        }
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }
}
