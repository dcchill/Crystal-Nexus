package net.crystalnexus.block.entity;

import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BasicEnergyCableBlockEntity extends EnergyCableMk2BlockEntity {
    public BasicEnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.BASIC_ENERGY_CABLE.get(), pos, state,
            CrystalnexusConfig.MACHINES.BASIC_ENERGY_CABLE);
    }
}
