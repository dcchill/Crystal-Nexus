package net.crystalnexus.block;

import net.crystalnexus.init.CrystalnexusModFluids;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class InversionSolutionBlock extends LiquidBlock {
    public InversionSolutionBlock() {
        super(CrystalnexusModFluids.INVERSION_SOLUTION.get(), BlockBehaviour.Properties.of().mapColor(MapColor.WATER)
            .strength(100f).noCollission().noLootTable().liquid().pushReaction(PushReaction.DESTROY)
            .sound(SoundType.EMPTY).replaceable());
    }
}
