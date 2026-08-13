package net.crystalnexus.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class IlmeniteOreBlock extends Block {
	public IlmeniteOreBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(3f, 3f).requiresCorrectToolForDrops());
	}
}
