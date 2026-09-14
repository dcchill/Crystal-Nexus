package net.crystalnexus.gametest;

import net.crystalnexus.jei_recipes.AcceleratorJeiRecipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class ParticleAcceleratorGameTests {
    private ParticleAcceleratorGameTests() {}

    @GameTest(template = "zero_point")
    public static void allAcceleratorRecipesLoad(GameTestHelper helper) {
        Set<ResourceLocation> recipeIds = helper.getLevel().getRecipeManager()
            .getAllRecipesFor(AcceleratorJeiRecipe.Type.INSTANCE).stream()
            .map(holder -> holder.id()).collect(Collectors.toSet());
        Set<ResourceLocation> expected = java.util.Arrays.stream(new String[] {
            "accelerator_coal_to_diamond", "accelerator_dark_matter", "accelerator_lapis_dust",
            "accelerator_nether_star", "accelerator_obsidrax_dust", "accelerator_oil",
            "accelerator_rubber_alt", "accelerator_wind_charge"
        }).map(path -> ResourceLocation.fromNamespaceAndPath("crystalnexus", path)).collect(Collectors.toSet());

        helper.assertTrue(recipeIds.containsAll(expected), "All Particle Accelerator recipes must load");
        helper.succeed();
    }
}
