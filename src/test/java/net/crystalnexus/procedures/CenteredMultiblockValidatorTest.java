package net.crystalnexus.procedures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CenteredMultiblockValidatorTest {
	@Test
	void acceptsCubeAndRectangularBounds() {
		assertTrue(validSize(3, 3, 3));
		assertTrue(validSize(5, 5, 5));
		assertTrue(validSize(7, 7, 7));
		assertTrue(validSize(5, 5, 7));
		assertTrue(validSize(7, 5, 7));
		assertTrue(validSize(3, 7, 5));
	}

	@Test
	void rejectsAnyDimensionBelowThree() {
		assertFalse(validSize(2, 5, 7));
		assertFalse(validSize(5, 1, 7));
		assertFalse(validSize(5, 7, 2));
	}

	@Test
	void identifiesRectangularShellAndInteriorPositions() {
		assertTrue(CenteredMultiblockDimensions.isShellPosition(4, 3, 1, 0, 0, 0, 4, 6, 2));
		assertTrue(CenteredMultiblockDimensions.isShellPosition(2, 6, 1, 0, 0, 0, 4, 6, 2));
		assertFalse(CenteredMultiblockDimensions.isShellPosition(2, 3, 1, 0, 0, 0, 4, 6, 2));
		assertTrue(CenteredMultiblockDimensions.isInside(2, 3, 1, 0, 0, 0, 4, 6, 2));
	}

	@Test
	void retainsLegacyScalingFromLargestDimension() {
		assertEquals(1, CenteredMultiblockDimensions.legacyRadius(3, 3, 3));
		assertEquals(3, CenteredMultiblockDimensions.legacyRadius(5, 5, 7));
		assertEquals(4, CenteredMultiblockDimensions.sizeMultiplier(3));
	}

	private static boolean validSize(int x, int y, int z) {
		return CenteredMultiblockDimensions.isValidBounds(0, 0, 0, x - 1, y - 1, z - 1);
	}
}
