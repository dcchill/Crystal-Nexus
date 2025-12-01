package net.crystalnexus.block;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SlabBlock;

public class TarrockSlabBlock extends SlabBlock {
	public TarrockSlabBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.NETHERRACK).strength(3f, 4.2f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM));
	}
}