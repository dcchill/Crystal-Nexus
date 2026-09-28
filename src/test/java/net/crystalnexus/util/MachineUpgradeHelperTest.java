package net.crystalnexus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachineUpgradeHelperTest {
	@Test
	void upgradeWeightFollowsRequestedCurveAndCapsAtSixteen() {
		assertEquals(0.0, MachineUpgradeHelper.weightForCount(0));
		assertEquals(1.0, MachineUpgradeHelper.weightForCount(1));
		assertEquals(2.0, MachineUpgradeHelper.weightForCount(2));
		assertEquals(3.75, MachineUpgradeHelper.weightForCount(4));
		assertEquals(5.25, MachineUpgradeHelper.weightForCount(6));
		assertEquals(6.5, MachineUpgradeHelper.weightForCount(8));
		assertEquals(7.5, MachineUpgradeHelper.weightForCount(10));
		assertEquals(8.25, MachineUpgradeHelper.weightForCount(12));
		assertEquals(8.875, MachineUpgradeHelper.weightForCount(14));
		assertEquals(9.375, MachineUpgradeHelper.weightForCount(16));
		assertEquals(MachineUpgradeHelper.weightForCount(16), MachineUpgradeHelper.weightForCount(64));
		for (int count = 3; count < 16; count += 2)
			assertEquals((MachineUpgradeHelper.weightForCount(count - 1) + MachineUpgradeHelper.weightForCount(count + 1)) / 2,
				MachineUpgradeHelper.weightForCount(count), 0.001);
	}

	@Test
	void processingSpeedScalesWithoutReducingTimeToZero() {
		assertEquals(100.0, MachineUpgradeHelper.scaledProcessingTime(100, 50, MachineUpgradeHelper.weightForCount(0)));
		assertEquals(50.0, MachineUpgradeHelper.scaledProcessingTime(100, 50, MachineUpgradeHelper.weightForCount(1)));
		assertEquals(100.0 / 3.0, MachineUpgradeHelper.scaledProcessingTime(100, 50, MachineUpgradeHelper.weightForCount(2)));
		assertEquals(100.0 / (1 + MachineUpgradeHelper.weightForCount(16)),
				MachineUpgradeHelper.scaledProcessingTime(100, 50, MachineUpgradeHelper.weightForCount(16)));
		int[] counts = {0, 1, 2, 4, 6, 8, 10, 12, 14, 16};
		double[] speed = {1.0, 1.4, 1.8, 2.5, 3.1, 3.6, 4.0, 4.3, 4.55, 4.75};
		for (int i = 0; i < counts.length; i++)
			assertEquals(speed[i], 140.0 / MachineUpgradeHelper.scaledProcessingTime(140, 100,
				MachineUpgradeHelper.weightForCount(counts[i])), 0.000001);
	}
	@Test
	void speedMultiplierIsCappedAtTwentyTimes() {
		assertEquals(0.05, MachineUpgradeHelper.clampCookMultiplier(0.01));
		assertEquals(20.0, 1.0 / MachineUpgradeHelper.clampCookMultiplier(0.01));
	}

	@Test
	void generatorSpeedUsesOutputMultiplierInsteadOfFuelDuration() {
		assertEquals(20.0, 1.0 / MachineUpgradeHelper.clampCookMultiplier(0.01));
		assertEquals(0.1, 1.0 / MachineUpgradeHelper.clampCookMultiplier(20.0));
	}
}
