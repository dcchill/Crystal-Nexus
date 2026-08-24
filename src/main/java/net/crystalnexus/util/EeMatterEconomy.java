package net.crystalnexus.util;

public final class EeMatterEconomy {
	public static final int CREATION_FE_PER_ITEM = 10_240_000;
	public static final int EXTRACTION_FE_PER_ITEM = 4_096_000;

	private EeMatterEconomy() {
	}

	public static int creationCost(int outputCount) {
		return Math.multiplyExact(CREATION_FE_PER_ITEM, Math.clamp(outputCount, 1, 4));
	}
}
