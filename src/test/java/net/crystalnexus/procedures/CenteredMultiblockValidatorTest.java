package net.crystalnexus.procedures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CenteredMultiblockValidatorTest {
	@Test
	void acceptsCenteredThreeFiveAndSevenBlockStructures() {
		assertEquals(1, CenteredMultiblockDimensions.radiusBetween(0, 0, 0, 1, 0, 0));
		assertEquals(2, CenteredMultiblockDimensions.radiusBetween(0, 0, 0, 0, 0, -2));
		assertEquals(3, CenteredMultiblockDimensions.radiusBetween(0, 0, 0, -3, 0, 0));
	}

	@Test
	void rejectsOversizedOrOffCenterControllers() {
		assertEquals(0, CenteredMultiblockDimensions.radiusBetween(0, 0, 0, 4, 0, 0));
		assertEquals(0, CenteredMultiblockDimensions.radiusBetween(0, 0, 0, 3, 0, 1));
		assertEquals(0, CenteredMultiblockDimensions.radiusBetween(0, 0, 0, 0, 1, 3));
	}

	@Test
	void identifiesTheWholeSevenBlockShell() {
		assertTrue(CenteredMultiblockDimensions.isShellOffset(3, 0, 0, 3));
		assertTrue(CenteredMultiblockDimensions.isShellOffset(1, -3, 2, 3));
		assertTrue(CenteredMultiblockDimensions.isShellOffset(2, 1, 3, 3));
		assertFalse(CenteredMultiblockDimensions.isShellOffset(2, 2, 2, 3));
		assertFalse(CenteredMultiblockDimensions.isShellOffset(4, 0, 3, 3));
	}

	@Test
	void scalesThreeFiveAndSevenBlockStructures() {
		assertEquals(1, CenteredMultiblockDimensions.sizeMultiplier(1));
		assertEquals(2, CenteredMultiblockDimensions.sizeMultiplier(2));
		assertEquals(4, CenteredMultiblockDimensions.sizeMultiplier(3));
	}
}
