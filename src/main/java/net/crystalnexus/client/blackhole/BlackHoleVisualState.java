package net.crystalnexus.client.blackhole;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlackHoleVisualState {
	private static final List<Visual> VISUALS = new ArrayList<>();

	public static void add(double x, double y, double z, double radius, int durationTicks) {
		long startTick = 0L;
		if (Minecraft.getInstance().level != null) {
			startTick = Minecraft.getInstance().level.getGameTime();
		}
		VISUALS.add(new Visual(new Vec3(x, y, z), radius, durationTicks, startTick));
	}

	public static List<Visual> active(long gameTime) {
		Iterator<Visual> iterator = VISUALS.iterator();
		while (iterator.hasNext()) {
			Visual visual = iterator.next();
			if (visual.age(gameTime) > visual.durationTicks()) {
				iterator.remove();
			}
		}
		return List.copyOf(VISUALS);
	}

	public record Visual(Vec3 center, double radius, int durationTicks, long startTick) {
		public double age(long gameTime) {
			return gameTime - startTick;
		}
	}
}
