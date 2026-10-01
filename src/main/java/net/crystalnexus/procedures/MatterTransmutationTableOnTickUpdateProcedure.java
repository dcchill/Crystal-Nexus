package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.jei_recipes.MatterTransmutationRecipe;
import net.crystalnexus.config.CrystalnexusConfig;

import java.util.stream.Collectors;
import java.util.List;

public class MatterTransmutationTableOnTickUpdateProcedure {

	public static String execute(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);

		final int cookTime = CrystalnexusConfig.MACHINES.MATTER_TRANSMUTATION_PROCESS.ticksPerCraft();
		final int energyCost = CrystalnexusConfig.MACHINES.MATTER_TRANSMUTATION_PROCESS.energyPerCraft();
		final int inputSlots = 8;
		final int outputSlot = 8;

		setBlockNBT(world, pos, "maxProgress", cookTime);

		
		MatterTransmutationRecipe recipe = findMatchingRecipe(world, pos, inputSlots);
		if (recipe == null) {
			setBlockNBT(world, pos, "progress", 0);
			return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, pos, null));
		}

		if (getEnergyStored(world, pos, null) < energyCost) {
			return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, pos, null));
		}

		ItemStack result = recipe.getResultItem(null);
		if (result.isEmpty() || result.getItem() == Blocks.AIR.asItem()) {
			setBlockNBT(world, pos, "progress", 0);
			return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, pos, null));
		}

		if (!canOutputAccept(world, pos, outputSlot, result)) {
			return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, pos, null));
		}

		double progress = getBlockNBTNumber(world, pos, "progress");
		if (progress < 0) progress = 0;

		if (progress < cookTime) {
			setBlockNBT(world, pos, "progress", progress + 1);

			if (world instanceof ServerLevel level)
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x + 0.5, y + 0.5, z + 0.5, 1, 0.25, 0, 0.25, 0);

			return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, pos, null));
		}

		addToOutput(world, pos, outputSlot, result);
		consumeInputs(world, pos, recipe, inputSlots);
		extractEnergy(world, pos, energyCost);
		setBlockNBT(world, pos, "progress", 0);

		return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, pos, null));
	}


	private static MatterTransmutationRecipe findMatchingRecipe(LevelAccessor world, BlockPos pos, int inputSlots) {
		if (!(world instanceof Level lvl)) return null;

		List<MatterTransmutationRecipe> recipes =
			lvl.getRecipeManager()
				.getAllRecipesFor(MatterTransmutationRecipe.Type.INSTANCE)
				.stream()
				.map(RecipeHolder::value)
				.collect(Collectors.toList());

		for (MatterTransmutationRecipe recipe : recipes) {
			if (recipe.isEnabledByConfig() && recipeMatchesWithCounts(world, pos, recipe, inputSlots)) {
				return recipe;
			}
		}
		// AE2 processing patterns pool identical ingredients instead of retaining ring positions.
		for (MatterTransmutationRecipe recipe : recipes)
			if (recipe.isEnabledByConfig() && arrangePooledInputs(lvl, pos, recipe, inputSlots)) return recipe;
		return null;
	}

	private static boolean arrangePooledInputs(Level level, BlockPos pos, MatterTransmutationRecipe recipe, int slots) {
		if (recipe.getIngredients().size() != slots
				|| !(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable inventory)) return false;
		var remaining = new java.util.ArrayList<ItemStack>();
		var arranged = new java.util.ArrayList<ItemStack>();
		for (int i = 0; i < slots; i++) remaining.add(inventory.getStackInSlot(i).copy());
		int total = remaining.stream().mapToInt(ItemStack::getCount).sum();
		int batchSize = java.util.stream.IntStream.range(0, slots).map(recipe::getInputCount).sum();
		if (total == 0 || total % batchSize != 0) return false;
		int batches = total / batchSize;
		for (int target = 0; target < slots; target++) {
			Ingredient ingredient = recipe.getIngredients().get(target);
			ItemStack chosen = remaining.stream().filter(stack -> !stack.isEmpty()
				&& ingredient.test(stack)).findFirst().orElse(ItemStack.EMPTY);
			int needed = recipe.getInputCount(target) * batches;
			if (chosen.isEmpty() || needed > chosen.getMaxStackSize()) return false;
			ItemStack result = chosen.copyWithCount(needed);
			for (ItemStack source : remaining) {
				if (!ItemStack.isSameItemSameComponents(source, result)) continue;
				int taken = Math.min(needed, source.getCount());
				source.shrink(taken);
				needed -= taken;
			}
			if (needed != 0) return false;
			arranged.add(result);
		}
		// Commit only after every input has been accounted for; failed matches leave inventory intact.
		if (remaining.stream().anyMatch(stack -> !stack.isEmpty())) return false;
		for (int i = 0; i < slots; i++) inventory.setStackInSlot(i, arranged.get(i));
		return true;
	}

	private static boolean recipeMatchesWithCounts(LevelAccessor world, BlockPos pos, MatterTransmutationRecipe recipe, int inputSlots) {
		NonNullList<Ingredient> ings = recipe.getIngredients();
		if (ings == null || ings.size() < inputSlots) return false;

		for (int i = 0; i < inputSlots; i++) {
			Ingredient ing = ings.get(i);
			ItemStack inSlot = itemFromBlockInventory(world, pos, i);

			if (!ing.test(inSlot.copy())) return false;

			int required = recipe.getInputCount(i);
			if (inSlot.getCount() < required) return false;
		}
		return true;
	}


	private static boolean canOutputAccept(LevelAccessor world, BlockPos pos, int outSlot, ItemStack toAdd) {
		ItemStack out = itemFromBlockInventory(world, pos, outSlot);

		if (out.isEmpty() || out.getItem() == Blocks.AIR.asItem()) {
			return toAdd.getCount() <= toAdd.getMaxStackSize();
		}

		if (!ItemStack.isSameItemSameComponents(out, toAdd)) return false;

		return out.getCount() + toAdd.getCount() <= out.getMaxStackSize();
	}

	private static void addToOutput(LevelAccessor world, BlockPos pos, int outSlot, ItemStack toAdd) {
		if (!(world instanceof ILevelExtension ext)) return;
		if (!(ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable inv)) return;

		ItemStack out = inv.getStackInSlot(outSlot).copy();
		if (out.isEmpty() || out.getItem() == Blocks.AIR.asItem()) {
			ItemStack set = toAdd.copy();
			inv.setStackInSlot(outSlot, set);
			return;
		}

		out.grow(toAdd.getCount());
		inv.setStackInSlot(outSlot, out);
	}


	private static void consumeInputs(LevelAccessor world, BlockPos pos, MatterTransmutationRecipe recipe, int inputSlots) {
		if (!(world instanceof ILevelExtension ext)) return;
		if (!(ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable inv)) return;

		for (int i = 0; i < inputSlots; i++) {
			int required = recipe.getInputCount(i);
			if (required <= 0) required = 1;

			ItemStack stk = inv.getStackInSlot(i).copy();
			stk.shrink(required);
			inv.setStackInSlot(i, stk);
		}
	}


	private static void extractEnergy(LevelAccessor world, BlockPos pos, int amount) {
		if (world instanceof ILevelExtension ext) {
			IEnergyStorage storage = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
			if (storage != null) storage.extractEnergy(amount, false);
		}
	}

	private static void setBlockNBT(LevelAccessor world, BlockPos pos, String tag, double value) {
		if (world.isClientSide()) return;

		BlockEntity be = world.getBlockEntity(pos);
		if (be == null) return;

		BlockState bs = world.getBlockState(pos);
		be.getPersistentData().putDouble(tag, value);
		if (world instanceof Level level)
			level.sendBlockUpdated(pos, bs, bs, 3);
	}


	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}

	public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.getEnergyStored();
		}
		return 0;
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
	}
}
