package net.crystalnexus.data;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DepotSavedData extends SavedData {
    public static final String ID = "crystalnexus_depot";

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
        CompoundTag items = tag.getCompound("items");
        for (String key : items.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) data.counts.put(id, items.getLong(key));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag items = new CompoundTag();
        counts.object2LongEntrySet().forEach(e -> items.putLong(e.getKey().toString(), e.getLongValue()));
        tag.put("items", items);
        return tag;
    } 

    private static String searchKey(ResourceLocation id) {
    return SEARCH_CACHE.computeIfAbsent(id, key -> {
        var item = BuiltInRegistries.ITEM.get(key);
        if (item == null) return (key.getNamespace() + " " + key.getPath()).toLowerCase(Locale.ROOT);

        // Display name is language-dependent; that's fine for "search by name"
        String display = new ItemStack(item).getHoverName().getString();

        // Include namespace for mod searching and registry path as fallback
        return (display + " " + key.getNamespace() + " " + key.getPath()).toLowerCase(Locale.ROOT);
    });
}

    public long getCount(ResourceLocation itemId) {
        return counts.getLong(itemId);
    }

    public void add(ResourceLocation itemId, long amount) {
        if (amount <= 0) return;
        counts.put(itemId, counts.getLong(itemId) + amount);
        setDirty();
    }

    public long remove(ResourceLocation itemId, long amount) {
        if (amount <= 0) return 0;
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

    // Support "@modid" filters (can be combined with text: "@minecraft stone")
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

    // ✅ No lambda: avoids "effectively final" headaches
    for (var e : counts.object2LongEntrySet()) {
        long count = e.getLongValue();
        if (count <= 0) continue;

        ResourceLocation id = e.getKey();

        if (modFilter != null && !id.getNamespace().toLowerCase(Locale.ROOT).contains(modFilter)) {
            continue;
        }

        if (!textFilter.isEmpty()) {
            String key = searchKey(id); // display + namespace + path
            if (!key.contains(textFilter)) continue;
        }

        all.add(new Entry(id, count));
    }

    // Sort by amount desc, then stable by id
    all.sort(Comparator
            .comparingLong(DepotSavedData.Entry::count).reversed()
            .thenComparing(a -> a.itemId().toString()));

    int start = Math.max(0, page) * pageSize;
    if (start >= all.size()) return List.of();

    int end = Math.min(all.size(), start + pageSize);
    return all.subList(start, end);
}

}
