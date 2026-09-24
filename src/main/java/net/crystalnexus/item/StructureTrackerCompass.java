package net.crystalnexus.item;

public final class StructureTrackerCompass {
	private static final int SIZE = 15;

	private StructureTrackerCompass() {
	}

	public static String bar(double lookX, double lookZ, double targetX, double targetZ) {
		double lookLength = Math.hypot(lookX, lookZ);
		double targetLength = Math.hypot(targetX, targetZ);
		if (lookLength < 1.0e-6 || targetLength < 1.0e-6) {
			return "=======<>=======";
		}
		lookX /= lookLength;
		lookZ /= lookLength;
		targetX /= targetLength;
		targetZ /= targetLength;
		double dot = lookX * targetX + lookZ * targetZ;
		if (dot < 0) {
			return "===============";
		}
		double cross = lookX * targetZ - lookZ * targetX;
		int center = SIZE / 2;
		int pointer = (int) Math.round(center + Math.atan2(-cross, dot) / (Math.PI / 2) * center);
		pointer = Math.max(0, Math.min(SIZE - 1, pointer));

		StringBuilder bar = new StringBuilder(SIZE + 1);
		for (int i = 0; i < SIZE; i++) {
			bar.append(i == pointer ? "<>" : "=");
		}
		return bar.toString();
	}
}
