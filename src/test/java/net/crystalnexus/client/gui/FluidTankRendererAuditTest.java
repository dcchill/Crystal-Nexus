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
				"FluidChemicalReactionChamberGUIScreen", "FluidInputGuiScreen", "FluidPackagerGUIScreen",
				"GravitationalArrayScreen", "NodeExtractorGUIScreen", "PistonGenGUIScreen",
				"PlasmaGeneratorScreen", "ReactorGUIScreen", "RefineryScreen", "SolarEngineScreen",
				"SteamChamberGUIScreen", "SteamEngineGUIScreen", "TemporalExploiterScreen"))
			assertTrue(Files.readString(GUI.resolve(screen + ".java")).contains("FluidTankRenderer.draw"), screen);

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
