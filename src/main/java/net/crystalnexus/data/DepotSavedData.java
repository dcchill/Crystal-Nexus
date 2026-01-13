package net.crystalnexus.data;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DepotSavedData extends SavedData {
    public static final String ID = "crystalnexus_depot";

    private final Object2LongMap<ResourceLocation> counts = new Object2LongOpenHashMap<>();

    public record Entry(ResourceLocation itemId, long count) {}

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
        String s = (search == null ? "" : search).toLowerCase(Locale.ROOT);

        List<Entry> all = new ArrayList<>();
        counts.object2LongEntrySet().forEach(e -> {
            if (e.getLongValue() <= 0) return;
            String idStr = e.getKey().toString().toLowerCase(Locale.ROOT);
            if (idStr.contains(s)) all.add(new Entry(e.getKey(), e.getLongValue()));
        });

        all.sort(Comparator
            .comparingLong(DepotSavedData.Entry::count).reversed()
            .thenComparing(e -> e.itemId().toString()));


        int start = Math.max(0, page) * pageSize;
        if (start >= all.size()) return List.of();

        int end = Math.min(all.size(), start + pageSize);
        return all.subList(start, end);
    }
}
