package net.crystalnexus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class BuddingSulfurCrystalBlock extends Block {

    public BuddingSulfurCrystalBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.GLASS)
                .strength(2f, 19f)
                .requiresCorrectToolForDrops()
                .randomTicks());
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 15;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        // Slow growth rate
        if (random.nextInt(30) != 0) return;

        // Pick random face
        Direction direction = Direction.values()[random.nextInt(6)];
        BlockPos targetPos = pos.relative(direction);

        BlockState targetState = level.getBlockState(targetPos);

        // Only grow into air
        if (!targetState.isAir()) return;

        level.setBlock(targetPos,
                CrystalnexusModBlocks.SMALL_SULFUR_CRYSTAL.get()
                        .defaultBlockState()
                        .setValue(SmallSulfurCrystalBlock.FACING, direction),
                2);
    }
}