package net.crystalnexus.client;

import net.crystalnexus.client.gui.StructureTrackerScreen;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class StructureTrackerClient {
	private StructureTrackerClient() {
	}

	public static void openSelection(List<String> structures, String selected) {
		Minecraft.getInstance().setScreen(new StructureTrackerScreen(structures, selected));
	}
}
