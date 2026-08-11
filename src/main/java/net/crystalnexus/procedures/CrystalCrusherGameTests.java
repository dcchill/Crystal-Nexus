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
		List<String> previewCategories = DepotCraftingService.preview(player, new DepotSavedData(), Items.COPPER_INGOT, 1)
				.nodes().getFirst().alternatives().stream().map(choice -> choice.category().toLowerCase()).toList();
		helper.assertTrue(previewCategories.contains("smelting"),
				"Copper ingot preview must retain smelting alongside blasting: " + previewCategories);
		helper.succeed();
	}
}
