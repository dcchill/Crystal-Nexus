package net.crystalnexus.block;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.Blocks;

public class RawCrystalBlockStairsBlock extends StairBlock {
	public RawCrystalBlockStairsBlock() {
		super(Blocks.AIR.defaultBlockState(), BlockBehaviour.Properties.of().strength(1f, 7.5f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT));
	}

	@Override
	public float getExplosionResistance() {
		return 7.5f;
	}
}