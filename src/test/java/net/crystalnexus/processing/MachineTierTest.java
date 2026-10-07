package net.crystalnexus.processing;

import org.junit.jupiter.api.Test;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineTierTest {
    @Test
    void onlyHyperDoublesTheCurrentEnergyInputLimit() {
        for (MachineTier tier : MachineTier.values())
            assertEquals(tier == MachineTier.HYPER ? 40960 : 20480, tier.energyInput(20480));
        assertEquals(0, MachineTier.HYPER.energyInput(0));
        assertEquals(Integer.MAX_VALUE, MachineTier.HYPER.energyInput(Integer.MAX_VALUE));
    }

    @Test
    void materialSlotCapacityFollowsUpgradeSlots() {
        int[] capacities = {64, 64, 128, 256, 256, 512, 512, 1024, 1024};
        for (MachineTier tier : MachineTier.values())
            assertEquals(capacities[tier.level()], tier.itemSlotCapacity(), tier.displayName());
    }

    @Test
    void visibleNumbersPreserveInternalLevelsAndMaterialNames() {
        String[] names = {"Iron", "Crystal", "Chlorophyte", "Invertium", "Azurine", "Carbon",
            "Ferrosteel", "Obsidrax", "Hyper"};
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
