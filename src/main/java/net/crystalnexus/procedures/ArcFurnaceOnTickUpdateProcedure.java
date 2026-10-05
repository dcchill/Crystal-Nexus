package net.crystalnexus.procedures;

import net.crystalnexus.block.ChemicalReactionChamberBlock;
import net.crystalnexus.block.entity.ArcFurnaceBlockEntity;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.ArcFurnaceRecipe;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.util.ArcFurnaceRecipeSupport;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;

public final class ArcFurnaceOnTickUpdateProcedure {
	private static final int ENERGY_PER_OPERATION = 1024;
	private ArcFurnaceOnTickUpdateProcedure() {}

	public static void execute(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        var machine = level.getBlockEntity(pos);
        net.crystalnexus.util.MachineUpgradeHelper.processParallel(machine, net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(net.crystalnexus.util.MachineUpgradeHelper.upgrades(machine, 3, 4)), () -> executeSingle(level, pos));
    }

    private static boolean executeSingle(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        boolean completed = false;
		if (!(level.getBlockEntity(pos) instanceof ArcFurnaceBlockEntity furnace)) return false;
		if (!furnace.prepareForProcessing(level)) {
			furnace.getPersistentData().putDouble("progress", 0);
			setActive(level, pos, furnace, false);
			sync(level, pos, furnace);
			return false;
		}
		var upgrade = MachineUpgradeHelper.upgrades(furnace, 3, 4);
		MachineTier tier = MachineTier.from(level.getBlockState(pos));
		double baseTime = MachineUpgradeHelper.processingTime(upgrade, 100, 75, 50);
		double cookTime = Math.max(1, Math.ceil(tier.processingTime(MachineUpgradeHelper.cookTime(upgrade, baseTime))
			/ furnace.heatingLayerCount()));
		int energyCost = tier.energyCost(MachineUpgradeHelper.energyCost(upgrade, ENERGY_PER_OPERATION));
		ArcFurnaceRecipe recipe = findRecipe(level, furnace);
		ItemStack output = recipe == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
		furnace.getPersistentData().putDouble("maxProgress", cookTime);

		if (recipe == null || furnace.availableEnergy() < energyCost || !canStack(furnace.getItem(2), output)) {
			furnace.getPersistentData().putDouble("progress", 0);
			setActive(level, pos, furnace, false);
			sync(level, pos, furnace);
			return false;
		}

		setActive(level, pos, furnace, true);
		double progress = furnace.getPersistentData().getDouble("progress") + 1;
		furnace.getPersistentData().putDouble("progress", progress);
		if (progress >= cookTime) {
			if (!furnace.consumeEnergy(energyCost)) return false;
			consumeInputs(furnace, recipe);
			output.setCount(output.getCount() + furnace.getItem(2).getCount());
			furnace.setItem(2, output);
			furnace.getPersistentData().putDouble("progress", 0);
        completed = true;
		}
		sync(level, pos, furnace);

        return completed;
    }

	private static ArcFurnaceRecipe findRecipe(net.minecraft.server.level.ServerLevel level, ArcFurnaceBlockEntity furnace) {
		for (var holder : level.getRecipeManager().getAllRecipesFor(ArcFurnaceRecipe.Type.INSTANCE))
			if (furnace.recipeTier() >= holder.value().minimumArcFurnaceTier()
					&& matches(holder.value(), furnace.getItem(0), furnace.getItem(1))) return holder.value();
		for (ArcFurnaceRecipe recipe : ArcFurnaceRecipeSupport.externalRecipes(level))
			if (matches(recipe, furnace.getItem(0), furnace.getItem(1))) return recipe;
		return null;
	}

	static boolean matches(ArcFurnaceRecipe recipe, ItemStack first, ItemStack second) {
		if (recipe.getIngredients().size() == 1) {
			Ingredient ingredient = recipe.getIngredients().getFirst();
			int count = recipe.ingredientCount(0);
			return ingredient.test(first) && first.getCount() >= count && second.isEmpty()
				|| ingredient.test(second) && second.getCount() >= count && first.isEmpty();
		}
		if (first.isEmpty() || second.isEmpty() || recipe.getIngredients().size() != 2) return false;
		Ingredient a = recipe.getIngredients().get(0), b = recipe.getIngredients().get(1);
		return a.test(first) && first.getCount() >= recipe.ingredientCount(0)
			&& b.test(second) && second.getCount() >= recipe.ingredientCount(1)
			|| a.test(second) && second.getCount() >= recipe.ingredientCount(0)
			&& b.test(first) && first.getCount() >= recipe.ingredientCount(1);
	}

	private static void consumeInputs(ArcFurnaceBlockEntity furnace, ArcFurnaceRecipe recipe) {
		if (recipe.getIngredients().size() == 1) {
			int slot = recipe.getIngredients().getFirst().test(furnace.getItem(0)) ? 0 : 1;
			furnace.removeItem(slot, recipe.ingredientCount(0));
			return;
		}
		Ingredient first = recipe.getIngredients().get(0);
		Ingredient second = recipe.getIngredients().get(1);
		int firstSlot = first.test(furnace.getItem(0)) && furnace.getItem(0).getCount() >= recipe.ingredientCount(0)
			&& second.test(furnace.getItem(1)) && furnace.getItem(1).getCount() >= recipe.ingredientCount(1) ? 0 : 1;
		furnace.removeItem(firstSlot, recipe.ingredientCount(0));
		furnace.removeItem(1 - firstSlot, recipe.ingredientCount(1));
	}

	private static boolean canStack(ItemStack current, ItemStack output) {
		return !output.isEmpty() && (current.isEmpty() || ItemStack.isSameItemSameComponents(current, output))
			&& current.getCount() + output.getCount() <= output.getMaxStackSize();
	}

	private static void setActive(net.minecraft.server.level.ServerLevel level, BlockPos pos, ArcFurnaceBlockEntity furnace, boolean active) {
		furnace.setHeatingCoresActive(active);
		BlockState state = level.getBlockState(pos);
		int value = active ? 2 : 1;
		if (state.hasProperty(ChemicalReactionChamberBlock.BLOCKSTATE)
				&& state.getValue(ChemicalReactionChamberBlock.BLOCKSTATE) != value)
			level.setBlock(pos, state.setValue(ChemicalReactionChamberBlock.BLOCKSTATE, value), 3);
	}

	private static void sync(net.minecraft.server.level.ServerLevel level, BlockPos pos, ArcFurnaceBlockEntity furnace) {
		furnace.setChanged();
		level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
	}
}
