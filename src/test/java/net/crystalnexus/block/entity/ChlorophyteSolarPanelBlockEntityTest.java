package net.crystalnexus.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.crystalnexus.energy.SolarPanelOutput;
import org.junit.jupiter.api.Test;

class ChlorophyteSolarPanelBlockEntityTest {
    @Test void productionMatchesDaylightAndRain() {
        assertEquals(128, SolarPanelOutput.rate(true, false));
        assertEquals(64, SolarPanelOutput.rate(true, true));
        assertEquals(0, SolarPanelOutput.rate(false, false));
    }
}
