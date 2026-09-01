package net.crystalnexus.block;

import net.crystalnexus.block.entity.HyperEnergyCableBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;

public class HyperEnergyCableBlock extends EnergyCableMk2Block {
    public HyperEnergyCableBlock(ResourceLocation id) { super(id); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HyperEnergyCableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == CrystalnexusModBlockEntities.HYPER_ENERGY_CABLE.get()
            ? (lvl, pos, blockState, be) -> ((HyperEnergyCableBlockEntity) be).serverTick() : null;
    }
}
