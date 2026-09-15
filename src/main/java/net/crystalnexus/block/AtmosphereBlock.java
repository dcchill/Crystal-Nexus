package net.crystalnexus.block;

import net.crystalnexus.init.CrystalnexusModFluids;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class AtmosphereBlock extends LiquidBlock {
    public AtmosphereBlock() {
        super(CrystalnexusModFluids.ATMOSPHERE.get(), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
            .strength(100F).noCollission().noLootTable().liquid().replaceable()
            .pushReaction(PushReaction.DESTROY).sound(SoundType.EMPTY));
    }
}
