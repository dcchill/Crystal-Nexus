package net.crystalnexus.util;

import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class ArcFurnaceRecipeSupportGameTests {
	private ArcFurnaceRecipeSupportGameTests() {}

	@GameTest(template = "zero_point")
	public static void rejectsNamedCraftingAlloys(GameTestHelper helper) {
		ShapelessRecipe source = new ShapelessRecipe("", CraftingBookCategory.MISC,
			new ItemStack(Items.GOLD_INGOT, 4), NonNullList.of(Ingredient.EMPTY,
				Ingredient.of(Items.COPPER_INGOT), Ingredient.of(Items.IRON_INGOT)));
		helper.assertTrue(ArcFurnaceRecipeSupport.convert(new RecipeHolder<>(
			ResourceLocation.parse("foreign:alloy"), source), helper.getLevel().registryAccess(), helper.getLevel()) == null,
			"A crafting recipe named alloy must stay out of the furnace");
		helper.getLevel().getRecipeManager()
			.byKey(ResourceLocation.parse("justdirethings:template_eclipsealloy-duplicate"))
			.ifPresent(holder -> helper.assertTrue(ArcFurnaceRecipeSupport.convert(holder,
				helper.getLevel().registryAccess(), helper.getLevel()) == null,
				"The Eclipse Alloy template crafting recipe must stay out of the furnace"));
		helper.succeed();
	}
}
