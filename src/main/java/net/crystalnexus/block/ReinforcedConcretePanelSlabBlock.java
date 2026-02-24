package net.crystalnexus.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SlabBlock;

public class ReinforcedConcretePanelSlabBlock extends SlabBlock {
	public ReinforcedConcretePanelSlabBlock() {
		super(BlockBehaviour.Properties.of().strength(5.15f, 9.5f).requiresCorrectToolForDrops());
	}
}