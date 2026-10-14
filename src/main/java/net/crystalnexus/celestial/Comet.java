package net.crystalnexus.celestial;

import net.crystalnexus.multiblock.HeavenshatterMeteor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public record Comet(UUID id, ResourceLocation classification, String name, int difficulty, int azimuth, int elevation,
                    int frequency, int distance, int diameter, long seed, List<BlockState> palette, List<Integer> counts) {
    public Comet { palette = List.copyOf(palette); counts = List.copyOf(counts); }
    public static Comet create(UUID id, CometDefinitions.Definition d, int azimuth, int elevation, int frequency, int distance, int diameter, long seed) {
        int[] counts = new int[d.palette().size()];
        for (var cell : HeavenshatterMeteor.cells(diameter, seed, counts.length)) counts[cell.material()]++;
        return new Comet(id, d.id(), d.name(), d.difficulty(), azimuth, elevation, frequency, distance, diameter, seed,
            d.palette(), Arrays.stream(counts).boxed().toList());
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id); tag.putString("type", classification.toString()); tag.putString("name", name);
        tag.putInt("difficulty", difficulty); tag.putInt("azimuth", azimuth); tag.putInt("elevation", elevation);
        tag.putInt("frequency", frequency); tag.putInt("distance", distance); tag.putInt("diameter", diameter); tag.putLong("seed", seed);
        ListTag blocks = new ListTag();
        palette.forEach(s -> blocks.add(NbtUtils.writeBlockState(s))); tag.put("palette", blocks);
        return tag;
    }
    public static Comet load(CompoundTag tag, HolderLookup.Provider registries) {
        List<BlockState> palette = new ArrayList<>();
        ListTag blocks = tag.getList("palette", Tag.TAG_COMPOUND);
        if (blocks.size() < 2 || blocks.size() > 5) throw new IllegalArgumentException("Invalid saved palette");
        for (int i = 0; i < blocks.size(); i++) palette.add(NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), blocks.getCompound(i)));
        int diameter = Math.clamp(tag.getInt("diameter"), 7, 14);
        var definition = new CometDefinitions.Definition(ResourceLocation.parse(tag.getString("type")), tag.getString("name"), 1,
            Math.clamp(tag.getInt("difficulty"), 1, 3), diameter, diameter, 1, 10000, palette);
        return create(tag.getUUID("id"), definition, Math.floorMod(tag.getInt("azimuth"), 360), Math.clamp(tag.getInt("elevation"), 0, 90),
            Math.clamp(tag.getInt("frequency"), 0, 100), Math.clamp(tag.getInt("distance"), 1, 10000), diameter, tag.getLong("seed"));
    }
    public CompoundTag information() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name); tag.putString("type", classification.toString()); tag.putInt("distance", distance);
        tag.putInt("mass", counts.stream().mapToInt(Integer::intValue).sum());
        tag.putInt("yield", counts.stream().skip(1).mapToInt(Integer::intValue).sum());
        ListTag materials = new ListTag();
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag material = new CompoundTag();
            material.putString("block", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(palette.get(i).getBlock()).toString());
            material.putInt("count", counts.get(i)); materials.add(material);
        }
        tag.put("materials", materials);
        return tag;
    }
}
