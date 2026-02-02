package net.crystalnexus.client.orescanner;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public class OreOutlineClient {

	private static final List<BlockPos> OUTLINED = new ArrayList<>();
	private static long expireGameTime = 0;
	private static long pulseStartTime = 0;
	private static int pulseDuration = 0;

	public static void onScannerResult(List<BlockPos> positions, int durationTicks) {
		OUTLINED.clear();
		OUTLINED.addAll(positions);

		var level = Minecraft.getInstance().level;
		if (level != null) {
			expireGameTime = level.getGameTime() + durationTicks;
			pulseStartTime = level.getGameTime();
			pulseDuration = durationTicks;
		}
	}

	public static boolean isActive() {
		var level = Minecraft.getInstance().level;
		if (level == null) return false;
		long now = level.getGameTime();
		if (now > expireGameTime) {
			OUTLINED.clear();
			return false;
		}
		return !OUTLINED.isEmpty();
	}

	public static List<BlockPos> getOutlined() {
		return OUTLINED;
	}

	public static float getFade(float partialTick) {
		var level = Minecraft.getInstance().level;
		if (level == null || pulseDuration <= 0) return 1f;
		long now = level.getGameTime();
		float t = (float)((now - pulseStartTime) + partialTick) / (float)pulseDuration;
		return 1.0f - Mth.clamp(t, 0f, 1f);
	}
}
