package net.crystalnexus.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public final class MeteorTrackerSavedData extends SavedData {
	private static final String ID = "crystalnexus_resource_meteors";
	private final Set<Long> meteors = new HashSet<>();

	public static MeteorTrackerSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(
				new SavedData.Factory<>(MeteorTrackerSavedData::new, MeteorTrackerSavedData::load), ID);
	}

	public static MeteorTrackerSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
		MeteorTrackerSavedData data = new MeteorTrackerSavedData();
		for (long position : tag.getLongArray("Meteors")) {
			data.meteors.add(position);
		}
		return data;
	}

	public synchronized void add(BlockPos position) {
		if (this.meteors.add(position.asLong())) {
			this.setDirty();
		}
	}

	public synchronized BlockPos nearest(BlockPos origin) {
		BlockPos nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (long packed : this.meteors) {
			BlockPos candidate = BlockPos.of(packed);
			double distance = origin.distSqr(candidate);
			if (distance < nearestDistance) {
				nearest = candidate;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	@Override
	public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
		tag.putLongArray("Meteors", this.meteors.stream().mapToLong(Long::longValue).toArray());
		return tag;
	}
}
