package net.crystalnexus.jei_recipes;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitaniumElectrolysisRecipeTest {
    @Test
    void acceptsExactlyOnePositiveInput() {
        assertNull(TitaniumElectrolysisRecipeRules.validationError(true, false, 1000, 250));
        assertNull(TitaniumElectrolysisRecipeRules.validationError(false, true, 1, 250));
    }

    @Test
    void rejectsMissingConflictingAndInvalidInputs() {
        assertEquals("Titanium electrolysis recipes require exactly one fluid_input or item_input",
            TitaniumElectrolysisRecipeRules.validationError(false, false, 1, 250));
        assertEquals("Titanium electrolysis recipes require exactly one fluid_input or item_input",
            TitaniumElectrolysisRecipeRules.validationError(true, true, 1, 250));
        assertEquals("Titanium electrolysis recipe amounts must be positive",
            TitaniumElectrolysisRecipeRules.validationError(false, true, 0, 250));
        assertEquals("Titanium electrolysis recipe amounts must be positive",
            TitaniumElectrolysisRecipeRules.validationError(true, false, 1000, 0));
    }

    @Test
    void bundledRecipesContainSpecifiedAmounts() throws Exception {
        String water = Files.readString(Path.of("src/main/resources/data/crystalnexus/recipe/titanium_electrolysis_water.json"));
        String biomass = Files.readString(Path.of("src/main/resources/data/crystalnexus/recipe/titanium_electrolysis_biomass.json"));
        assertTrue(water.contains("\"minecraft:water\"") && water.contains("\"amount\": 1000")
            && water.contains("\"crystalnexus:oxygen\"") && water.contains("\"amount\": 250"));
        assertTrue(biomass.contains("\"crystalnexus:biomass\"") && biomass.contains("\"item_input_count\": 1")
            && biomass.contains("\"crystalnexus:nitrogen\"") && biomass.contains("\"amount\": 250"));
    }
}
