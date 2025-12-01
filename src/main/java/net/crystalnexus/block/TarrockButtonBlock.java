package net.crystalnexus.block;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.ButtonBlock;

public class TarrockButtonBlock extends ButtonBlock {
	public TarrockButtonBlock() {
		super(BlockSetType.STONE, 20, BlockBehaviour.Properties.of().sound(SoundType.NETHERRACK).strength(2.6f, 2.7f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM));
	}
}