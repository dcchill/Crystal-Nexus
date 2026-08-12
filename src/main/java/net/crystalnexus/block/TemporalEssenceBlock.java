package net.crystalnexus.block;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.crystalnexus.init.CrystalnexusModFluids;

public class TemporalEssenceBlock extends LiquidBlock {
	public TemporalEssenceBlock() {
		super(CrystalnexusModFluids.TEMPORAL_ESSENCE.get(), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(100f).noCollission().noLootTable().liquid().pushReaction(PushReaction.DESTROY)
				.sound(SoundType.EMPTY).replaceable());
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		int level = state.getValue(LEVEL);
		if (level == 0)
			return fluid.getSource(false);
		return level < 8 ? fluid.getFlowing(8 - level, false) : fluid.getFlowing(16 - level, true);
	}
}
