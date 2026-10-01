package net.crystalnexus.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConveyerBeltModeTest {
    @Test
    void choosesTheAttachedStorageMode() {
        assertEquals(ConveyerBeltMode.Mode.NORMAL, ConveyerBeltMode.Mode.forStorage(false, false));
        assertEquals(ConveyerBeltMode.Mode.INPUT, ConveyerBeltMode.Mode.forStorage(true, false));
        assertEquals(ConveyerBeltMode.Mode.OUTPUT, ConveyerBeltMode.Mode.forStorage(false, true));
        assertEquals(ConveyerBeltMode.Mode.INPUT, ConveyerBeltMode.Mode.forStorage(true, true));
    }

    @Test
    void tiersDoubleInSpeed() {
        assertEquals(4, ConveyerBeltTier.BASIC.ticksPerMove());
        assertEquals(2, ConveyerBeltTier.TITANIUM.ticksPerMove());
        assertEquals(1, ConveyerBeltTier.METEORITE.ticksPerMove());
    }
}
