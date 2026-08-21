package net.crystalnexus.gametest;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.SingularityCompressionRecipe;
import net.crystalnexus.item.ResourceSingularityItem;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class SingularityGameTests {
	private SingularityGameTests() {
	}

	@GameTest(template = "zero_point")
	public static void resourceSingularityCompressesAndBreaksDown(GameTestHelper helper) {
		CraftingRecipe breakdown = helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
				.filter(holder -> holder.id().getPath().equals("singularity_breakdown"))
				.findFirst().orElseThrow().value();
		verify(helper, breakdown, CrystalnexusModItems.IRON_SINGULARITY.get(), Items.IRON_INGOT, 10_368);
		verify(helper, breakdown, CrystalnexusModItems.GOLD_SINGULARITY.get(), Items.GOLD_INGOT, 10_368);
		verify(helper, breakdown, CrystalnexusModItems.DIAMOND_SINGULARITY.get(), Items.DIAMOND, 10_368);
		verify(helper, breakdown, CrystalnexusModItems.EMERALD_SINGULARITY.get(), Items.EMERALD, 10_368);
		verify(helper, breakdown, CrystalnexusModItems.COPPER_SINGULARITY.get(), Items.COPPER_INGOT, 10_368);
		verify(helper, breakdown, CrystalnexusModItems.REDSTONE_SINGULARITY.get(), Items.REDSTONE, 10_368);
		verify(helper, breakdown, CrystalnexusModItems.QUARTZ_SINGULARITY.get(), Items.QUARTZ, 10_368);
		verify(helper, breakdown, CrystalnexusModItems.COAL_SINGULARITY.get(), Items.COAL, 10_368);
		verify(helper, breakdown, CrystalnexusModItems.ENERGY_SINGULARITY.get(), CrystalnexusModItems.EE_MATTER.get(), 1_728);
		verify(helper, breakdown, CrystalnexusModItems.WOOD_SINGULARITY.get(), Items.OAK_LOG, ResourceSingularityItem.ITEM_CAPACITY);
		verify(helper, breakdown, CrystalnexusModItems.STONE_SINGULARITY.get(), Items.STONE, ResourceSingularityItem.ITEM_CAPACITY);
		verify(helper, breakdown, CrystalnexusModItems.DIRT_SINGULARITY.get(), Items.DIRT, ResourceSingularityItem.ITEM_CAPACITY);
		helper.assertTrue(!new ItemStack(CrystalnexusModItems.GOLD_SINGULARITY.get()).hasCraftingRemainingItem(),
				"Singularities must only leave a remainder in the dedicated breakdown recipe");
		helper.succeed();
	}

	private static void verify(GameTestHelper helper, CraftingRecipe breakdown, Item singularity, Item resource, int capacity) {
		SingularityCompressionRecipe compression = helper.getLevel().getRecipeManager()
				.getAllRecipesFor(SingularityCompressionRecipe.Type.INSTANCE).stream()
				.map(holder -> holder.value())
				.filter(recipe -> recipe.getResultItem(helper.getLevel().registryAccess()).is(singularity))
				.findFirst().orElseThrow();
		helper.assertTrue(compression.getInputCount(0) == capacity
				&& compression.getIngredients().getFirst().test(new ItemStack(resource)),
				"Singularity compression must use its configured material and item count");
		NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
		grid.set(0, new ItemStack(singularity));
		CraftingInput input = CraftingInput.of(3, 3, grid);
		helper.assertTrue(breakdown.matches(input, helper.getLevel()),
				"A singularity by itself must match the breakdown recipe");
		ItemStack output = breakdown.assemble(input, helper.getLevel().registryAccess());
		ItemStack remainder = breakdown.getRemainingItems(input).getFirst();
		helper.assertTrue(output.is(resource) && output.getCount() == 64,
				"Breaking down a resource singularity must return one stack of its material");
		helper.assertTrue(remainder.is(singularity) && remainder.getDamageValue() == 64,
				"The returned singularity must lose durability equal to the extracted item count");
	}
}
