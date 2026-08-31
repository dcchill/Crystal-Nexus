package net.crystalnexus.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuarryChunkSelectionTest {
	@Test
	void clampsToSevenBySevenAndIncludesCenter() {
		var chunks = QuarryChunkSelection.offsets(99, 99);
		assertEquals(49, chunks.size());
		assertTrue(chunks.contains(new QuarryChunkSelection.ChunkOffset(0, 0)));
	}

	@Test
	void evenSizesBiasExtraChunksEastAndSouth() {
		assertEquals(-1, QuarryChunkSelection.minOffset(4));
		assertEquals(2, QuarryChunkSelection.maxOffset(4));
		var chunks = QuarryChunkSelection.offsets(2, 4);
		assertEquals(8, chunks.size());
		assertTrue(chunks.contains(new QuarryChunkSelection.ChunkOffset(1, 2)));
	}

	@Test
	void blockOrderTraversesEachLayerFromOutsideToMiddle() {
		var blocks = QuarryChunkSelection.blockOffsetsOuterToInner(1, 1);
		assertEquals(16 * 16, blocks.size());
		assertEquals(blocks.size(), new HashSet<>(blocks).size());
		assertEquals(new QuarryChunkSelection.BlockOffset(0, 0), blocks.getFirst());
		assertEquals(new QuarryChunkSelection.BlockOffset(7, 8), blocks.getLast());

		int previousRing = -1;
		for (var block : blocks) {
			int ring = Math.min(Math.min(block.x(), 15 - block.x()), Math.min(block.z(), 15 - block.z()));
			assertTrue(ring >= previousRing, "Traversal must never return to an outer ring");
			previousRing = ring;
		}
		assertEquals(49 * 16 * 16, QuarryChunkSelection.blockOffsetsOuterToInner(7, 7).size());
	}
}
