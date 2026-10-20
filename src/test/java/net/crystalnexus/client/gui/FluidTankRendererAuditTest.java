package net.crystalnexus.client.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidTankRendererAuditTest {
	private static final Path GUI = Path.of("src/main/java/net/crystalnexus/client/gui");

	@Test void everyFluidGaugeUsesTheSharedAnimatedRenderer() throws IOException {
		for (String screen : List.of("CircuitPressGUIScreen", "CryogenicFlashFreezerScreen",
				"FluidChemicalReactionChamberGUIScreen", "FluidPackagerGUIScreen",
				"GravitationalArrayScreen", "NodeExtractorGUIScreen", "PistonGenGUIScreen",
				"PlasmaGeneratorScreen", "ReactorGUIScreen", "RefineryScreen", "SolarEngineScreen",
				"SteamChamberGUIScreen", "SteamEngineGUIScreen", "TemporalExploiterScreen")) {
			String source = Files.readString(GUI.resolve(screen + ".java"));
			// machineTank/machineBlockTank are HolographicMachineScreen's wrappers around FluidTankRenderer.
			assertTrue(source.contains("FluidTankRenderer.draw") || source.contains("machineTank(")
					|| source.contains("machineBlockTank("), screen);
		}
		assertTrue(Files.readString(GUI.resolve("HolographicMachineScreen.java")).contains("FluidTankRenderer.draw"));

		try (var files = Files.list(GUI)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				if (file.endsWith("FluidTankRenderer.java")) continue;
				String source = Files.readString(file);
				assertFalse(source.contains("IClientFluidTypeExtensions"), file.toString());
				assertFalse(source.contains("FluidDisplayProcedure"), file.toString());
			}
		}
	}
}
