package net.crystalnexus.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StarDurabilityTest {
    @Test
    void durabilityDoublesByTierAndStressControlsConsumption() {
        int previous = 0;
        for (int durability : new int[] {
            StarDurability.YELLOW, StarDurability.ORANGE, StarDurability.BLUE, StarDurability.PINK
        }) {
            assertTrue((durability & (durability - 1)) == 0);
            assertTrue(previous == 0 || durability == previous * 2);
            previous = durability;
        }

        assertFalse(StarDurability.consumedBy(0, 0));
        assertTrue(StarDurability.consumedBy(2_500, 2_499));
        assertFalse(StarDurability.consumedBy(2_500, 2_500));
        assertTrue(StarDurability.consumedBy(10_000, 9_999));
    }
}
