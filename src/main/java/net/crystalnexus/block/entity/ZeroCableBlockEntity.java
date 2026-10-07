package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ZeroCableBlockEntity extends EnergyCableMk2BlockEntity {
    public ZeroCableBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.ZERO_CABLE.get(), pos, state,
            Integer.MAX_VALUE);
    }
}
