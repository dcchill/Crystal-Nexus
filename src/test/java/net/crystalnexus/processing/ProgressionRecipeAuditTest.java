package net.crystalnexus.processing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRecipeAuditTest {
    private static final Path RECIPES = Path.of("src/main/resources/data/crystalnexus/recipe");

    @Test
    void chlorophyteStartsAtCrystalProcessingWithoutVanillaSmeltingBypass() throws IOException {
        String profile = compact(Path.of(
            "src/main/resources/data/crystalnexus/crystalnexus/material_processing/chlorophyte.json"));
        assertTrue(profile.contains("\"minimum_machine_tier\":1"));
        assertTrue(profile.contains("\"disabled_auto_stages\":[\"slurry\"]"));

        List<Path> bypasses;
        try (var files = Files.list(RECIPES)) {
            bypasses = files.filter(path -> path.toString().endsWith(".json")).filter(path -> {
                try {
                    String recipe = compact(path);
                    boolean vanillaCooking = recipe.contains("\"type\":\"minecraft:smelting\"")
                        || recipe.contains("\"type\":\"minecraft:blasting\"");
                    return vanillaCooking && (recipe.contains("raw_chlorophyte")
                        || recipe.contains("chlorophyte_ore"));
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            }).toList();
        }
        assertTrue(bypasses.isEmpty(), () -> "Direct Chlorophyte cooking bypasses: " + bypasses);
    }

    @Test
    void fabricatedElectricalAndControlPartsKeepTheirProgressionGates() throws IOException {
        assertContains("electric_motor_recipe.json", "c:plates/copper", "crystalnexus:stator");
        assertContains("stator_recipe.json", "c:rods/copper");
        assertContains("circuit_press_recipe.json", "c:plates/gold", "crystalnexus:chlorophyte_machine_frame");
        assertContains("computation_cluster_recipe.json", "c:rods/gold", "crystalnexus:computation_node");
        assertContains("blueprint_controller_recipe.json", "c:plates/gold", "crystalnexus:carbon_fiber");
    }

    @Test
    void hyperProcessingUsesCarbonBoltsAndPreviousTierMachines() throws IOException {
        for (String recipe : List.of("hyper_crusher_recipe.json", "hyper_dust_separator_recipe.json",
            "hyper_refinery_recipe.json", "hyper_machine_frame_recipe.json")) {
            String json = compact(RECIPES.resolve(recipe));
            assertTrue(json.contains("crystalnexus:machine_bolt"), recipe);
            assertFalse(json.contains("crystalnexus:iron_machine_bolt"), recipe);
        }
        assertContains("hyper_crusher_recipe.json", "crystalnexus:invertium_crusher");
        assertContains("hyper_dust_separator_recipe.json", "crystalnexus:invertium_dust_separator");
        assertContains("hyper_refinery_recipe.json", "crystalnexus:invertium_refinery");
    }

    @Test
    void lateGameMachinesAndMultiblockPortsRemainCraftable() throws IOException {
        assertContains("machine_energy_output_recipe.json", "crystalnexus:machine_energy_output",
            "crystalnexus:energy_cable_mk_2");
        assertContains("multiblock_item_output_recipe.json", "crystalnexus:multiblock_item_output",
            "crystalnexus:smart_splitter");
        assertContains("gravity_control_point_recipe.json", "crystalnexus:gravity_control_point",
            "crystalnexus:energy_singularity", "c:plates/titanium");
        assertContains("gravitational_array_controller_recipe.json", "crystalnexus:gravitational_array_controller",
            "crystalnexus:gravity_control_point", "crystalnexus:singularity_compressor");
        assertContains("solar_simulator_controller_recipe.json", "crystalnexus:solar_simulator_controller",
            "crystalnexus:multiblock_item_output", "crystalnexus:hyper_machine_frame");
        assertContains("solar_engine_controller_recipe.json", "crystalnexus:solar_engine_controller",
            "crystalnexus:machine_energy_output", "crystalnexus:machine_fluid_input", "crystalnexus:tungsten");
    }

    @Test
    void tieredMachineRecipesFollowTheirMaterialProgression() throws IOException {
        assertContains("iron_smelter_recipe.json", "crystalnexus:machine_frame");
        assertContains("crystal_smelter_recipe.json", "crystalnexus:crystal_machine_frame",
            "crystalnexus:iron_smelter");
        assertContains("chlorophyte_smelter_recipe.json", "crystalnexus:chlorophyte_machine_frame",
            "crystalnexus:crystal_smelter");
        assertContains("invertium_smelter_recipe.json", "crystalnexus:invertium_machine_frame",
            "crystalnexus:chlorophyte_smelter");

        assertContains("crafting_factory_recipe.json", "crystalnexus:iron_machine_frame");
        assertContains("crystal_crafting_factory_recipe.json", "crystalnexus:ancient_crystal",
            "crystalnexus:crafting_factory");
        assertContains("titanium_crafting_factory_recipe.json", "c:plates/titanium",
            "crystalnexus:crystal_crafting_factory");
		assertContains("titanium_extractinator_recipe.json", "c:plates/titanium",
			"crystalnexus:extractinator", "crystalnexus:titanium_machine_frame");
		assertContains("refining_wolframite.json", "crystalnexus:wolframite", "crystalnexus:nitrogen",
			"crystalnexus:tungsten_dust", "minimum_machine_tier\":3");
		assertContains("tungsten_arc_furnace.json", "crystalnexus:tungsten_dust",
			"crystalnexus:hot_tungsten");
		assertContains("extractinator_cobbled_deepslate.json", "crystalnexus:wolframite");

        assertContains("circuit_press_recipe.json", "crystalnexus:chlorophyte_machine_frame");
        assertContains("titanium_carbide_circuit_press_recipe.json",
            "crystalnexus:titanium_carbide_machine_frame");
    }

	@Test
	void balanceFixesStayOnCentralizedCounts() throws IOException {
		String oreProcessor = Files.readString(Path.of(
			"src/main/java/net/crystalnexus/procedures/OreProcessorOnTickUpdateProcedure.java"));
		assertTrue(oreProcessor.contains("CrushingRecipeSupport.findResult"));
		assertTrue(oreProcessor.contains("MaterialProcessingCatalog.NUGGETS_PER_DUST"));
		assertFalse(oreProcessor.contains("outputAmount = 4"));
		assertFalse(oreProcessor.contains("outputAmount2 = 14"));

		String ultimaSmelter = Files.readString(Path.of(
			"src/main/java/net/crystalnexus/procedures/UltimaSmelterOnTickUpdateProcedure.java"));
		assertTrue(ultimaSmelter.contains("int[] INPUT_SLOTS = {0, 3, 5, 6}"));
		assertTrue(ultimaSmelter.contains("if (results[lane].isEmpty())"));

		String overfuel = compact(RECIPES.resolve("fluid_chemical_reaction_overfuel.json"));
		assertTrue(overfuel.contains("\"item_input_2_count\":3"));
		assertFalse(overfuel.contains("\"coal_block\",\"count\""));
	}

	@Test
	void titaniumExtractinatorKeepsItsTieredEnergyAndDropBonus() throws IOException {
		String procedure = Files.readString(Path.of(
			"src/main/java/net/crystalnexus/procedures/ExtractinatorOnTickUpdateProcedure.java"));
		assertTrue(procedure.contains("MachineTier.from(world.getBlockState(pos)).energyCost"));
		assertTrue(procedure.contains("MachineTier.INVERTIUM_TITANIUM ? 2 : 1"));
		assertTrue(procedure.contains("CrystalnexusModItems.WOLFRAMITE"));
	}

    private static void assertContains(String recipe, String... values) throws IOException {
        String json = compact(RECIPES.resolve(recipe));
        for (String value : values) assertTrue(json.contains(value), recipe + " must contain " + value);
    }

    private static String compact(Path path) throws IOException {
        return Files.readString(path).replaceAll("\\s", "");
    }
}
