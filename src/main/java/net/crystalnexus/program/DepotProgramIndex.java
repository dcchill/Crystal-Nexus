package net.crystalnexus.program;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Small global index so active per-player depot data can be ticked while the owner is offline. */
public final class DepotProgramIndex extends SavedData {
    private static final String ID = "crystalnexus_depot_program_runs";
    private final Set<UUID> owners = new LinkedHashSet<>();

    public static DepotProgramIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(DepotProgramIndex::new, DepotProgramIndex::load), ID);
    }

    private static DepotProgramIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        DepotProgramIndex data = new DepotProgramIndex();
        ListTag list = tag.getList("owners", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try { data.owners.add(UUID.fromString(list.getString(i))); }
            catch (IllegalArgumentException ignored) {}
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        owners.forEach(owner -> list.add(StringTag.valueOf(owner.toString())));
        tag.put("owners", list);
        return tag;
    }

    public Set<UUID> owners() { return Set.copyOf(owners); }
    public void add(UUID owner) { if (owners.add(owner)) setDirty(); }
    public void remove(UUID owner) { if (owners.remove(owner)) setDirty(); }
}
