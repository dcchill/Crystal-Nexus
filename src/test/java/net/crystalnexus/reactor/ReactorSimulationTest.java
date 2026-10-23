package net.crystalnexus.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReactorSimulationTest {
	@Test
	void heatScalesWithDeliveredLoadAndCapsAtFullLoad() {
		assertEquals(0.20, ReactorBalance.demandHeatFactor(0, 1000), 1e-9);
		assertEquals(0.60, ReactorBalance.demandHeatFactor(500, 1000), 1e-9);
		assertEquals(1.00, ReactorBalance.demandHeatFactor(1000, 1000), 1e-9);
		assertEquals(1.00, ReactorBalance.demandHeatFactor(2000, 1000), 1e-9);
		assertEquals(0.20, ReactorBalance.demandHeatFactor(1000, 0), 1e-9);
	}

	@Test
	void darkMatterCellsResonateByTheirTotalCount() {
		assertEquals(1, ReactorBalance.darkMatterResonanceMultiplier(0));
		assertEquals(1, ReactorBalance.darkMatterResonanceMultiplier(1));
		assertEquals(6, ReactorBalance.darkMatterResonanceMultiplier(6));
	}
}
