package net.crystalnexus.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReactorSimulationTest {
	@Test
	void darkMatterCellsResonateByTheirTotalCount() {
		assertEquals(1, ReactorBalance.darkMatterResonanceMultiplier(0));
		assertEquals(1, ReactorBalance.darkMatterResonanceMultiplier(1));
		assertEquals(6, ReactorBalance.darkMatterResonanceMultiplier(6));
	}
}
