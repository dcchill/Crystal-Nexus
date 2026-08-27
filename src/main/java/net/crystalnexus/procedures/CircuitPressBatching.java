package net.crystalnexus.procedures;

final class CircuitPressBatching {
	private CircuitPressBatching() {
	}

	static int basicBatchSize(int inputCount, int materialCount, int outputSpace, int outputPerCraft) {
		if (outputPerCraft <= 0) return 0;
		return Math.max(0, Math.min(8, Math.min(inputCount,
			Math.min(materialCount, outputSpace / outputPerCraft))));
	}
}
