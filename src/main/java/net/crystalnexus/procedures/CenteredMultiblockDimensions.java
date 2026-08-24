package net.crystalnexus.procedures;

import net.minecraft.core.BlockPos;

final class CenteredMultiblockDimensions {
	static final int MAX_DIMENSION = 17;
	static final long MAX_VOLUME = 4_913;

	private CenteredMultiblockDimensions() {
	}

	static boolean isValidBounds(BlockPos min, BlockPos max) {
		return isValidBounds(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
	}

	static boolean isValidBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		int sizeX = size(minX, maxX);
		int sizeY = size(minY, maxY);
		int sizeZ = size(minZ, maxZ);
		return sizeX >= 3 && sizeY >= 3 && sizeZ >= 3 && sizeX <= MAX_DIMENSION && sizeY <= MAX_DIMENSION
				&& sizeZ <= MAX_DIMENSION && (long) sizeX * sizeY * sizeZ <= MAX_VOLUME;
	}

	static boolean isInside(BlockPos pos, BlockPos min, BlockPos max) {
		return isInside(pos.getX(), pos.getY(), pos.getZ(), min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
	}

	static boolean isInside(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return x > minX && x < maxX && y > minY && y < maxY && z > minZ && z < maxZ;
	}

	static boolean isShellPosition(BlockPos pos, BlockPos min, BlockPos max) {
		return isShellPosition(pos.getX(), pos.getY(), pos.getZ(), min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
	}

	static boolean isShellPosition(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ
				&& (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
	}

	static BlockPos center(BlockPos min, BlockPos max) {
		return new BlockPos(Math.floorDiv(min.getX() + max.getX(), 2),
				Math.floorDiv(min.getY() + max.getY(), 2),
				Math.floorDiv(min.getZ() + max.getZ(), 2));
	}

	/** Legacy scaling value retained for ports and reaction-chamber output. */
	static int legacyRadius(BlockPos min, BlockPos max) {
		return legacyRadius(size(min.getX(), max.getX()), size(min.getY(), max.getY()), size(min.getZ(), max.getZ()));
	}

	static int legacyRadius(int sizeX, int sizeY, int sizeZ) {
		return Math.max(sizeX, Math.max(sizeY, sizeZ)) / 2;
	}

	static int sizeMultiplier(int radius) {
		return 1 << Math.max(0, Math.min(30, radius - 1));
	}

	static int reactionOutputMultiplier(BlockPos min, BlockPos max) {
		return reactionOutputMultiplier(size(min.getX(), max.getX()), size(min.getY(), max.getY()), size(min.getZ(), max.getZ()));
	}

	static int reactionOutputMultiplier(int sizeX, int sizeY, int sizeZ) {
		long interiorVolume = (long) Math.max(1, sizeX - 2) * Math.max(1, sizeY - 2) * Math.max(1, sizeZ - 2);
		if (interiorVolume <= 1) return 1;
		if (interiorVolume <= 64) return 2;
		if (interiorVolume <= 216) return 3;
		return 4;
	}

	private static int size(int min, int max) {
		return max - min + 1;
	}
}
