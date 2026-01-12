package net.crystalnexus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.crystalnexus.init.CrystalnexusModBlockEntities;

public class ConveyerBeltBlockEntity extends ConveyerBeltBaseBlockEntity {
    public ConveyerBeltBlockEntity(BlockPos position, BlockState state) {
        super(CrystalnexusModBlockEntities.CONVEYER_BELT.get(), position, state);
    }
}
