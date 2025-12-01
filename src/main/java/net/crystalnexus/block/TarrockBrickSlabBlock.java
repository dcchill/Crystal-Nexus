package net.crystalnexus.block;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SlabBlock;

public class TarrockBrickSlabBlock extends SlabBlock {
	public TarrockBrickSlabBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.NETHER_BRICKS).strength(3.25f, 4.7f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM));
	}
}