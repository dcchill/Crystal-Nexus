package net.crystalnexus.procedures;

import net.crystalnexus.block.ChemicalReactionChamberBlock;
import net.crystalnexus.block.entity.ArcFurnaceBlockEntity;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.ArcFurnaceRecipe;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;

public final class ArcFurnaceOnTickUpdateProcedure {
	private static final int ENERGY_PER_OPERATION = 4096;
	private ArcFurnaceOnTickUpdateProcedure() {}

	public static void execute(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof ArcFurnaceBlockEntity furnace)) return;
		if (!furnace.prepareForProcessing(level)) {
			furnace.getPersistentData().putDouble("progress", 0);
			setActive(level, pos, furnace, false);
			sync(level, pos, furnace);
			return;
		}
		ItemStack upgrade = furnace.getItem(3);
		MachineTier tier = MachineTier.from(level.getBlockState(pos));
		double baseTime = upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get()) ? 75
			: upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) ? 50 : 100;
		double cookTime = tier.processingTime(MachineUpgradeHelper.cookTime(upgrade, baseTime));
		int energyCost = tier.energyCost(MachineUpgradeHelper.energyCost(upgrade, ENERGY_PER_OPERATION));
		ArcFurnaceRecipe recipe = findRecipe(level, furnace);
		ItemStack output = recipe == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
		furnace.getPersistentData().putDouble("maxProgress", cookTime);

		if (recipe == null || furnace.availableEnergy() < energyCost || !canStack(furnace.getItem(2), output)) {
			furnace.getPersistentData().putDouble("progress", 0);
			setActive(level, pos, furnace, false);
			sync(level, pos, furnace);
			return;
		}

		setActive(level, pos, furnace, true);
		double progress = furnace.getPersistentData().getDouble("progress") + 1;
		furnace.getPersistentData().putDouble("progress", progress);
		if (progress >= cookTime) {
			consumeInputs(furnace, recipe);
			output.setCount(output.getCount() + furnace.getItem(2).getCount());
			furnace.setItem(2, output);
			furnace.extractEnergy(energyCost, false);
			furnace.getPersistentData().putDouble("progress", 0);
		}
		sync(level, pos, furnace);
	}

	private static ArcFurnaceRecipe findRecipe(net.minecraft.server.level.ServerLevel level, ArcFurnaceBlockEntity furnace) {
		for (var holder : level.getRecipeManager().getAllRecipesFor(ArcFurnaceRecipe.Type.INSTANCE))
			if (matches(holder.value(), furnace.getItem(0), furnace.getItem(1))) return holder.value();
		return null;
	}

	static boolean matches(ArcFurnaceRecipe recipe, ItemStack first, ItemStack second) {
		if (recipe.getIngredients().size() == 1) {
			Ingredient ingredient = recipe.getIngredients().getFirst();
			return ingredient.test(first) && second.isEmpty() || ingredient.test(second) && first.isEmpty();
		}
		if (first.isEmpty() || second.isEmpty() || recipe.getIngredients().size() != 2) return false;
		Ingredient a = recipe.getIngredients().get(0), b = recipe.getIngredients().get(1);
		return a.test(first) && b.test(second) || a.test(second) && b.test(first);
	}

	private static void consumeInputs(ArcFurnaceBlockEntity furnace, ArcFurnaceRecipe recipe) {
		if (recipe.getIngredients().size() == 1) {
			int slot = recipe.getIngredients().getFirst().test(furnace.getItem(0)) ? 0 : 1;
			furnace.removeItem(slot, 1);
			return;
		}
		furnace.removeItem(0, 1);
		furnace.removeItem(1, 1);
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
