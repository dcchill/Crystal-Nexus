package net.crystalnexus.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeartCycleTest {
    @Test void beatsExactlyEveryTwentyTicks() {
        HeartCycle cycle = new HeartCycle();
        for (int tick = 1; tick <= 100; tick++) assertEquals(tick % 20 == 0, cycle.tick(true));
    }
    @Test void missedBeatsDoNotAccumulate() {
        HeartCycle cycle = new HeartCycle();
        for (int tick = 0; tick < 100; tick++) assertFalse(cycle.tick(false));
        for (int tick = 0; tick < 19; tick++) assertFalse(cycle.tick(true));
        assertTrue(cycle.tick(true));
    }
    @Test void bufferCanDrainBetweenBeats() {
        HeartCycle cycle = new HeartCycle();
        for (int tick = 0; tick < 19; tick++) assertFalse(cycle.tick(false));
        assertTrue(cycle.tick(true));
    }
    @Test void dismantlingOrReloadStartsAFreshPeriod() {
        HeartCycle cycle = new HeartCycle();
        for (int tick = 0; tick < 19; tick++) assertFalse(cycle.tick(true));
        cycle.reset();
        for (int tick = 0; tick < 19; tick++) assertFalse(cycle.tick(true));
        assertTrue(cycle.tick(true));
    }
}
