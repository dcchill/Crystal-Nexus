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
    public static void fortuneIncreasesOreDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        var tool = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        var fortune = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(tool, mutable -> mutable.set(fortune, 10));
        for (String name : new String[]{"ancient_crystal_ore", "ancient_crystal_ore_stone", "azurine_ore", "deepslate_azurine_ore",
                "blutonium_ore", "chlorophyte_ore", "invertium_ore", "silicon_ore", "deepslate_silicon_ore", "sulfur_ore"}) {
            var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("crystalnexus", name));
            var loot = level.getServer().reloadableRegistries().getLootTable(block.getLootTable());
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, helper.absolutePos(net.minecraft.core.BlockPos.ZERO).getCenter())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, block.defaultBlockState())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, tool)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
            int drops = 0;
            for (int seed = 1; seed <= 16; seed++)
                drops += loot.getRandomItems(params, seed).stream().mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();
            int base = name.equals("ancient_crystal_ore") ? 4 : name.equals("ancient_crystal_ore_stone") || name.equals("sulfur_ore") ? 2 : 1;
            helper.assertTrue(drops > base * 16, name + " must grant bonus drops with Fortune");
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void oreFeaturesAreAttachedToBiomes(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var placedFeatures = registries.registryOrThrow(Registries.PLACED_FEATURE);
        var biomes = registries.registryOrThrow(Registries.BIOME);

        for (String name : new String[]{"ancient_crystal_ore", "ancient_crystal_ore_stone", "blutonium_ore", "chlorophyte_ore",
                "deepslate_silicon_ore", "azurine_ore", "silicon_ore", "sulfur_ore"}) {
            PlacedFeature feature = placedFeatures.get(ResourceLocation.fromNamespaceAndPath("crystalnexus", name));
            helper.assertTrue(feature != null && biomes.getOrThrow(Biomes.PLAINS).getGenerationSettings().hasFeature(feature),
                    name + " must be registered in overworld biome generation");
        }

        PlacedFeature invertium = placedFeatures.get(ResourceLocation.fromNamespaceAndPath("crystalnexus", "invertium_ore"));
        helper.assertTrue(invertium != null
                        && biomes.getOrThrow(Biomes.END_HIGHLANDS).getGenerationSettings().hasFeature(invertium),
                "invertium_ore must be registered in End biome generation");
        PlacedFeature meteor = placedFeatures.get(ResourceLocation.fromNamespaceAndPath("crystalnexus", "resource_meteor"));
        helper.assertTrue(meteor != null && biomes.getOrThrow(Biomes.PLAINS).getGenerationSettings().hasFeature(meteor),
                "resource_meteor must be registered in overworld biome generation");
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
