package net.crystalnexus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.crystalnexus.init.CrystalnexusModBlockEntities;

public class ConveyerBeltInputBlockEntity extends ConveyerBeltBaseBlockEntity {
    public ConveyerBeltInputBlockEntity(BlockPos position, BlockState state) {
        super(CrystalnexusModBlockEntities.CONVEYER_BELT_INPUT.get(), position, state);
    }
}
