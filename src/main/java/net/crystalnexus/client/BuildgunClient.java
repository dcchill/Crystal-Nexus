package net.crystalnexus.client;

import net.crystalnexus.client.gui.BuildgunSchematicScreen;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class BuildgunClient {
	private BuildgunClient() {
	}

	public static void openSchematicMenu(List<String> schematics) {
		Minecraft.getInstance().setScreen(new BuildgunSchematicScreen(schematics));
	}
}
