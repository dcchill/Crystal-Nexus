package net.crystalnexus.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public final class PlasmaBlock extends Block {
    public PlasmaBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.GLASS).strength(1.1f, 8f).lightLevel(state -> 15)
            .noOcclusion().hasPostProcess((state, level, pos) -> true).emissiveRendering((state, level, pos) -> true)
            .isRedstoneConductor((state, level, pos) -> false));
    }

    @Override public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.getBlock() == this || super.skipRendering(state, adjacent, side);
    }

    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 0; }
    @Override public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }
}
