package net.crystalnexus.recipe;

import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

public class SingularityBreakdownRecipe extends CustomRecipe {
	private static final int STACK_SIZE = 64;
	public static final SimpleCraftingRecipeSerializer<SingularityBreakdownRecipe> SERIALIZER =
			new SimpleCraftingRecipeSerializer<>(SingularityBreakdownRecipe::new);

	public SingularityBreakdownRecipe(CraftingBookCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		ItemStack singularity = ItemStack.EMPTY;
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack stack = input.getItem(slot);
			if (stack.isEmpty())
				continue;
			if (!singularity.isEmpty() || stack.getCount() != 1)
				return false;
			singularity = stack;
		}
		return !singularity.isEmpty() && outputFor(singularity) != Items.AIR && remainingItems(singularity) > 0;
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack singularity = singularity(input);
		Item output = outputFor(singularity);
		return output == Items.AIR ? ItemStack.EMPTY : new ItemStack(output, Math.min(STACK_SIZE, remainingItems(singularity)));
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		NonNullList<ItemStack> remainders = NonNullList.withSize(input.size(), ItemStack.EMPTY);
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack stack = input.getItem(slot);
			if (outputFor(stack) == Items.AIR)
				continue;
			int damage = stack.getDamageValue() + Math.min(STACK_SIZE, remainingItems(stack));
			if (damage < stack.getMaxDamage()) {
				ItemStack remainder = stack.copyWithCount(1);
				remainder.setDamageValue(damage);
				remainders.set(slot, remainder);
			}
		}
		return remainders;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 1;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	private static ItemStack singularity(CraftingInput input) {
		for (int slot = 0; slot < input.size(); slot++)
			if (!input.getItem(slot).isEmpty())
				return input.getItem(slot);
		return ItemStack.EMPTY;
	}

	private static int remainingItems(ItemStack stack) {
		return stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : 0;
	}

	private static Item outputFor(ItemStack stack) {
		if (stack.is(CrystalnexusModItems.IRON_SINGULARITY.get())) return Items.IRON_INGOT;
		if (stack.is(CrystalnexusModItems.GOLD_SINGULARITY.get())) return Items.GOLD_INGOT;
		if (stack.is(CrystalnexusModItems.DIAMOND_SINGULARITY.get())) return Items.DIAMOND;
		if (stack.is(CrystalnexusModItems.EMERALD_SINGULARITY.get())) return Items.EMERALD;
		if (stack.is(CrystalnexusModItems.COPPER_SINGULARITY.get())) return Items.COPPER_INGOT;
		if (stack.is(CrystalnexusModItems.REDSTONE_SINGULARITY.get())) return Items.REDSTONE;
		if (stack.is(CrystalnexusModItems.QUARTZ_SINGULARITY.get())) return Items.QUARTZ;
		if (stack.is(CrystalnexusModItems.COAL_SINGULARITY.get())) return Items.COAL;
		if (stack.is(CrystalnexusModItems.ENERGY_SINGULARITY.get())) return CrystalnexusModItems.EE_MATTER.get();
		if (stack.is(CrystalnexusModItems.WOOD_SINGULARITY.get())) return Items.OAK_LOG;
		if (stack.is(CrystalnexusModItems.STONE_SINGULARITY.get())) return Items.STONE;
		if (stack.is(CrystalnexusModItems.DIRT_SINGULARITY.get())) return Items.DIRT;
		return Items.AIR;
	}
}
