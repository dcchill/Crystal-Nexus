package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class CrystalTankBlockEntity extends TankBlockEntity {
    public CrystalTankBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.CRYSTAL_TANK.get(), pos, state, Math.max(1, azurinePerBlockCapacity() / 2));
    }
}
