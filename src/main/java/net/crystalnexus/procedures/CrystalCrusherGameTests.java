package net.crystalnexus.procedures;

import com.mojang.authlib.GameProfile;
import net.crystalnexus.cli.DepotCraftingService;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.util.CrushingRecipeSupport;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class CrystalCrusherGameTests {
	private CrystalCrusherGameTests() {
	}

	@GameTest(template = "zero_point")
	public static void supportsModdedCrushingRecipes(GameTestHelper helper) {
		if (!ModList.get().isLoaded("mekanism")) {
			helper.succeed();
			return;
		}
		ItemStack result = CrushingRecipeSupport.findResult(helper.getLevel(), new ItemStack(Items.BONE));
		helper.assertTrue(result.is(Items.BONE_MEAL), "Expected the installed Mekanism crushing recipe for bone");
		helper.assertTrue(CrushingRecipeSupport.jeiRecipes(helper.getLevel()).stream()
				.anyMatch(recipe -> recipe.getIngredients().getFirst().test(new ItemStack(Items.BONE))
						&& recipe.getResultItem(helper.getLevel().registryAccess()).is(Items.BONE_MEAL)),
				"Expected the Mekanism bone recipe in the Crystal Crusher JEI entries");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void advertisesSmeltingAndBlastingRecipes(GameTestHelper helper) {
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				new GameProfile(UUID.randomUUID(), "recipe-index-test"), ClientInformation.createDefault());
		List<String> categories = DepotCraftingService.recipeChoices(player, Items.COPPER_INGOT).stream()
				.map(choice -> choice.category().toLowerCase()).toList();
		helper.assertTrue(categories.contains("smelting"), "Copper ingot must advertise its smelting recipe: " + categories);
		helper.assertTrue(categories.contains("blasting"), "Copper ingot must advertise its blasting recipe: " + categories);
		DepotSavedData depot = new DepotSavedData();
		depot.deposit(Items.RAW_COPPER.builtInRegistryHolder().key().location(), 1);
		var previewChoices = DepotCraftingService.preview(player, depot, Items.COPPER_INGOT, 1)
			.nodes().getFirst().alternatives();
		List<String> previewCategories = previewChoices.stream().map(choice -> choice.category().toLowerCase()).toList();
		helper.assertTrue(previewCategories.contains("smelting"),
				"Copper ingot preview must retain smelting alongside blasting: " + previewCategories);
		helper.assertTrue(previewChoices.stream().filter(choice -> choice.category().equalsIgnoreCase("smelting"))
				.findFirst().orElseThrow().inputs().stream().flatMap(slot -> slot.alternatives().stream())
				.anyMatch(input -> input.itemId().equals(Items.RAW_COPPER.builtInRegistryHolder().key().location())),
				"The tree should retain the smelting route that best matches stored inputs");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void filtersCraftableTreeItemsAndSupportsNoRecipe(GameTestHelper helper) {
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				new GameProfile(UUID.randomUUID(), "craftable-tree-test"), ClientInformation.createDefault());
		DepotSavedData depot = new DepotSavedData();
		var logs = Items.OAK_LOG.builtInRegistryHolder().key().location();
		var planks = Items.OAK_PLANKS.builtInRegistryHolder().key().location();
		depot.deposit(logs, 1);
		helper.assertTrue(DepotCraftingService.catalog(player, depot, "oak_planks", 0, true).entries().stream()
				.anyMatch(entry -> entry.itemId().equals(planks) && entry.craftable()),
				"Craftable-only results must include items producible from current depot contents");

		depot.setPreferredRecipe(planks, DepotCraftingService.NO_RECIPE_ROUTE);
		var disabled = DepotCraftingService.preview(player, depot, Items.OAK_PLANKS, 1);
		helper.assertTrue(!disabled.success()
					&& DepotCraftingService.NO_RECIPE_ROUTE.equals(disabled.nodes().getFirst().selectedRoute())
					&& DepotCraftingService.catalog(player, depot, "oak_planks", 0, true).entries().isEmpty(),
				"No recipe must make an item an external input and remove it from craftable-only results");

		depot.clearPreferredRecipe(planks);
		helper.assertTrue(DepotCraftingService.catalog(player, new DepotSavedData(), "oak_planks", 0, true)
				.entries().isEmpty(), "Craftable-only results must exclude recipes whose inputs are unavailable");

		var bedrock = Items.BEDROCK.builtInRegistryHolder().key().location();
		depot.setProcessingPattern(bedrock, 1, java.util.Map.of(logs, 1L));
		helper.assertTrue(DepotCraftingService.catalog(player, depot, "Bedrock", 0, false).entries().stream()
				.anyMatch(entry -> entry.itemId().equals(bedrock)),
				"The indexed catalog must search display names and include newly programmed outputs");
		depot.removeProcessingPattern(bedrock);
		helper.assertTrue(DepotCraftingService.catalog(player, depot, "Bedrock", 0, false).entries().isEmpty(),
				"The indexed catalog must invalidate when a programmed output is removed");
		helper.succeed();
	}
}
