package net.crystalnexus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachineUpgradeHelperTest {
	@Test
	void speedMultiplierIsCappedAtTwentyTimes() {
		assertEquals(0.05, MachineUpgradeHelper.clampCookMultiplier(0.01));
		assertEquals(20.0, 1.0 / MachineUpgradeHelper.clampCookMultiplier(0.01));
	}
}
