package net.crystalnexus.procedures;

final class CenteredMultiblockDimensions {
	static final int MAX_RADIUS = 3;

	private CenteredMultiblockDimensions() {
	}

	static int radiusBetween(int coreX, int coreY, int coreZ, int controllerX, int controllerY, int controllerZ) {
		if (coreY != controllerY) {
			return 0;
		}
		int dx = Math.abs(coreX - controllerX);
		int dz = Math.abs(coreZ - controllerZ);
		int radius = dx + dz;
		return dx != 0 && dz != 0 || radius < 1 || radius > MAX_RADIUS ? 0 : radius;
	}

	static boolean isShellOffset(int dx, int dy, int dz, int radius) {
		return Math.abs(dx) <= radius && Math.abs(dy) <= radius && Math.abs(dz) <= radius
				&& (Math.abs(dx) == radius || Math.abs(dy) == radius || Math.abs(dz) == radius);
	}

	static int sizeMultiplier(int radius) {
		return switch (radius) {
			case 2 -> 2;
			case 3 -> 4;
			default -> 1;
		};
	}
}
