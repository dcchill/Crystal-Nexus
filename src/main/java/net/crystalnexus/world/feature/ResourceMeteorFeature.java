package net.crystalnexus.world.feature;

import com.mojang.serialization.Codec;
import net.crystalnexus.data.MeteorTrackerSavedData;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class ResourceMeteorFeature extends Feature<NoneFeatureConfiguration> {
    private static final int CRATER_RADIUS = 16;
    private static final int CRATER_DEPTH = 6;
    private static final BlockState[] ORES = {
            Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(), Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),
            Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState(), Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState(),
            Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(), Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState(),
            Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState(), Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState(),
            CrystalnexusModBlocks.DEEPSLATE_SILICON_ORE.get().defaultBlockState(),
            CrystalnexusModBlocks.DEEPSLATE_ILMENITE_ORE.get().defaultBlockState()
    };

    public ResourceMeteorFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1;
        BlockPos surface = new BlockPos(origin.getX(), surfaceY, origin.getZ());
        BlockState surfaceState = level.getBlockState(surface);
        if (surfaceY <= level.getMinBuildHeight() || surfaceState.isAir() || !surfaceState.getFluidState().isEmpty()
                || !surfaceState.isSolidRender(level, surface)) return false;

        BlockPos center = new BlockPos(origin.getX(), surfaceY - 2, origin.getZ());
        carveCrater(level, center);
        int scraps = 1 + random.nextInt(3);
        for (int x = -5; x <= 5; x++) for (int y = -5; y <= 5; y++) for (int z = -5; z <= 5; z++) {
            if (x * x + y * y + z * z > 25) continue;
            BlockPos pos = center.offset(x, y, z);
            BlockState state = Blocks.DEEPSLATE.defaultBlockState();
            if (scraps > 0 && random.nextFloat() < 0.025F) {
                state = CrystalnexusModBlocks.METEORITE_SCRAP_BLOCK.get().defaultBlockState();
                scraps--;
            } else if (random.nextFloat() < 0.15F) state = ORES[random.nextInt(ORES.length)];
            level.setBlock(pos, state, 2);
        }
        // Guarantee the requested 1-3 scrap blocks even if random positions miss.
        while (scraps-- > 0) level.setBlock(center.offset(random.nextInt(7) - 3, random.nextInt(7) - 3, random.nextInt(7) - 3),
                CrystalnexusModBlocks.METEORITE_SCRAP_BLOCK.get().defaultBlockState(), 2);
        ServerLevel serverLevel = level.getLevel();
        serverLevel.getServer().execute(() -> MeteorTrackerSavedData.get(serverLevel).add(center));
        return true;
    }

    private static void carveCrater(WorldGenLevel level, BlockPos center) {
        for (int x = -CRATER_RADIUS; x <= CRATER_RADIUS; x++) for (int z = -CRATER_RADIUS; z <= CRATER_RADIUS; z++) {
            int distance = x * x + z * z;
            if (distance > CRATER_RADIUS * CRATER_RADIUS) continue;
            int depth = Math.max(0, CRATER_DEPTH - (int) (Math.sqrt(distance) * CRATER_DEPTH / CRATER_RADIUS));
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, center.getX() + x, center.getZ() + z) - 1;
            for (int y = 0; y < depth; y++) level.setBlock(new BlockPos(center.getX() + x, surfaceY - y, center.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
        }
    }
}
