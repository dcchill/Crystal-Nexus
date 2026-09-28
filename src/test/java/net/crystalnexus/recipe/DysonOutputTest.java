package net.crystalnexus.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DysonOutputTest {
    @Test void frameSupportsSixSheetsWithCarbonPriority() {
        assertEquals(new DysonOutput.Result(4, 2, 4 * 4096 + 2 * 512), DysonOutput.calculate(1, 4, 8, 1));
        assertEquals(new DysonOutput.Result(4, 2, (4 * 4096 + 2 * 512) * 8), DysonOutput.calculate(1, 8, 8, 8));
    }

    @Test void starMultipliersAndCapacityBounds() {
        assertEquals(0, DysonOutput.calculate(0, 1024, 1024, 8).fePerTick());
        assertEquals(0, DysonOutput.calculate(1024, 1024, 1024, 0).fePerTick());
        assertEquals(512, DysonOutput.calculate(1, 0, 1, 1).fePerTick());
        assertEquals(16384 * 4, DysonOutput.calculate(1, 1, 0, 4).fePerTick());
        assertEquals(100_663_296, DysonOutput.calculate(1024, 3072, 0, 8).fePerTick());
    }

    @Test void integrityScalesOutputLinearly() {
        assertEquals(1_000, DysonOutput.MAX_INTEGRITY);
        int full = DysonOutput.calculate(1, 1, 0, 1).fePerTick();
        assertEquals(0, DysonOutput.calculate(1, 1, 0, 1, 0).fePerTick());
        assertEquals(full / 2, DysonOutput.calculate(1, 1, 0, 1, DysonOutput.MAX_INTEGRITY / 2).fePerTick());
        assertEquals(full, DysonOutput.calculate(1, 1, 0, 1, DysonOutput.MAX_INTEGRITY).fePerTick());
    }

    @Test void carbonSolarCapacityReachesFullOutputAt512Structures() {
        assertEquals(1368, DysonOutput.calculate(342, 2048, 0, 8).carbonSheets());
        assertEquals(new DysonOutput.Result(2048, 0, 67_108_864), DysonOutput.calculate(512, 2048, 0, 8));
    }
}
