package net.crystalnexus.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.crystalnexus.world.inventory.MachineGuiLayout;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class MachineGuiCoverageTest {
    private static final Path ROOT = Path.of("src/main/java/net/crystalnexus");
    private JsonObject manifest() throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/crystalnexus/machine_gui_tiers.json"))).getAsJsonObject();
    }
    @Test void eachMigratedScreenHasExplicitRecipeAssignmentsAndProtectedScreensDoNotOptIn() throws Exception {
        var manifest = manifest();
        var screens = manifest.getAsJsonObject("screens");
        var machines = manifest.getAsJsonObject("machines");
        for (var entry : screens.entrySet()) {
            String source = Files.readString(ROOT.resolve("client/gui/" + entry.getKey() + ".java"));
            assertTrue(source.contains("extends HolographicMachineScreen<"), entry.getKey());
            var ids = entry.getValue().getAsJsonObject().getAsJsonArray("machines");
            assertTrue(!ids.isEmpty() || entry.getKey().equals("OreGenGUIScreen"), entry.getKey());
            for (var id : ids) assertTrue(machines.has(id.getAsString()), id.toString());
        }
        try (var files = Files.list(ROOT.resolve("client/gui"))) {
            for (Path file : files.filter(p -> p.toString().endsWith("Screen.java")).toList()) {
                if (Files.readString(file).contains("extends HolographicMachineScreen<"))
                    assertTrue(screens.has(file.getFileName().toString().replace(".java", "")), file.toString());
            }
        }
        for (var screen : manifest.getAsJsonArray("protectedScreens")) {
            String source = Files.readString(ROOT.resolve("client/gui/" + screen.getAsString() + ".java"));
            assertFalse(source.contains("HolographicMachineScreen"), screen.toString());
            assertFalse(source.contains("MachineGuiTheme"), screen.toString());
        }
        for (var machine : machines.entrySet()) {
            var data = machine.getValue().getAsJsonObject();
            assertTrue(data.get("tier").getAsInt() >= 0 && data.get("tier").getAsInt() <= 8);
            if (!data.get("recipe").isJsonNull()) assertTrue(Files.exists(Path.of("src/main/resources/data/crystalnexus/recipe", data.get("recipe").getAsString())));
        }
    }
    @Test void frameAndCustomRecipeAssignmentsUseCraftingMaterialsRatherThanLegacyNames() throws Exception {
        var machines = manifest().getAsJsonObject("machines");
        for (String id : new String[]{"invertium_crusher", "invertium_refinery"}) assertEquals(4, machines.getAsJsonObject(id).get("tier").getAsInt());
        for (String id : new String[]{"hyper_crusher", "quantum_miner", "singularity_matrix"}) assertEquals(8, machines.getAsJsonObject(id).get("tier").getAsInt());
        assertEquals(5, machines.getAsJsonObject("tesseract").get("tier").getAsInt());
        assertEquals(1, machines.getAsJsonObject("parts_assembler").get("tier").getAsInt());
    }
    @Test void explicitMachineSlotPositionsNeverOverlap() throws Exception {
        var pattern = Pattern.compile("new (?:SlotItemHandler|MachineItemSlot|Slot)\\(\\w+, (\\d+), (-?\\d+), (-?\\d+)\\)");
        for (var entry : manifest().getAsJsonObject("screens").entrySet()) {
            String menu = entry.getValue().getAsJsonObject().get("menu").getAsString();
            String source = Files.readString(ROOT.resolve("world/inventory/" + menu + ".java"));
            var matcher = pattern.matcher(source);
            var bounds = new ArrayList<MachineGuiLayout.Rect>();
            while (matcher.find()) bounds.add(MachineGuiLayout.slot(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))));
            for (int i = 0; i < bounds.size(); i++) for (int j = i + 1; j < bounds.size(); j++)
                assertFalse(bounds.get(i).intersects(bounds.get(j)), menu + ": " + bounds.get(i) + " overlaps " + bounds.get(j));
        }
    }
}
