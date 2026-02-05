package net.crystalnexus.data;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DepotSavedData extends SavedData {
    public static final String ID = "crystalnexus_depot";

    // ===== Capacity / Upgrades =====
    public static final long BASE_CAPACITY = 20480L; // tune this
    private int upgradeLevel = 0;

    // ===== Stored items =====
    private final Object2LongMap<ResourceLocation> counts = new Object2LongOpenHashMap<>();

    public record Entry(ResourceLocation itemId, long count) {}
    private static final Map<ResourceLocation, String> SEARCH_CACHE = new ConcurrentHashMap<>();

    public static DepotSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DepotSavedData::new, DepotSavedData::load),
                ID
        );
    }

    public static DepotSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        DepotSavedData data = new DepotSavedData();

        data.upgradeLevel = tag.getInt("upgradeLevel");

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

        CompoundTag items = new CompoundTag();
        counts.object2LongEntrySet().forEach(e -> items.putLong(e.getKey().toString(), e.getLongValue()));
        tag.put("items", items);

        return tag;
    }

    // ===== Capacity helpers =====

    public int getUpgradeLevel() {
        return upgradeLevel;
    }

    /** Doubles capacity per upgrade (BASE * 2^upgradeLevel). */
    public long getCapacity() {
        if (upgradeLevel >= 62) return Long.MAX_VALUE / 2; // overflow safety
        return BASE_CAPACITY << upgradeLevel;
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

    /** Consumes an upgrade (your item handles shrinking). */
    public void addUpgrade() {
        upgradeLevel++;
        setDirty();
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

    /**
     * SAFE deposit method: respects capacity.
     * @return how many were accepted (0..amount)
     */
    public long deposit(ResourceLocation itemId, long amount) {
        if (amount <= 0) return 0;
        if (itemId == null) return 0;

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

        int start = Math.max(0, page) * pageSize;
        if (start >= all.size()) return List.of();

        int end = Math.min(all.size(), start + pageSize);
        return all.subList(start, end);
    }
}
