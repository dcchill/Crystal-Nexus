package net.crystalnexus.data;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import net.crystalnexus.block.entity.DepotControllerBlockEntity;
import net.crystalnexus.config.CrystalnexusConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

public class DepotSavedData extends SavedData {
    public static final String ID = "crystalnexus_depot";
    public static final int MAX_UPGRADE_LEVEL = 62;
    private static final ResourceLocation UPLINK_ID = ResourceLocation.fromNamespaceAndPath("crystalnexus", "depot_uplink");

    // ===== Capacity / Upgrades =====
    private int upgradeLevel = 0;
    private ResourceLocation controllerDimension;
    private BlockPos controllerPos;

    // ===== Stored items =====
    private final Object2LongMap<ResourceLocation> counts = new Object2LongOpenHashMap<>();

    public record Entry(ResourceLocation itemId, long count) {}
    private static final Map<ResourceLocation, String> SEARCH_CACHE = new ConcurrentHashMap<>();

    public static DepotSavedData get(ServerPlayer player) {
        return get(player.serverLevel(), player.getUUID());
    }

    public static DepotSavedData get(ServerLevel level, UUID playerId) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DepotSavedData::new, DepotSavedData::load),
                ID + "_" + playerId
        );
    }

    public static DepotSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        DepotSavedData data = new DepotSavedData();

        data.upgradeLevel = Math.max(0, Math.min(MAX_UPGRADE_LEVEL, tag.getInt("upgradeLevel")));
        data.controllerDimension = ResourceLocation.tryParse(tag.getString("ControllerDimension"));
        if (data.controllerDimension != null && tag.contains("ControllerPos")) {
            data.controllerPos = BlockPos.of(tag.getLong("ControllerPos"));
        }

        CompoundTag items = tag.getCompound("items");
        for (String key : items.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) data.counts.put(id, items.getLong(key));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("upgradeLevel", upgradeLevel);
        if (controllerDimension != null && controllerPos != null) {
            tag.putString("ControllerDimension", controllerDimension.toString());
            tag.putLong("ControllerPos", controllerPos.asLong());
        }

        CompoundTag items = new CompoundTag();
        counts.object2LongEntrySet().forEach(e -> items.putLong(e.getKey().toString(), e.getLongValue()));
        tag.put("items", items);

        return tag;
    }

    // ===== Capacity helpers =====

    public int getUpgradeLevel() {
        return upgradeLevel;
    }

    public void setController(ServerLevel level, BlockPos pos) {
        controllerDimension = level.dimension().location();
        controllerPos = pos.immutable();
        setDirty();
    }

    public void setControllerIfAbsent(ServerLevel level, BlockPos pos) {
        if (controllerDimension == null || controllerPos == null) setController(level, pos);
    }

    public void clearController(ServerLevel level, BlockPos pos) {
        if (isController(level, pos)) {
            controllerDimension = null;
            controllerPos = null;
            setDirty();
        }
    }

    public boolean isController(ServerLevel level, BlockPos pos) {
        return controllerDimension != null && controllerPos != null
                && controllerDimension.equals(level.dimension().location()) && controllerPos.equals(pos);
    }

    public static boolean hasPoweredController(ServerPlayer player) {
        return hasPoweredController(player.serverLevel(), player.getUUID());
    }

    public static boolean hasPoweredController(ServerLevel level, UUID playerId) {
        DepotControllerBlockEntity controller = getController(level, playerId);
        return controller != null && controller.isPowered();
    }

    public static @Nullable DepotControllerBlockEntity getController(ServerLevel level, UUID playerId) {
        DepotSavedData data = get(level, playerId);
        if (data.controllerDimension == null || data.controllerPos == null) return null;
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, data.controllerDimension);
        ServerLevel controllerLevel = level.getServer().getLevel(dimension);
        if (controllerLevel == null || !controllerLevel.hasChunkAt(data.controllerPos)) return null;
        if (controllerLevel.getBlockEntity(data.controllerPos) instanceof DepotControllerBlockEntity controller
                && playerId.equals(controller.getOwner())) return controller;
        return null;
    }

    public static boolean requirePoweredController(ServerPlayer player) {
        if (hasPoweredController(player)) return true;
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Your Depot Controller is missing, unloaded, or out of power.")
                .withStyle(net.minecraft.ChatFormatting.RED), true);
        return false;
    }

    /** Doubles capacity per upgrade (BASE * 2^upgradeLevel). */
    public long getCapacity() {
        long baseCapacity = CrystalnexusConfig.MACHINES.depotBaseCapacity();
        if (upgradeLevel >= 63 || baseCapacity > (Long.MAX_VALUE >> upgradeLevel)) return Long.MAX_VALUE;
        return baseCapacity << upgradeLevel;
    }

    /** Total items stored (sum of all counts). */
    public long getUsed() {
        long sum = 0L;
        for (var e : counts.object2LongEntrySet()) {
            long v = e.getLongValue();
            if (v > 0) sum += v;
        }
        return sum;
    }

    public long getFree() {
        long free = getCapacity() - getUsed();
        return Math.max(0L, free);
    }

    public boolean canInsert(long amount) {
        if (amount <= 0) return true;
        return amount <= getFree();
    }

    /** @return whether the depot was upgraded. */
    public boolean addUpgrade() {
        if (upgradeLevel >= MAX_UPGRADE_LEVEL || getCapacity() == Long.MAX_VALUE) return false;
        upgradeLevel++;
        setDirty();
        return true;
    }

    private static String searchKey(ResourceLocation id) {
        return SEARCH_CACHE.computeIfAbsent(id, key -> {
            var item = BuiltInRegistries.ITEM.get(key);
            if (item == null) return (key.getNamespace() + " " + key.getPath()).toLowerCase(Locale.ROOT);

            String display = new ItemStack(item).getHoverName().getString();
            return (display + " " + key.getNamespace() + " " + key.getPath()).toLowerCase(Locale.ROOT);
        });
    }

    // ===== Storage API (SAFE) =====

    public long getCount(ResourceLocation itemId) {
        return counts.getLong(itemId);
    }

    public void fillStackedContents(StackedContents contents) {
        counts.object2LongEntrySet().forEach(entry -> {
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            if (item == null || item == net.minecraft.world.item.Items.AIR || entry.getLongValue() <= 0) return;
            ItemStack stack = new ItemStack(item);
            stack.setCount((int) Math.min(Integer.MAX_VALUE, entry.getLongValue()));
            contents.accountStack(stack, Integer.MAX_VALUE);
        });
    }

    /**
     * SAFE deposit method: respects capacity.
     * @return how many were accepted (0..amount)
     */
    public long deposit(ResourceLocation itemId, long amount) {
        if (amount <= 0) return 0;
        if (itemId == null || itemId.equals(UPLINK_ID)) return 0;

        long free = getFree();
        long toAdd = Math.min(free, amount);
        if (toAdd <= 0) return 0;

        counts.put(itemId, counts.getLong(itemId) + toAdd);
        setDirty();
        return toAdd;
    }

    /**
     * SAFE deposit for ItemStack count.
     * Uses the registry id of the stack item.
     * @return how many items were accepted
     */
    public long depositStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return 0;
        return deposit(id, stack.getCount());
    }

    /**
     * Convenience: tries to deposit as much of the stack as possible and shrinks it by accepted amount.
     * @return accepted amount
     */
    public long tryDepositAll(ItemStack stack) {
        long accepted = depositStack(stack);
        if (accepted > 0) {
            stack.shrink((int) accepted);
        }
        return accepted;
    }

    // Backward compatibility: keep addCapped name if other code calls it
    public long addCapped(ResourceLocation itemId, long amount) {
        return deposit(itemId, amount);
    }

    // ===== Storage API (UNSAFE) =====

    /**
     * UNSAFE: ignores capacity. Only use for admin/debug/migrations.
     */
    public void add(ResourceLocation itemId, long amount) {
        if (amount <= 0) return;
        if (itemId == null) return;
        counts.put(itemId, counts.getLong(itemId) + amount);
        setDirty();
    }

    public long remove(ResourceLocation itemId, long amount) {
        if (amount <= 0) return 0;
        if (itemId == null) return 0;

        long have = counts.getLong(itemId);
        long take = Math.min(have, amount);
        if (take <= 0) return 0;

        long left = have - take;
        if (left <= 0) counts.removeLong(itemId);
        else counts.put(itemId, left);

        setDirty();
        return take;
    }

    public List<Entry> page(String search, int page, int pageSize) {
        List<Entry> all = filteredEntries(search);
        int start = Math.max(0, page);
        if (start >= all.size()) return List.of();
        return all.subList(start, Math.min(all.size(), start + pageSize));
    }

    public int countEntries(String search) {
        return filteredEntries(search).size();
    }

    private List<Entry> filteredEntries(String search) {
        String raw = (search == null ? "" : search).trim().toLowerCase(Locale.ROOT);

        String modFilter = null;
        String textFilter = raw;

        if (raw.contains("@")) {
            String[] parts = raw.split("\\s+");
            StringBuilder rest = new StringBuilder();
            for (String p : parts) {
                if (p.startsWith("@") && p.length() > 1 && modFilter == null) {
                    modFilter = p.substring(1);
                } else if (!p.isBlank()) {
                    if (rest.length() > 0) rest.append(' ');
                    rest.append(p);
                }
            }
            textFilter = rest.toString();
        }

        List<Entry> all = new ArrayList<>();

        for (var e : counts.object2LongEntrySet()) {
            long count = e.getLongValue();
            if (count <= 0) continue;

            ResourceLocation id = e.getKey();

            if (modFilter != null && !id.getNamespace().toLowerCase(Locale.ROOT).contains(modFilter)) {
                continue;
            }

            if (!textFilter.isEmpty()) {
                String key = searchKey(id);
                if (!key.contains(textFilter)) continue;
            }

            all.add(new Entry(id, count));
        }

        all.sort(Comparator
                .comparingLong(DepotSavedData.Entry::count).reversed()
                .thenComparing(a -> a.itemId().toString()));
        return all;
    }
}
