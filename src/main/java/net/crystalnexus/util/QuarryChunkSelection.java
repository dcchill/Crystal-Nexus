package net.crystalnexus.util;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public final class QuarryChunkSelection {
	public static final int MAX_SIZE = 7;

	private QuarryChunkSelection() {
	}

	public static int clampSize(int size) {
		return Math.max(1, Math.min(MAX_SIZE, size));
	}

	public static int minOffset(int size) {
		return -(clampSize(size) - 1) / 2;
	}

	public static int maxOffset(int size) {
		return clampSize(size) / 2;
	}

	public static List<ChunkPos> chunks(ChunkPos center, int width, int depth) {
		return offsets(width, depth).stream()
			.map(offset -> new ChunkPos(center.x + offset.x(), center.z + offset.z()))
			.toList();
	}

	public static List<ChunkOffset> offsets(int width, int depth) {
		List<ChunkOffset> chunks = new ArrayList<>(clampSize(width) * clampSize(depth));
		for (int x = minOffset(width); x <= maxOffset(width); x++) {
			for (int z = minOffset(depth); z <= maxOffset(depth); z++) {
				chunks.add(new ChunkOffset(x, z));
			}
		}
		return List.copyOf(chunks);
	}

	public static boolean containsOffset(int x, int z, int width, int depth) {
		return x >= minOffset(width) && x <= maxOffset(width)
			&& z >= minOffset(depth) && z <= maxOffset(depth);
	}

	public static List<BlockOffset> blockOffsetsOuterToInner(int width, int depth) {
		int minX = minOffset(width) * 16;
		int maxX = (maxOffset(width) + 1) * 16 - 1;
		int minZ = minOffset(depth) * 16;
		int maxZ = (maxOffset(depth) + 1) * 16 - 1;
		List<BlockOffset> blocks = new ArrayList<>((maxX - minX + 1) * (maxZ - minZ + 1));
		while (minX <= maxX && minZ <= maxZ) {
			for (int x = minX; x <= maxX; x++) blocks.add(new BlockOffset(x, minZ));
			for (int z = minZ + 1; z <= maxZ; z++) blocks.add(new BlockOffset(maxX, z));
			if (minZ < maxZ) for (int x = maxX - 1; x >= minX; x--) blocks.add(new BlockOffset(x, maxZ));
			if (minX < maxX) for (int z = maxZ - 1; z > minZ; z--) blocks.add(new BlockOffset(minX, z));
			minX++;
			maxX--;
			minZ++;
			maxZ--;
		}
		return List.copyOf(blocks);
	}

	public record ChunkOffset(int x, int z) {
	}

	public record BlockOffset(int x, int z) {
	}
}
