package net.crystalnexus.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.Blocks;

public class ReinforcedConcreteStairsBlock extends StairBlock {
	public ReinforcedConcreteStairsBlock() {
		super(Blocks.AIR.defaultBlockState(), BlockBehaviour.Properties.of().strength(5.15f, 9.5f).requiresCorrectToolForDrops());
	}

	@Override
	public float getExplosionResistance() {
		return 9.5f;
	}
}