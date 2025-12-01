package net.crystalnexus.block;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SlabBlock;

public class SmoothTarrockSlabBlock extends SlabBlock {
	public SmoothTarrockSlabBlock() {
		super(BlockBehaviour.Properties.of().strength(3f, 4.2f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM));
	}
}