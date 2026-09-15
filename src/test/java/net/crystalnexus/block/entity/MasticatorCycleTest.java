package net.crystalnexus.block.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasticatorCycleTest {
	@Test
	void completesAndResetsOnTheFortiethTick() {
		assertFalse(MasticatorCycle.completes(38, 40));
		assertEquals(39, MasticatorCycle.advance(38, 40));
		assertTrue(MasticatorCycle.completes(39, 40));
		assertEquals(0, MasticatorCycle.advance(39, 40));
	}
}
