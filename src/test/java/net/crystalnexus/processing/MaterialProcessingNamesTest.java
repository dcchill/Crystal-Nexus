package net.crystalnexus.processing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialProcessingNamesTest {
    @Test
    void extractsCommonAndLegacyMaterialNames() {
        assertEquals("copper", MaterialProcessingNames.extract("c:ores/copper"));
        assertEquals("tin", MaterialProcessingNames.extract("forge:raw_materials/tin"));
        assertEquals("", MaterialProcessingNames.extract("c:ores"));
    }

    @Test
    void normalizesDefinitionMaterialIds() {
        assertEquals("copper", MaterialProcessingNames.normalizeMaterial("c:copper"));
        assertEquals("copper", MaterialProcessingNames.normalizeMaterial("c:raw_materials/copper"));
    }

    @Test
    void assignsProgressionTiersToProtectedMaterials() {
        assertEquals(1, MaterialProcessingNames.requiredMachineTier("chlorophyte"));
        assertEquals(2, MaterialProcessingNames.requiredMachineTier("invertium"));
        assertEquals(2, MaterialProcessingNames.requiredMachineTier("platinum"));
        assertEquals(3, MaterialProcessingNames.requiredMachineTier("carbon"));
        assertEquals(3, MaterialProcessingNames.requiredMachineTier("hyper_alloy"));
    }

    @Test
    void higherMachineTiersAreFasterAndMoreEfficient() {
        int previousTicks = Integer.MAX_VALUE;
        int previousEnergy = Integer.MAX_VALUE;
        for (MachineTier tier : MachineTier.values()) {
            int ticks = (int) tier.processingTime(100);
            int energy = tier.energyCost(4096);
            assertTrue(tier == MachineTier.CRYSTAL || ticks < previousTicks);
            assertTrue(tier == MachineTier.CRYSTAL || energy < previousEnergy);
            previousTicks = ticks;
            previousEnergy = energy;
        }
    }
}
