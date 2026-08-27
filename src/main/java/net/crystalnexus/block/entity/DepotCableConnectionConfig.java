package net.crystalnexus.block.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class DepotCableConnectionConfig {
    public enum FilterMode { ALLOW_LISTED, BLOCK_LISTED }

    public static final int FILTER_SLOTS = 9;
    private final Direction side;
    private int priority;
    private FilterMode filterMode = FilterMode.BLOCK_LISTED;
    private final List<ItemStack> itemFilters = new ArrayList<>(FILTER_SLOTS);

    public DepotCableConnectionConfig(Direction side) {
        this.side = side;
        for (int i = 0; i < FILTER_SLOTS; i++) itemFilters.add(ItemStack.EMPTY);
    }

    public Direction side() { return side; }
    public int priority() { return priority; }
    public FilterMode filterMode() { return filterMode; }
    public List<ItemStack> itemFilters() { return List.copyOf(itemFilters); }

    public void setPriority(int priority) { this.priority = Math.clamp(priority, -100, 100); }
    public void setFilterMode(FilterMode filterMode) {
        this.filterMode = filterMode == null ? FilterMode.BLOCK_LISTED : filterMode;
    }
    public void setFilter(int slot, ItemStack stack) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!copy.isEmpty()) copy.setCount(1);
        itemFilters.set(slot, copy);
    }

    public boolean accepts(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        boolean listed = itemFilters.stream().anyMatch(filter -> !filter.isEmpty()
                && ItemStack.isSameItem(filter, stack));
        return filterMode == FilterMode.ALLOW_LISTED ? listed : !listed;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("side", side.getSerializedName());
        tag.putInt("priority", priority);
        tag.putString("filterMode", filterMode.name());
        ListTag filters = new ListTag();
        for (ItemStack filter : itemFilters) filters.add(filter.saveOptional(registries));
        tag.put("filters", filters);
        return tag;
    }

    public static DepotCableConnectionConfig load(CompoundTag tag, HolderLookup.Provider registries) {
        Direction side = Direction.byName(tag.getString("side"));
        if (side == null) return null;
        DepotCableConnectionConfig config = new DepotCableConnectionConfig(side);
        config.setPriority(tag.getInt("priority"));
        try {
            config.setFilterMode(FilterMode.valueOf(tag.getString("filterMode")));
        } catch (IllegalArgumentException ignored) {
            config.setFilterMode(FilterMode.BLOCK_LISTED);
        }
        ListTag filters = tag.getList("filters", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(filters.size(), FILTER_SLOTS); i++) {
            config.setFilter(i, ItemStack.parseOptional(registries, filters.getCompound(i)));
        }
        return config;
    }
}
