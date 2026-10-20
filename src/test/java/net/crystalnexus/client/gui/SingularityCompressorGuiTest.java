package net.crystalnexus.client.gui;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SingularityCompressorGuiTest {
    @Test void configuredGhostIsHiddenForRealInputsAndClearedOrInvalidSelections() {
        assertEquals(ResourceLocation.parse("minecraft:diamond"), SingularityCompressorGUIScreen.ghostItemId("minecraft:diamond", false));
        assertEquals(ResourceLocation.parse("crystalnexus:raw_heavy_iron"), SingularityCompressorGUIScreen.ghostItemId("crystalnexus:raw_heavy_iron", false));
        assertNull(SingularityCompressorGUIScreen.ghostItemId("minecraft:diamond", true));
        for (String cleared : new String[]{"", " ", "none", "invalid item!"})
            assertNull(SingularityCompressorGUIScreen.ghostItemId(cleared, false), cleared);
    }
}
