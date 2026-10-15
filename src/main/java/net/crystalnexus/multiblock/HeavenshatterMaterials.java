package net.crystalnexus.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class HeavenshatterMaterials {
    private static final TagKey<Block> STONES = TagKey.create(Registries.BLOCK, ResourceLocation.parse("c:stones"));
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, ResourceLocation.parse("crystalnexus:heavenshatter_ores"));
    private static final List<Block> VANILLA_ORES = List.of(Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE,
        Blocks.GOLD_ORE, Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE);

    private HeavenshatterMaterials() {}

    public static boolean eligible(BlockState state, Level level, BlockPos pos) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity()
            && !(state.getBlock() instanceof FallingBlock) && state.getDestroySpeed(level, pos) >= 0
            && state.isCollisionShapeFullBlock(level, pos) && state.isSolidRender(level, pos);
    }

    public static List<BlockState> select(Level level, BlockPos pos, RandomSource random) {
        return select(level, pos, random, List.of(), true);
    }

    public static List<BlockState> select(Level level, BlockPos pos, RandomSource random, List<BlockState> configured) {
        return select(level, pos, random, configured, true);
    }

    public static List<BlockState> select(Level level, BlockPos pos, RandomSource random, List<BlockState> configured, boolean includeStone) {
        List<BlockState> stones = tagged(STONES, level, pos);
        if (!configured.isEmpty() && eligible(configured.getFirst(), level, pos)) {
            stones = new ArrayList<>(stones);
            stones.add(configured.getFirst());
        }
        if (stones.isEmpty()) stones = List.of(Blocks.STONE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState());
        List<BlockState> ores = new ArrayList<>(tagged(ORES, level, pos));
        if (configured.size() > 1) ores.addAll(configured.subList(1, configured.size()).stream().filter(state -> eligible(state, level, pos)).toList());
        ores = new ArrayList<>(ores.stream().distinct().toList());
        if (ores.isEmpty()) ores.addAll(VANILLA_ORES.stream().map(Block::defaultBlockState).toList());
        List<BlockState> palette = new ArrayList<>();
        if (includeStone) {
            BlockState stone = stones.get(random.nextInt(stones.size()));
            palette.add(stone);
            ores.remove(stone);
        }
        int count = Math.min((includeStone ? 1 : 2) + random.nextInt(4), ores.size());
        for (int i = 0; i < count; i++) palette.add(ores.remove(random.nextInt(ores.size())));
        if (palette.size() == 1) palette.add(Blocks.IRON_ORE.defaultBlockState());
        return List.copyOf(palette);
    }

    private static List<BlockState> tagged(TagKey<Block> tag, Level level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getTag(tag).stream().flatMap(set -> set.stream())
            .map(holder -> holder.value().defaultBlockState()).filter(state -> eligible(state, level, pos)).distinct().toList();
    }
}
