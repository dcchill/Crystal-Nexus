package net.crystalnexus.radiation;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class RadiationTags {

    public static final TagKey<Block> RADIOACTIVE_BLOCKS =
            TagKey.create(
                    Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath("crystalnexus", "radioactive_blocks")
            );
}
