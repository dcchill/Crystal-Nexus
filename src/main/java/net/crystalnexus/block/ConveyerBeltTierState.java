package net.crystalnexus.block;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class ConveyerBeltTierState {
    private ConveyerBeltTierState() {}

    static ConveyerBeltTier tierAt(BlockState state) {
        if (state.getBlock() == CrystalnexusModBlocks.TITANIUM_CONVEYER_BELT.get()) return ConveyerBeltTier.TITANIUM;
        if (state.getBlock() == CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get()) return ConveyerBeltTier.METEORITE;
        if (state.hasProperty(ConveyerBeltInputBlock.TIER)) return ConveyerBeltTier.fromIndex(state.getValue(ConveyerBeltInputBlock.TIER));
        return ConveyerBeltTier.BASIC;
    }

    static Block normalBlock(ConveyerBeltTier tier) {
        return switch (tier) {
            case BASIC -> CrystalnexusModBlocks.CONVEYER_BELT.get();
            case TITANIUM -> CrystalnexusModBlocks.TITANIUM_CONVEYER_BELT.get();
            case METEORITE -> CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get();
        };
    }

    static BlockState applyTier(BlockState state, ConveyerBeltTier tier) {
        return state.hasProperty(ConveyerBeltInputBlock.TIER)
                ? state.setValue(ConveyerBeltInputBlock.TIER, tier.index())
                : state;
    }
}
