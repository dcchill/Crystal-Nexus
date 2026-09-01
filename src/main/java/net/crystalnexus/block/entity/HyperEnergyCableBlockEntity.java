package net.crystalnexus.block.entity;

import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class HyperEnergyCableBlockEntity extends EnergyCableMk2BlockEntity {
    public HyperEnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.HYPER_ENERGY_CABLE.get(), pos, state,
            CrystalnexusConfig.MACHINES.HYPER_ENERGY_CABLE);
    }
}
