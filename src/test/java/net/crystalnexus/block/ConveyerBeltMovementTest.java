package net.crystalnexus.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConveyerBeltMovementTest {
    @Test
    void itemAdvancesWhenThereIsAnEmptySegmentAhead() {
        assertTrue(ConveyerBeltMovement.canAdvance(0b0011, 0, false));
    }

    @Test
    void fullyBlockedQueueStaysStill() {
        for (int segment = 0; segment < 4; segment++) {
            assertFalse(ConveyerBeltMovement.canAdvance(0b1111, segment, false));
        }
    }

    @Test
    void queueAdvancesTogetherWhenTailCanExit() {
        for (int segment = 0; segment < 4; segment++) {
            assertTrue(ConveyerBeltMovement.canAdvance(0b1111, segment, true));
        }
    }

    @Test
    void tailWaitsWhenNextConveyorHeadIsOccupied() {
        assertFalse(ConveyerBeltMovement.canAdvance(0b1000, 3, false));
    }

    @Test
    void incomingHeadDoesNotResetOtherSegmentTiming() {
        assertEquals(20L, ConveyerBeltMovement.renderStartTime(0, 10L, 20L));
        assertEquals(10L, ConveyerBeltMovement.renderStartTime(1, 10L, 20L));
    }
}
