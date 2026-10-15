package net.crystalnexus.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A self-contained original composition, independent of the beam and remaining blocks. */
public record RecordedMeteor(UUID id, BlockPos base, int diameter, long seed,
                             List<BlockState> palette, List<Integer> weights) {
    public RecordedMeteor {
        palette = List.copyOf(palette);
        weights = List.copyOf(weights);
        if (id == null || base == null || diameter < HeavenshatterMeteor.MIN_DIAMETER
            || diameter > HeavenshatterMeteor.MAX_DIAMETER || palette.size() < 2 || palette.size() > 5
            || weights.size() != palette.size() || weights.stream().anyMatch(weight -> weight <= 0 || weight > 8000)
            || weights.stream().mapToInt(Integer::intValue).sum() > diameter * diameter * diameter
            || palette.stream().anyMatch(state -> state.isAir() || !state.getFluidState().isEmpty()
                || state.hasBlockEntity() || state.getBlock() instanceof FallingBlock))
            throw new IllegalArgumentException("Invalid recorded meteor");
        base = base.immutable();
    }

    public static RecordedMeteor create(BlockPos base, int diameter, long seed, List<BlockState> palette) {
        return new RecordedMeteor(UUID.randomUUID(), base.immutable(), diameter, seed, palette,
            HeavenshatterMeteor.materialCounts(diameter, seed, palette.size()));
    }

    public int totalWeight() { return weights.stream().mapToInt(Integer::intValue).sum(); }

    public BlockState materialAtRoll(int roll) {
        return palette.get(HeavenshatterMeteor.weightedMaterial(weights, roll));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", 1);
        tag.putUUID("id", id);
        tag.putLong("base", base.asLong());
        tag.putInt("diameter", diameter);
        tag.putLong("seed", seed);
        ListTag states = new ListTag();
        for (BlockState state : palette) states.add(NbtUtils.writeBlockState(state));
        tag.put("palette", states);
        tag.putIntArray("weights", weights);
        return tag;
    }

    public static Optional<RecordedMeteor> load(CompoundTag tag) {
        if (tag.getInt("version") != 1 || !tag.hasUUID("id") || !tag.contains("base", Tag.TAG_LONG)
            || !tag.contains("seed", Tag.TAG_LONG) || !tag.contains("diameter", Tag.TAG_INT)
            || !tag.contains("weights", Tag.TAG_INT_ARRAY)) return Optional.empty();
        ListTag states = tag.getList("palette", Tag.TAG_COMPOUND);
        if (states.size() < 2 || states.size() > 5) return Optional.empty();
        try {
            java.util.ArrayList<BlockState> palette = new java.util.ArrayList<>();
            for (int i = 0; i < states.size(); i++) {
                CompoundTag state = states.getCompound(i);
                ResourceLocation name = ResourceLocation.tryParse(state.getString("Name"));
                if (name == null || !BuiltInRegistries.BLOCK.containsKey(name)) return Optional.empty();
                BlockState decoded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), state);
                // Reject unknown properties/values rather than silently simulating a default state.
                if (!NbtUtils.writeBlockState(decoded).equals(state)) return Optional.empty();
                palette.add(decoded);
            }
            return Optional.of(new RecordedMeteor(tag.getUUID("id"), BlockPos.of(tag.getLong("base")),
                tag.getInt("diameter"), tag.getLong("seed"), palette,
                java.util.Arrays.stream(tag.getIntArray("weights")).boxed().toList()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
