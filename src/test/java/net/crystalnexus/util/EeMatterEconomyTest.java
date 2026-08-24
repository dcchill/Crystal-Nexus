package net.crystalnexus.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EeMatterEconomyTest {
	@Test
	void everyReactionChamberScaleLosesEnergyOnExtraction() {
		for (int output = 1; output <= 4; output++) {
			assertTrue(EeMatterEconomy.creationCost(output) > EeMatterEconomy.EXTRACTION_FE_PER_ITEM * output);
		}
	}
}
