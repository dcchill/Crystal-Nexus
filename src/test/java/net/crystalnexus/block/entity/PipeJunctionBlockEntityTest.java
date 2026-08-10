package net.crystalnexus.block.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PipeJunctionBlockEntityTest {
    @Test
    void splitsFluidEvenlyAndRotatesTheRemainder() {
        assertArrayEquals(new int[]{34, 33, 33},
                FluidSplitMath.fairShares(100, new int[]{100, 100, 100}, 0));
        assertArrayEquals(new int[]{33, 34, 33},
                FluidSplitMath.fairShares(100, new int[]{100, 100, 100}, 1));
    }

    @Test
    void redistributesFluidRejectedByAFullPipe() {
        assertArrayEquals(new int[]{10, 45, 45},
                FluidSplitMath.fairShares(100, new int[]{10, 100, 100}, 0));
    }
}
