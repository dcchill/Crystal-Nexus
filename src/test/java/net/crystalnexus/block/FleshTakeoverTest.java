package net.crystalnexus.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FleshTakeoverTest {
	@Test
	void advancesUntilTheTakeoverIsComplete() {
		assertEquals(1, FleshTakeover.nextStage(0));
		assertEquals(2, FleshTakeover.nextStage(1));
		assertEquals(3, FleshTakeover.nextStage(2));
		assertEquals(3, FleshTakeover.nextStage(3));
	}
}
