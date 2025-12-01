package net.crystalnexus.block;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.SoundType;

public class TarrockWallBlock extends WallBlock {
	public TarrockWallBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.NETHERRACK).strength(3f, 4.2f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM).forceSolidOn());
	}
}