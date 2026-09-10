package net.crystalnexus.processing;

import org.junit.jupiter.api.Test;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineTierTest {
    @Test
    void visibleNumbersPreserveInternalLevelsAndMaterialNames() {
        String[] names = {"Iron", "Crystal", "Chlorophyte", "Invertium", "Titanium", "Carbon",
            "Titanium Carbide", "Tungsten", "Hyper"};
        assertEquals(names.length, MachineTier.values().length);
        var colors = new HashSet<Integer>();
        for (int level = 0; level < names.length; level++) {
            MachineTier tier = MachineTier.forLevel(level);
            assertEquals(level, tier.level());
            assertEquals(level + 1, tier.displayNumber());
            assertEquals(names[level], tier.displayName());
            assertTrue(colors.add(tier.displayColor()), "Each tier should have a distinct color");
            assertTrue(tier.displayColor() >= 0 && tier.displayColor() <= 0xFFFFFF);
        }
    }
}
