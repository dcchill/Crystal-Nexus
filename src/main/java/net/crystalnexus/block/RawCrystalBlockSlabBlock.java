package net.crystalnexus.block;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SlabBlock;

public class RawCrystalBlockSlabBlock extends SlabBlock {
	public RawCrystalBlockSlabBlock() {
		super(BlockBehaviour.Properties.of().strength(1f, 7.5f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT));
	}
}