package net.crystalnexus.block;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.WallBlock;

public class SmoothTarrockWallBlock extends WallBlock {
	public SmoothTarrockWallBlock() {
		super(BlockBehaviour.Properties.of().strength(3f, 4.2f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM).forceSolidOn());
	}
}