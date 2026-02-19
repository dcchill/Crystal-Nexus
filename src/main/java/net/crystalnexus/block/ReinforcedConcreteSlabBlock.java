package net.crystalnexus.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SlabBlock;

public class ReinforcedConcreteSlabBlock extends SlabBlock {
	public ReinforcedConcreteSlabBlock() {
		super(BlockBehaviour.Properties.of().strength(5.15f, 9.5f).requiresCorrectToolForDrops());
	}
}