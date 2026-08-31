package net.crystalnexus.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class GravitationalArrayCostScheduleTest {
    @Test
    void yellowDwarfCostsAreDistributedExactlyAcross1200ProgressTicks() {
        long energy = 0;
        long fluid = 0;
        for (int tick = 0; tick < 1200; tick++) {
            energy += GravitationalArrayCostSchedule.next(50_000_000L, tick, 1200);
            fluid += GravitationalArrayCostSchedule.next(250_000L, tick, 1200);
        }
        assertEquals(50_000_000L, energy);
        assertEquals(250_000L, fluid);
    }

    @Test
    void invalidProgressCannotConsumeResources() {
        assertThrows(IllegalArgumentException.class,
            () -> GravitationalArrayCostSchedule.next(50_000_000L, 1200, 1200));
    }

    @Test
    void shortSolarSimulatorDurationExposesItsRequiredTransferRate() {
        assertEquals(2_133_334, GravitationalArrayCostSchedule.maximumStep(6_400_000, 3));
    }
}
