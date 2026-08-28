package net.crystalnexus.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConveyerBeltRenderClockTest {
    @Test
    void startsEachServerStepWhenTheClientReceivesIt() {
        ConveyerBeltRenderClock clock = new ConveyerBeltRenderClock();

        assertEquals(140L, clock.startTime(1, 100L, Long.MIN_VALUE, 140L));
        assertEquals(140L, clock.startTime(1, 100L, Long.MIN_VALUE, 142L));
        assertEquals(148L, clock.startTime(1, 108L, Long.MIN_VALUE, 148L));
    }

    @Test
    void incomingHeadDoesNotRestartOtherSegments() {
        ConveyerBeltRenderClock clock = new ConveyerBeltRenderClock();

        assertEquals(140L, clock.startTime(1, 100L, Long.MIN_VALUE, 140L));
        assertEquals(143L, clock.startTime(0, 100L, 102L, 143L));
        assertEquals(140L, clock.startTime(1, 100L, 102L, 144L));
    }
}
