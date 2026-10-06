package net.crystalnexus.block.entity;

import net.crystalnexus.block.PlasmaBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

/** Server-side route data carried between plasma positions during a detour. */
public final class PlasmaBlockEntity extends BlockEntity {
    private List<BlockPos> route = List.of();

    public PlasmaBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.PLASMA_BLOCK.get(), pos, state);
    }

    public List<BlockPos> getRoute() { return route; }
    public void setRoute(List<BlockPos> route) {
        this.route = List.copyOf(route);
        setChanged();
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        long[] saved = tag.getLongArray("route");
        route = saved.length <= PlasmaBlock.SEARCH_LIMIT ? Arrays.stream(saved).mapToObj(BlockPos::of).toList() : List.of();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLongArray("route", route.stream().mapToLong(BlockPos::asLong).toArray());
    }
}
