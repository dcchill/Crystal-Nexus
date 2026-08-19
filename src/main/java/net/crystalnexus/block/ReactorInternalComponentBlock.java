package net.crystalnexus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;

public class ReactorInternalComponentBlock extends Block {
	private final Component tooltipText;

	public ReactorInternalComponentBlock(Component tooltipText) {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.75f, 18f).requiresCorrectToolForDrops());
		this.tooltipText = tooltipText;
	}

	public ReactorInternalComponentBlock() {
		this(net.minecraft.network.chat.Component.literal("Reactor Internal Component"));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}

	public Component getTooltipText() {
		return tooltipText;
	}
}
