package net.crystalnexus.block;

import net.crystalnexus.init.CrystalnexusModFluids;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class SulfuricAcidBlock extends LiquidBlock {
    public SulfuricAcidBlock() {
        super(CrystalnexusModFluids.SULFURIC_ACID.get(), BlockBehaviour.Properties.of().mapColor(MapColor.WATER)
            .strength(100f).noCollission().noLootTable().liquid().pushReaction(PushReaction.DESTROY)
            .sound(SoundType.EMPTY).replaceable());
    }
}
