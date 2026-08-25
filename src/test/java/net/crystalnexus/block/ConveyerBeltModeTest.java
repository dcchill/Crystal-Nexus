package net.crystalnexus.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConveyerBeltModeTest {
    @Test
    void choosesTheAttachedStorageMode() {
        assertEquals(ConveyerBeltMode.Mode.NORMAL, ConveyerBeltMode.modeFor(false, false));
        assertEquals(ConveyerBeltMode.Mode.INPUT, ConveyerBeltMode.modeFor(true, false));
        assertEquals(ConveyerBeltMode.Mode.OUTPUT, ConveyerBeltMode.modeFor(false, true));
        assertEquals(ConveyerBeltMode.Mode.INPUT, ConveyerBeltMode.modeFor(true, true));
    }
}
