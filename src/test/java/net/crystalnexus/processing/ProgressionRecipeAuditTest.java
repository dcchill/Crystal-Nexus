package net.crystalnexus.processing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRecipeAuditTest {
    private static final Path RECIPES = Path.of("src/main/resources/data/crystalnexus/recipe");
    private static final Pattern MINIMUM_AGE = Pattern.compile("\\\"minimum_age\\\"\\s*:\\s*(-?\\d+)");

    @Test
    void fabricatedElectricalAndControlPartsKeepTheirProgressionGates() throws IOException {
        assertContains("electric_motor_recipe.json", "c:plates/copper", "crystalnexus:stator");
        assertContains("stator_recipe.json", "c:rods/copper");
        assertContains("circuit_press_recipe.json", "c:plates/gold", "crystalnexus:titanium_machine_frame");
        assertContains("computation_cluster_recipe.json", "c:rods/gold", "crystalnexus:computation_node");
        assertContains("blueprint_controller_recipe.json", "c:plates/gold", "crystalnexus:carbon_fiber");
    }

    @Test
    void hyperProcessingUsesCarbonBoltsAndPreviousAgeMachines() throws IOException {
        for (String recipe : List.of("hyper_crusher_recipe.json", "hyper_dust_separator_recipe.json",
            "hyper_refinery_recipe.json")) {
            String json = compact(RECIPES.resolve(recipe));
            assertTrue(json.contains("crystalnexus:machine_bolt"), recipe);
            assertFalse(json.contains("crystalnexus:iron_machine_bolt"), recipe);
        }
        assertContains("hyper_crusher_recipe.json", "crystalnexus:tungsten_crusher");
        assertContains("hyper_dust_separator_recipe.json", "crystalnexus:tungsten_dust_separator");
        assertContains("hyper_refinery_recipe.json", "crystalnexus:tungsten_refinery");
    }

    @Test
    void lateGameMachinesAndMultiblockPortsRemainCraftable() throws IOException {
        assertContains("machine_energy_output_recipe.json", "crystalnexus:machine_energy_output",
            "crystalnexus:energy_cable_mk_2");
        assertContains("multiblock_item_output_recipe.json", "crystalnexus:multiblock_item_output",
            "crystalnexus:smart_splitter");
        assertContains("gravity_control_point_recipe.json", "crystalnexus:gravity_control_point",
            "crystalnexus:dark_matter", "c:plates/titanium");
        assertContains("gravitational_array_controller_recipe.json", "crystalnexus:gravitational_array_controller",
            "crystalnexus:gravity_control_point", "crystalnexus:singularity_compressor");
        assertContains("solar_simulator_controller_recipe.json", "crystalnexus:solar_simulator_controller",
            "crystalnexus:multiblock_item_output", "crystalnexus:hyper_machine_frame");
        assertContains("solar_engine_controller_recipe.json", "crystalnexus:solar_engine_controller",
            "crystalnexus:machine_energy_output", "crystalnexus:machine_fluid_input", "crystalnexus:tungsten");
    }

    @Test
    void agedMachineRecipesFollowTheirMaterialProgression() throws IOException {
        assertContains("crystal_machine_frame_recipe.json", "crystalnexus:machine_frame");
        assertContains("titanium_machine_frame_recipe.json", "crystalnexus:crystal_machine_frame");
        assertContains("carbon_machine_frame_recipe.json", "crystalnexus:titanium_machine_frame");
        assertContains("titanium_carbide_machine_frame_recipe.json", "crystalnexus:carbon_machine_frame");
        assertContains("tungsten_machine_frame_recipe.json", "crystalnexus:titanium_carbide_machine_frame");
        assertContains("hyper_machine_frame_recipe.json", "crystalnexus:tungsten_machine_frame");

        assertContains("crystal_crusher_recipe.json", "crystalnexus:crystal_machine_frame");
        assertContains("titanium_crusher_recipe.json", "crystalnexus:crystal_crusher",
            "crystalnexus:titanium_machine_frame");
        assertContains("tungsten_crusher_recipe.json", "crystalnexus:titanium_crusher",
            "crystalnexus:tungsten_machine_frame");
        assertContains("hyper_crusher_recipe.json", "crystalnexus:tungsten_crusher",
            "crystalnexus:hyper_machine_frame");

        assertContains("dust_separator_recipe.json", "crystalnexus:crystal_machine_frame");
        assertContains("titanium_dust_separator_recipe.json", "crystalnexus:dust_separator",
            "crystalnexus:titanium_machine_frame");
        assertContains("tungsten_dust_separator_recipe.json", "crystalnexus:titanium_dust_separator",
            "crystalnexus:tungsten_machine_frame");
        assertContains("hyper_dust_separator_recipe.json", "crystalnexus:tungsten_dust_separator",
            "crystalnexus:hyper_machine_frame");

        assertContains("refinery_recipe.json", "crystalnexus:crystal_machine_frame");
        assertContains("titanium_refinery_recipe.json", "crystalnexus:refinery",
            "crystalnexus:titanium_machine_frame");
        assertContains("tungsten_refinery_recipe.json", "crystalnexus:titanium_refinery",
            "crystalnexus:tungsten_machine_frame");
        assertContains("hyper_refinery_recipe.json", "crystalnexus:tungsten_refinery",
            "crystalnexus:hyper_machine_frame");

        assertContains("iron_smelter_recipe.json", "crystalnexus:machine_frame");
        assertContains("crystal_smelter_recipe.json", "crystalnexus:crystal_machine_frame",
            "crystalnexus:iron_smelter");
        assertContains("titanium_smelter_recipe.json", "crystalnexus:titanium_machine_frame",
            "crystalnexus:crystal_smelter");
        assertContains("tungsten_smelter_recipe.json", "crystalnexus:tungsten_machine_frame",
            "crystalnexus:titanium_smelter");

        assertContains("crafting_factory_recipe.json", "crystalnexus:machine_frame");
        assertContains("crystal_crafting_factory_recipe.json", "crystalnexus:ancient_crystal",
            "crystalnexus:crafting_factory", "crystalnexus:crystal_machine_frame");
        assertContains("titanium_crafting_factory_recipe.json", "c:plates/titanium",
            "crystalnexus:crystal_crafting_factory", "crystalnexus:titanium_machine_frame");
		assertContains("titanium_extractinator_recipe.json", "c:plates/titanium",
			"crystalnexus:extractinator", "crystalnexus:titanium_machine_frame");
		assertContains("refining_wolframite.json", "crystalnexus:wolframite", "crystalnexus:nitrogen",
			"crystalnexus:tungsten_dust", "minimum_age\":2");
		assertContains("tungsten_arc_furnace.json", "crystalnexus:tungsten_dust",
			"crystalnexus:hot_tungsten");
		assertContains("extractinator_cobbled_deepslate.json", "crystalnexus:wolframite");

        assertContains("circuit_press_recipe.json", "crystalnexus:titanium_machine_frame");
        assertContains("arc_furnace_recipe.json", "crystalnexus:titanium_sheet",
            "crystalnexus:titanium_machine_frame", "crystalnexus:titanium_ingot");
        assertContains("titanium_carbide_circuit_press_recipe.json",
            "crystalnexus:titanium_carbide_machine_frame", "crystalnexus:circuit_press");
    }

    @Test
    void physicalMachinesUseTheirSeparatedLogicalAges() throws IOException {
        String blocks = Files.readString(Path.of(
            "src/main/java/net/crystalnexus/init/CrystalnexusModBlocks.java"));
        assertTrue(blocks.contains("TITANIUM_CRUSHER = REGISTRY.register(\"titanium_crusher\", () -> new CrystalCrusherBlock(MachineAge.AGE_2))"));
        assertTrue(blocks.contains("TITANIUM_DUST_SEPARATOR = REGISTRY.register(\"titanium_dust_separator\", () -> new DustSeparatorBlock(MachineAge.AGE_2))"));
        assertTrue(blocks.contains("TITANIUM_REFINERY = REGISTRY.register(\"titanium_refinery\", () -> new RefineryBlock(MachineAge.AGE_2))"));
        assertTrue(blocks.contains("TUNGSTEN_CRUSHER = REGISTRY.register(\"tungsten_crusher\", () -> new CrystalCrusherBlock(MachineAge.AGE_3))"));
        assertTrue(blocks.contains("TUNGSTEN_DUST_SEPARATOR = REGISTRY.register(\"tungsten_dust_separator\", () -> new DustSeparatorBlock(MachineAge.AGE_3))"));
        assertTrue(blocks.contains("TUNGSTEN_REFINERY = REGISTRY.register(\"tungsten_refinery\", () -> new RefineryBlock(MachineAge.AGE_3))"));
        assertTrue(blocks.contains("HYPER_CRUSHER = REGISTRY.register(\"hyper_crusher\", () -> new CrystalCrusherBlock(MachineAge.AGE_4))"));
        assertTrue(blocks.contains("HYPER_DUST_SEPARATOR = REGISTRY.register(\"hyper_dust_separator\", () -> new DustSeparatorBlock(MachineAge.AGE_4))"));
        assertTrue(blocks.contains("HYPER_REFINERY = REGISTRY.register(\"hyper_refinery\", () -> new RefineryBlock(MachineAge.AGE_4))"));

        assertTrue(blocks.contains("TUNGSTEN_SMELTER = REGISTRY.register(\"tungsten_smelter\", () -> new TitaniumSmelterBlock(MachineAge.AGE_3))"));

        String arcFurnace = Files.readString(Path.of(
            "src/main/java/net/crystalnexus/block/ArcFurnaceBlock.java"));
        assertTrue(arcFurnace.contains("return MachineAge.AGE_2"));

        String ages = Files.readString(Path.of(
            "src/main/java/net/crystalnexus/processing/MachineAge.java"));
        assertTrue(ages.contains("machine.machineAge() : STARTER"));
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
	void titaniumExtractinatorKeepsItsAgedEnergyAndDropBonus() throws IOException {
		String procedure = Files.readString(Path.of(
			"src/main/java/net/crystalnexus/procedures/ExtractinatorOnTickUpdateProcedure.java"));
		assertTrue(procedure.contains("MachineAge.from(world.getBlockState(pos)).energyCost"));
		assertTrue(procedure.contains("MachineAge.AGE_2 ? 2 : 1"));
		assertTrue(procedure.contains("CrystalnexusModItems.WOLFRAMITE"));
		assertTrue(procedure.contains("rareDrop(world, BlockPos.containing(x, y, z), 64, 5)"));
	}

    @Test
    void oldMachineProgressionVocabularyIsAbsent() throws IOException {
        for (Path root : List.of(Path.of("src/main/java"), Path.of("src/main/resources"))) {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".json")
                                || path.toString().endsWith(".md")).toList()) {
                    String content = Files.readString(path);
                    assertFalse(content.contains("minimum_machine_tier"), path.toString());
                    assertFalse(content.contains("MachineTier"), path.toString());
                    assertFalse(content.contains("TieredMachineBlock"), path.toString());
                }
            }
        }
    }

    @Test
    void everyProcessingRequirementMapsToAnAgeFromOneThroughFour() throws IOException {
        List<Path> roots = List.of(RECIPES,
                Path.of("src/main/resources/data/crystalnexus/crystalnexus/material_processing"));
        for (Path root : roots) {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(value -> value.toString().endsWith(".json")).toList()) {
                    String json = Files.readString(path);
                    var match = MINIMUM_AGE.matcher(json);
                    int required = match.find() ? MachineAge.requireNumber(Integer.parseInt(match.group(1))) : 1;
                    if (json.contains("\"type\": \"crystalnexus:ore_crushing\"")
                            || json.contains("\"type\":\"crystalnexus:ore_crushing\"")
                            || json.contains("\"type\": \"crystalnexus:dust_seperation\"")
                            || json.contains("\"type\":\"crystalnexus:dust_seperation\"")
                            || json.contains("\"type\": \"crystalnexus:refining\"")
                            || json.contains("\"type\":\"crystalnexus:refining\"")) {
                        assertTrue(required >= 1 && required <= 4, path.toString());
                    }
                }
            }
        }
    }

    @Test
    void restoredSystemsAndExcludedProgressionsStayDeliberate() throws IOException {
        String blocks = Files.readString(Path.of("src/main/java/net/crystalnexus/init/CrystalnexusModBlocks.java"));
        assertTrue(blocks.contains("INVERTIUM_ORE"));
        assertTrue(blocks.contains("DEPOT_UPLOADER"));
        assertFalse(blocks.contains("QUANTUM_MINER"));
        assertFalse(blocks.contains("CHLOROPHYTE"));
        assertContains("fluid_chemical_reaction_inversion_solution.json", "crystalnexus:inversion_solution");
        assertContains("gold_plated_copper_sheet_reaction.json", "crystalnexus:gold_plated_copper_sheet");
        assertContains("scrap_pellet_recipe.json", "crystalnexus:netherite_scrap_pellet");
        assertContains("depot_uploader_recipe.json", "crystalnexus:depot_uploader");
        assertTrue(Files.exists(Path.of("src/main/resources/data/crystalnexus/worldgen/configured_feature/invertium_ore.json")));
        assertTrue(Files.exists(Path.of("src/main/resources/data/crystalnexus/neoforge/biome_modifier/invertium_ore_biome_modifier.json")));
        assertTrue(Files.exists(Path.of("src/main/resources/data/crystalnexus/advancement/inverted_advancement.json")));
        for (String tool : List.of("axe", "hoe", "pickaxe", "shovel", "sword"))
            assertTrue(Files.exists(RECIPES.resolve("invertium_" + tool + "_recipe.json")), tool);
    }

    private static void assertContains(String recipe, String... values) throws IOException {
        String json = compact(RECIPES.resolve(recipe));
        for (String value : values) assertTrue(json.contains(value), recipe + " must contain " + value);
    }

    private static String compact(Path path) throws IOException {
        return Files.readString(path).replaceAll("\\s", "");
    }
}
