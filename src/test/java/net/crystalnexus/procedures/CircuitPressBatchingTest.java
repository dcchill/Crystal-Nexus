package net.crystalnexus.procedures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CircuitPressBatchingTest {
    @Test
    void craftsAsManyWholeRecipesAsAvailableUpToEight() {
        assertEquals(1, CircuitPressBatching.basicBatchSize(1, 1, 64, 1));
        assertEquals(2, CircuitPressBatching.basicBatchSize(2, 5, 64, 1));
        assertEquals(8, CircuitPressBatching.basicBatchSize(20, 20, 64, 1));
        assertEquals(3, CircuitPressBatching.basicBatchSize(8, 8, 7, 2));
        assertEquals(0, CircuitPressBatching.basicBatchSize(8, 8, 1, 2));
    }
}
