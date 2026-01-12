package net.crystalnexus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.crystalnexus.init.CrystalnexusModBlockEntities;

public class ConveyerBeltOutputBlockEntity extends ConveyerBeltBaseBlockEntity {
    public ConveyerBeltOutputBlockEntity(BlockPos position, BlockState state) {
        super(CrystalnexusModBlockEntities.CONVEYER_BELT_OUTPUT.get(), position, state);
    }
}
