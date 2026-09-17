package net.crystalnexus.block;

import net.crystalnexus.assembly.AssemblyLineEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class AssemblyLineCasingBlock extends Block {
    public AssemblyLineCasingBlock() { super(Properties.of().strength(4, 20).sound(SoundType.METAL).requiresCorrectToolForDrops()); }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (old.getBlock() != state.getBlock()) AssemblyLineEvents.changed(level, pos, true);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (next.getBlock() != state.getBlock()) AssemblyLineEvents.changed(level, pos, true);
        super.onRemove(state, level, pos, next, moving);
    }
}
