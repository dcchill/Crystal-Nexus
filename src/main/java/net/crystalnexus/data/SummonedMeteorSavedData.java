package net.crystalnexus.data;

import net.crystalnexus.multiblock.HeavenshatterMeteor;
import net.crystalnexus.multiblock.RecordedMeteor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SummonedMeteorSavedData extends SavedData {
    private final Map<UUID, RecordedMeteor> meteors = new LinkedHashMap<>();
    private record Cell(RecordedMeteor meteor, BlockState state) {}
    private final Map<Long, Cell> cells = new HashMap<>();

    public static SummonedMeteorSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(SummonedMeteorSavedData::new,
            SummonedMeteorSavedData::load), "crystalnexus_summoned_meteors");
    }

    public RecordedMeteor register(BlockPos base, int diameter, long seed, List<BlockState> palette) {
        // A loaded ARRIVED controller must find its existing record, not create a second identity.
        for (RecordedMeteor meteor : meteors.values())
            if (meteor.base().equals(base) && meteor.diameter() == diameter && meteor.seed() == seed
                && meteor.palette().equals(palette)) return meteor;
        RecordedMeteor meteor = RecordedMeteor.create(base, diameter, seed, palette);
        index(meteor);
        setDirty();
        return meteor;
    }

    private void index(RecordedMeteor meteor) {
        meteors.put(meteor.id(), meteor);
        for (var cell : HeavenshatterMeteor.cells(meteor.diameter(), meteor.seed(), meteor.palette().size()))
            cells.put(meteor.base().offset(cell.x(), cell.y(), cell.z()).asLong(),
                new Cell(meteor, meteor.palette().get(cell.material())));
    }

    public RecordedMeteor at(ServerLevel level, BlockPos position) {
        // shortcut: identity uses position/state; track block movement if identical replacements must be excluded.
        Cell cell = cells.get(position.asLong());
        return cell != null && cell.state().equals(level.getBlockState(position)) ? cell.meteor() : null;
    }

    public RecordedMeteor nearest(ServerLevel level, Vec3 position, double range) {
        RecordedMeteor nearest = null;
        double nearestDistance = range * range;
        for (RecordedMeteor meteor : meteors.values()) {
            BlockPos base = meteor.base();
            double distance = new AABB(base.getX(), base.getY(), base.getZ(),
                base.getX() + meteor.diameter(), base.getY() + meteor.diameter(), base.getZ() + meteor.diameter()).distanceToSqr(position);
            if (distance > nearestDistance || (nearest != null && distance == nearestDistance)) continue;
            // Only mark loaded meteors with at least one original block still present.
            for (BlockPos block : BlockPos.betweenClosed(base, base.offset(meteor.diameter() - 1, meteor.diameter() - 1, meteor.diameter() - 1))) {
                Cell cell = cells.get(block.asLong());
                if (cell != null && cell.meteor().id().equals(meteor.id()) && level.hasChunkAt(block)
                    && cell.state().equals(level.getBlockState(block))) {
                    nearest = meteor;
                    nearestDistance = distance;
                    break;
                }
            }
        }
        return nearest;
    }

    public static SummonedMeteorSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SummonedMeteorSavedData data = new SummonedMeteorSavedData();
        ListTag meteors = tag.getList("meteors", Tag.TAG_COMPOUND);
        for (int i = 0; i < meteors.size(); i++) RecordedMeteor.load(meteors.getCompound(i)).ifPresent(data::index);
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag saved = new ListTag();
        for (RecordedMeteor meteor : meteors.values()) saved.add(meteor.save());
        tag.put("meteors", saved);
        return tag;
    }
}
