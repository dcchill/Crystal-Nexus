package net.crystalnexus.processing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialProcessingNamesTest {
    @Test
    void extractsAndNormalizesMaterialNames() {
        assertEquals("copper", MaterialProcessingNames.extract("c:ores/copper"));
        assertEquals("tin", MaterialProcessingNames.extract("forge:raw_materials/tin"));
        assertEquals("", MaterialProcessingNames.extract("c:ores"));
        assertEquals("copper", MaterialProcessingNames.normalizeMaterial("c:raw_materials/copper"));
    }

    @Test
    void assignsRetainedMaterialsToFourAges() {
        assertEquals(1, MaterialProcessingNames.requiredMachineAge("ancient_crystal"));
        assertEquals(2, MaterialProcessingNames.requiredMachineAge("invertium"));
        assertEquals(2, MaterialProcessingNames.requiredMachineAge("titanium"));
        assertEquals(3, MaterialProcessingNames.requiredMachineAge("titanium_carbide"));
        assertEquals(3, MaterialProcessingNames.requiredMachineAge("carbon_fiber"));
        assertEquals(3, MaterialProcessingNames.requiredMachineAge("tungsten"));
        assertEquals(4, MaterialProcessingNames.requiredMachineAge("hyper_alloy"));
        assertEquals(4, MaterialProcessingNames.requiredMachineAge("solar_material"));
        assertEquals(4, MaterialProcessingNames.requiredMachineAge("zero_point_core"));
    }

    @Test
    void exactAgeMultipliersAreApplied() {
        int[] expectedEnergy = {50, 100, 200, 400, 800};
        int[] expectedTicks = {125, 100, 75, 50, 30};
        for (int i = 0; i < MachineAge.values().length; i++) {
            assertEquals(expectedEnergy[i], MachineAge.values()[i].energyCost(100));
            assertEquals(expectedTicks[i], MachineAge.values()[i].processingTime(100));
        }
    }

    @Test
    void ageOrderingControlsRecipeSupport() {
        assertFalse(MachineAge.STARTER.supports(1));
        assertTrue(MachineAge.AGE_1.supports(1));
		assertFalse(MachineAge.AGE_1.supports(2));
		assertTrue(MachineAge.AGE_2.supports(2));
        assertFalse(MachineAge.AGE_2.supports(3));
        assertTrue(MachineAge.AGE_3.supports(3));
		assertFalse(MachineAge.AGE_3.supports(4));
		assertTrue(MachineAge.AGE_4.supports(4));
    }

    @Test
    void invalidMinimumAgesAreRejectedAndUnknownNumbersFallBackToAgeOne() {
        assertThrows(IllegalArgumentException.class, () -> MachineAge.requireNumber(0));
        assertThrows(IllegalArgumentException.class, () -> MachineAge.requireNumber(5));
        assertEquals(MachineAge.AGE_1, MachineAge.forNumber(-1));
        assertEquals(MachineAge.AGE_1, MachineAge.forNumber(99));
    }

    @Test
    void ageLabelsDescribeCapabilityGroups() {
        assertEquals("Starter", MachineAge.STARTER.displayName());
        assertTrue(MachineAge.AGE_2.displayName().contains("Invertium"));
        assertTrue(MachineAge.AGE_3.displayName().contains("Titanium Carbide"));
        assertTrue(MachineAge.AGE_4.displayName().contains("Zero Point"));
    }
}
