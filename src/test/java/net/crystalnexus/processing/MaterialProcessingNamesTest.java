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
        assertEquals(4, MaterialProcessingNames.requiredMachineTier("carbon"));
        assertEquals(4, MaterialProcessingNames.requiredMachineTier("hyper_alloy"));
        assertEquals(6, MaterialProcessingNames.requiredMachineTier("tungsten"));
    }

    @Test
    void higherMachineTiersAreFaster() {
        int previousTicks = Integer.MAX_VALUE;
        for (MachineTier tier : MachineTier.values()) {
            int ticks = (int) tier.processingTime(100);
            assertTrue(tier == MachineTier.IRON || ticks < previousTicks);
            previousTicks = ticks;
        }
    }

    @Test
    void machineTierEnergyCostsFollowTheSmootherCurve() {
        int[] expectedCosts = {2048, 4096, 8192, 16384, 32768, 65536, 131072};
        for (int i = 0; i < MachineTier.values().length; i++) {
            int energy = MachineTier.values()[i].energyCost(4096);
            assertEquals(expectedCosts[i], energy);
        }
    }

    @Test
    void machineTierProcessingTimesFollowTheSmootherCurve() {
        int[] expectedTicks = {125, 100, 75, 50, 35, 30, 25};
        for (int i = 0; i < MachineTier.values().length; i++)
            assertEquals(expectedTicks[i], MachineTier.values()[i].processingTime(100));
    }

    @Test
    void tieredMachineFamiliesUseTheirMaterialTier() {
        assertEquals(1024, MachineTier.IRON.energyCost(2048));
        assertEquals(2048, MachineTier.CRYSTAL.energyCost(2048));
        assertEquals(4096, MachineTier.CHLOROPHYTE.energyCost(2048));
        assertEquals(8192, MachineTier.INVERTIUM_TITANIUM.energyCost(2048));
        assertEquals(32768, MachineTier.TITANIUM_CARBIDE.energyCost(2048));
        assertEquals(65536, MachineTier.TUNGSTEN.energyCost(2048));

        assertEquals(256, MachineTier.IRON.energyCost(512));
        assertEquals(512, MachineTier.CRYSTAL.energyCost(512));
        assertEquals(2048, MachineTier.INVERTIUM_TITANIUM.energyCost(512));

        assertEquals(4096, MachineTier.CHLOROPHYTE.energyCost(2048));
        assertEquals(65536, MachineTier.INVERTIUM_TITANIUM.energyCost(2048 * 8));
    }

    @Test
    void tierCapacityCanHoldItsBaseOperationCost() {
        assertEquals(32768, MachineTier.HYPER_CARBON.minimumCapacity(10240, 4096));
        assertEquals(65536, MachineTier.TITANIUM_CARBIDE.minimumCapacity(10240, 4096));
        assertEquals(131072, MachineTier.TUNGSTEN.minimumCapacity(10240, 4096));
        assertEquals(65536, MachineTier.INVERTIUM_TITANIUM.minimumCapacity(10240, 2048 * 8));
        assertEquals(50000, MachineTier.HYPER_CARBON.minimumCapacity(50000, 4096));
    }

    @Test
    void tierThreeUsesItsPlayerFacingTechnologyName() {
        assertEquals("Titanium", MachineTier.INVERTIUM_TITANIUM.displayName());
        assertEquals(MachineTier.CRYSTAL, MachineTier.forLevel(1));
        assertEquals(MachineTier.TITANIUM_CARBIDE, MachineTier.forLevel(5));
        assertEquals(MachineTier.TUNGSTEN, MachineTier.forLevel(6));
    }
}
