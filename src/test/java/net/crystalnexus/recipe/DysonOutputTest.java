package net.crystalnexus.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DysonOutputTest {
    @Test void frameSupportsSixSheetsWithCarbonPriority() {
        assertEquals(new DysonOutput.Result(4, 2, 4 * 16384 + 2 * 4096), DysonOutput.calculate(1, 4, 8, 1));
        assertEquals(new DysonOutput.Result(6, 0, 6 * 16384 * 8), DysonOutput.calculate(1, 8, 8, 8));
    }

    @Test void starMultipliersAndCapacityBounds() {
        assertEquals(0, DysonOutput.calculate(0, 1024, 1024, 8).fePerTick());
        assertEquals(0, DysonOutput.calculate(1024, 1024, 1024, 0).fePerTick());
        assertEquals(4096, DysonOutput.calculate(1, 0, 1, 1).fePerTick());
        assertEquals(16384 * 4, DysonOutput.calculate(1, 1, 0, 4).fePerTick());
        assertEquals(402_653_184, DysonOutput.calculate(1024, 3072, 0, 8).fePerTick());
    }
}
