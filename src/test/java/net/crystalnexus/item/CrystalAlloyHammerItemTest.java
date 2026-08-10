package net.crystalnexus.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalAlloyHammerItemTest {
    @Test
    void nodeChanceUsesExclusiveUpperBound() {
        assertTrue(CrystalAlloyHammerRoll.createsNode(0.009999F, 0.01F));
        assertFalse(CrystalAlloyHammerRoll.createsNode(0.01F, 0.01F));
        assertTrue(CrystalAlloyHammerRoll.createsNode(0.049999F, 0.05F));
        assertFalse(CrystalAlloyHammerRoll.createsNode(0.05F, 0.05F));
    }
}
