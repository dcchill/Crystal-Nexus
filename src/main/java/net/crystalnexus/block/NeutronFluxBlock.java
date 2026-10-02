package net.crystalnexus.block;

import net.crystalnexus.init.CrystalnexusModFluids;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class NeutronFluxBlock extends LiquidBlock {
	public NeutronFluxBlock() {
		super(CrystalnexusModFluids.NEUTRON_FLUX.get(), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
				.strength(100F).noCollission().noLootTable().liquid().replaceable()
				.pushReaction(PushReaction.DESTROY).sound(SoundType.EMPTY));
	}
}