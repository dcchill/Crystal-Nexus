package net.crystalnexus.block;

final class FleshTakeover {
	static final int COMPLETE_STAGE = 3;

	private FleshTakeover() {
	}

	static int nextStage(int stage) {
		return Math.min(stage + 1, COMPLETE_STAGE);
	}
}
