package net.crystalnexus.processing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlurryColorMathTest {
    @Test
    void averagesVisibleSpritePixelsAndIgnoresTransparentPixels() {
        assertEquals(0xff8a0a8a, SlurryColorMath.averageAbgr(new int[] {
            0xff0000ff, 
            0xffff0000, 
            0x0000ff00  
        }, 0xff7f95a3));
    }
}
