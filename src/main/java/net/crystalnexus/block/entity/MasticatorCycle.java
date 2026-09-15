package net.crystalnexus.block.entity;

final class MasticatorCycle {
	private MasticatorCycle() {
	}

	static boolean completes(int progress, int requiredTicks) {
		return progress + 1 >= requiredTicks;
	}

	static int advance(int progress, int requiredTicks) {
		return completes(progress, requiredTicks) ? 0 : progress + 1;
	}
}
