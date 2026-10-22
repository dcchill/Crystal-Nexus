package net.crystalnexus.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConveyerBeltRenderClockTest {
    @Test
    void preservesServerTimingAcrossUpdates() {
        ConveyerBeltRenderClock clock = new ConveyerBeltRenderClock();

        assertEquals(140L, clock.startTime(1, 100L, Long.MIN_VALUE, 140L));
        assertEquals(140L, clock.startTime(1, 100L, Long.MIN_VALUE, 142L));
        assertEquals(148.0, clock.startTime(1, 108L, Long.MIN_VALUE, 151L));
        assertEquals(148.25, clock.startTime(1, 108.25, Long.MIN_VALUE, 152L));
    }

    @Test
    void recoversWhenTheFirstSnapshotWasFromAnIdleBelt() {
        ConveyerBeltRenderClock clock = new ConveyerBeltRenderClock();
        assertEquals(140.0, clock.startTime(1, 0, Long.MIN_VALUE, 140));
        assertEquals(141.0, clock.startTime(1, 141, Long.MIN_VALUE, 141));
        assertEquals(142.0, clock.startTime(1, 142, Long.MIN_VALUE, 145));
    }

    @Test
    void incomingHeadDoesNotRestartOtherSegments() {
        ConveyerBeltRenderClock clock = new ConveyerBeltRenderClock();

        assertEquals(140L, clock.startTime(1, 100L, Long.MIN_VALUE, 140L));
        assertEquals(142.0, clock.startTime(0, 100L, 102L, 143L));
        assertEquals(140L, clock.startTime(1, 100L, 102L, 144L));
    }
}
