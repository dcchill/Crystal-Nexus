package net.crystalnexus.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MachineEnergyInputTest {
    @Test
    void machineInputScalesSafelyWithoutChangingItemInputOrExtraction() {
        assertEquals(20480, CrystalnexusConfig.MACHINES.CRYSTAL_SMELTER.maxReceive());
        assertEquals(2048, CrystalnexusConfig.MACHINES.CRYSTAL_SMELTER.maxExtract());
        assertEquals(10240, CrystalnexusConfig.MACHINES.CRYSTAL_SMELTER.capacity());
        assertEquals(2500, CrystalnexusConfig.ITEMS.BATTERY_CELL.maxReceive());
        assertEquals(0, CrystalnexusConfig.machineEnergyInput(0));
        assertEquals(20480, CrystalnexusConfig.machineEnergyInput(2048));
        assertEquals(Integer.MAX_VALUE, CrystalnexusConfig.machineEnergyInput(Integer.MAX_VALUE));
    }
}
