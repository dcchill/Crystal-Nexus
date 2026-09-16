package net.crystalnexus.block;

import net.crystalnexus.block.entity.CrystalTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CrystalTankBlock extends TankBlock {
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CrystalTankBlockEntity(pos, state); }
}
