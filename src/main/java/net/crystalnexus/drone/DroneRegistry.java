package net.crystalnexus.drone;

import net.crystalnexus.entity.LUNADroneEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Last known locations let remote controls report unloaded drones without loading chunks. */
public final class DroneRegistry extends SavedData {
    public record Entry(UUID id, UUID owner, String dimension) {}
    public final Map<UUID, Entry> entries = new HashMap<>();
    public static DroneRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(DroneRegistry::new, DroneRegistry::load, null), "crystalnexus_drones");
    }
    private static DroneRegistry load(CompoundTag p, HolderLookup.Provider lookup) {
        DroneRegistry r = new DroneRegistry();
        for (Tag raw : p.getList("drones", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag)raw;
            if (t.hasUUID("id") && t.hasUUID("owner")) {
                Entry e = new Entry(t.getUUID("id"), t.getUUID("owner"), t.getString("dimension")); r.entries.put(e.id, e);
            }
        }
        return r;
    }
    public void track(LUNADroneEntity drone) {
        if (drone.owner == null) return;
        Entry next = new Entry(drone.getUUID(), drone.owner, drone.level().dimension().location().toString());
        if (!next.equals(entries.put(next.id, next))) setDirty();
    }
    public void forget(UUID id) { if (entries.remove(id) != null) setDirty(); }
    public static LUNADroneEntity loaded(MinecraftServer server, UUID id) {
        for (var level : server.getAllLevels()) if (level.getEntity(id) instanceof LUNADroneEntity d && d.isAlive()) return d;
        return null;
    }
    @Override public CompoundTag save(CompoundTag p, HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) { CompoundTag t = new CompoundTag(); t.putUUID("id", e.id); t.putUUID("owner", e.owner); t.putString("dimension", e.dimension); list.add(t); }
        p.put("drones", list); return p;
    }
}
