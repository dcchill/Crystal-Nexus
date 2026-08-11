package net.crystalnexus.gametest;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class WorldgenGameTests {
    private WorldgenGameTests() {
    }

    @GameTest(template = "zero_point")
    public static void oreFeaturesAreAttachedToBiomes(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var placedFeatures = registries.registryOrThrow(Registries.PLACED_FEATURE);
        var biomes = registries.registryOrThrow(Registries.BIOME);

        for (String name : new String[]{"ancient_crystal_ore", "ancient_crystal_ore_stone", "blutonium_ore", "chlorophyte_ore",
                "deepslate_silicon_ore", "silicon_ore", "sulfur_ore"}) {
            PlacedFeature feature = placedFeatures.get(ResourceLocation.fromNamespaceAndPath("crystalnexus", name));
            helper.assertTrue(feature != null && biomes.getOrThrow(Biomes.PLAINS).getGenerationSettings().hasFeature(feature),
                    name + " must be registered in overworld biome generation");
        }

        PlacedFeature invertium = placedFeatures.get(ResourceLocation.fromNamespaceAndPath("crystalnexus", "invertium_ore"));
        helper.assertTrue(invertium != null
                        && biomes.getOrThrow(Biomes.END_HIGHLANDS).getGenerationSettings().hasFeature(invertium),
                "invertium_ore must be registered in End biome generation");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void oilNodeStructuresLoad(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ResourceLocation oilNodes = ResourceLocation.fromNamespaceAndPath("crystalnexus", "oil_nodes");
        helper.assertTrue(registries.registryOrThrow(Registries.STRUCTURE).containsKey(oilNodes),
                "Oil node surface structure must be registered");
        helper.assertTrue(registries.registryOrThrow(Registries.STRUCTURE_SET).containsKey(oilNodes),
                "Oil node surface structure set must be registered");
        helper.assertTrue(registries.registryOrThrow(Registries.TEMPLATE_POOL).containsKey(oilNodes),
                "Oil node template pool must be registered");
        for (String name : new String[]{"oil_node_a", "oil_node_b"}) {
            helper.assertTrue(helper.getLevel().getStructureManager().get(
                            ResourceLocation.fromNamespaceAndPath("crystalnexus", name)).isPresent(),
                    name + " structure template must load");
        }
        helper.succeed();
    }
}
