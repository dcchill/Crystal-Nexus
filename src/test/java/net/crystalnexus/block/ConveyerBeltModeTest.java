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
    void tiersIncreaseBatchSize() {
        assertEquals(16, ConveyerBeltTier.BASIC.batchSize());
        assertEquals(32, ConveyerBeltTier.TITANIUM.batchSize());
        assertEquals(64, ConveyerBeltTier.METEORITE.batchSize());
    }

    @Test
    void tiersDoubleInSpeed() {
        assertEquals(2.0, ConveyerBeltTier.BASIC.ticksPerMove());
        assertEquals(1.0, ConveyerBeltTier.TITANIUM.ticksPerMove());
        assertEquals(0.5, ConveyerBeltTier.METEORITE.ticksPerMove());
    }
}
