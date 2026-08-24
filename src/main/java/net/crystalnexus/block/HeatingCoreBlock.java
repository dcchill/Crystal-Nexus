package net.crystalnexus.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class HeatingCoreBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public HeatingCoreBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(5F, 15F)
            .requiresCorrectToolForDrops().lightLevel(state -> state.getValue(LIT) ? 15 : 0));
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}
