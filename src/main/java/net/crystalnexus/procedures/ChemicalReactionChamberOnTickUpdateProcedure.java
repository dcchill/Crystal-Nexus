package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
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
import net.crystalnexus.assembly.AssemblyLineMachine;
import net.minecraft.resources.ResourceLocation;

import net.crystalnexus.jei_recipes.ChemicalReactionRecipe;
import net.crystalnexus.util.MachineUpgradeHelper;


public class ChemicalReactionChamberOnTickUpdateProcedure {

	public static void execute(LevelAccessor world, double x, double y, double z) {
        var machine = world.getBlockEntity(net.minecraft.core.BlockPos.containing(x, y, z));
        int crafts = machine instanceof net.minecraft.world.Container inventory && inventory.getContainerSize() > 4 ? net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(inventory.getItem(4)) : 1;
        net.crystalnexus.util.MachineUpgradeHelper.processParallel(machine, crafts, () -> executeSingle(world, x, y, z));
    }

    private static boolean executeSingle(LevelAccessor world, double x, double y, double z) {
        boolean completed = false;
        if (world.isClientSide()) return false;
		double outputAmount = 0;
		double cookTime = 0;

		BlockPos pos = BlockPos.containing(x, y, z);

		if (world instanceof Level level && !AssemblyLineMachine.mayTick(level, pos)) return false;

		if (net.crystalnexus.util.MachineAnimationHelper.shouldIdle(world, pos, getBlockNBTNumber(world, pos, "progress"))) {
			setIntegerBlockState(world, pos, "blockstate", 1);
		} else {
			setIntegerBlockState(world, pos, "blockstate", 2);
		}

		ItemStack upgrade = itemFromBlockInventory(world, pos, 4).copy();

		outputAmount = 1;

		cookTime = MachineUpgradeHelper.processingTime(upgrade, 100, 75, 50);

		cookTime = MachineUpgradeHelper.cookTime(upgrade, cookTime);
		int energyCost = MachineUpgradeHelper.energyCost(upgrade, 4096);

		if (!world.isClientSide()) {
			BlockEntity be = world.getBlockEntity(pos);
			BlockState bs = world.getBlockState(pos);
			if (be != null) be.getPersistentData().putDouble("maxProgress", cookTime);
			if (world instanceof Level lvl) lvl.sendBlockUpdated(pos, bs, bs, 3);
		}

		MatchingRecipe match = getMatchingRecipe(world, pos);
		ItemStack resultStack = (match != null) ? match.result : ItemStack.EMPTY;

		if (resultStack.isEmpty() || resultStack.getItem() == Blocks.AIR.asItem()) {
			return false;
		}

		if (getEnergyStored(world, pos, null) < energyCost) {
			return false;
		}

		ItemStack outSlot = itemFromBlockInventory(world, pos, 3).copy();
		boolean outSlotEmpty = outSlot.isEmpty() || outSlot.getItem() == Blocks.AIR.asItem();
		boolean outSlotMatches = outSlotEmpty || outSlot.getItem() == resultStack.getItem();

		if (!outSlotMatches) {
			return false;
		}

		int addCount = Math.clamp(resultStack.getCount(), 1, 8);

		int currentOutCount = itemFromBlockInventory(world, pos, 3).getCount();
		int spaceLeft = 64 - currentOutCount;
		if (spaceLeft <= 0) {
			return false;
		}
		if (addCount > spaceLeft) return false;

		outputAmount = addCount;

final int _finalAddCount = addCount;
final ItemStack _finalResult = resultStack.copy();

completed = processRecipeTick(world, pos, cookTime, () -> {
	addToOutputSlot(world, pos, _finalResult, _finalAddCount);
	consumeInputs(world, pos);
	extractEnergy(world, pos, energyCost);
});

		return completed;
    }


	private static class MatchingRecipe {
		final ChemicalReactionRecipe recipe;
		final ItemStack result;

		MatchingRecipe(ChemicalReactionRecipe recipe, ItemStack result) {
			this.recipe = recipe;
			this.result = result;
		}
	}

	private static MatchingRecipe getMatchingRecipe(LevelAccessor world, BlockPos pos) {
		if (!(world instanceof Level lvl)) return null;

		ItemStack s0 = itemFromBlockInventory(world, pos, 0).copy();
		ItemStack s1 = itemFromBlockInventory(world, pos, 1).copy();
		ItemStack s2 = itemFromBlockInventory(world, pos, 2).copy();

		if (s0.isEmpty() || s1.isEmpty() || s2.isEmpty()) return null;

		ResourceLocation assigned = AssemblyLineMachine.assignedRecipe(lvl, pos);
		for (RecipeHolder<ChemicalReactionRecipe> holder : lvl.getRecipeManager()
				.getAllRecipesFor(ChemicalReactionRecipe.Type.INSTANCE)) {
			if (assigned != null && !assigned.equals(holder.id())) continue;
			ChemicalReactionRecipe recipe = holder.value();
			NonNullList<Ingredient> ing = recipe.getIngredients();
			if (matches3Shapeless(ing, s0, s1, s2)) {
				return new MatchingRecipe(recipe, recipe.getResultItem(null));
			}
		}
		return null;
	}

	private static boolean matches3Shapeless(NonNullList<Ingredient> ing, ItemStack a, ItemStack b, ItemStack c) {
		if (ing.size() != 3) return false;

		return (ing.get(0).test(a) && ing.get(1).test(b) && ing.get(2).test(c)) ||
			   (ing.get(0).test(a) && ing.get(1).test(c) && ing.get(2).test(b)) ||
			   (ing.get(0).test(b) && ing.get(1).test(a) && ing.get(2).test(c)) ||
			   (ing.get(0).test(b) && ing.get(1).test(c) && ing.get(2).test(a)) ||
			   (ing.get(0).test(c) && ing.get(1).test(a) && ing.get(2).test(b)) ||
			   (ing.get(0).test(c) && ing.get(1).test(b) && ing.get(2).test(a));
	}


	private static boolean processRecipeTick(LevelAccessor world, BlockPos pos, double cookTime, Runnable onFinish) {
		if (getBlockNBTNumber(world, pos, "progress") < cookTime) {
			if (!world.isClientSide()) {
				BlockEntity be = world.getBlockEntity(pos);
				BlockState bs = world.getBlockState(pos);
				if (be != null) be.getPersistentData().putDouble("progress", getBlockNBTNumber(world, pos, "progress") + 1);
				if (world instanceof Level lvl) lvl.sendBlockUpdated(pos, bs, bs, 3);
			}
			if (world instanceof ServerLevel lvl) {
				lvl.sendParticles(ParticleTypes.DRAGON_BREATH,
					pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					1, 0.25, 0, 0.25, 0);
			}
		}

		if (getBlockNBTNumber(world, pos, "progress") >= cookTime) {
			onFinish.run();

			if (!world.isClientSide()) {
				BlockEntity be = world.getBlockEntity(pos);
				BlockState bs = world.getBlockState(pos);
				if (be != null) be.getPersistentData().putDouble("progress", 0);
				if (world instanceof Level lvl) lvl.sendBlockUpdated(pos, bs, bs, 3);
			}
            return true;
		}
        return false;
	}

	private static void addToOutputSlot(LevelAccessor world, BlockPos pos, ItemStack result, int addCount) {
		if (addCount <= 0) return;

		if (world instanceof ILevelExtension ext &&
			ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable handler) {

			ItemStack current = handler.getStackInSlot(3).copy();
			ItemStack toSet = result.copy();

			int newCount = current.isEmpty() ? addCount : current.getCount() + addCount;
			if (newCount > 64) newCount = 64;

			toSet.setCount(newCount);
			handler.setStackInSlot(3, toSet);
		}
	}

	private static void consumeInputs(LevelAccessor world, BlockPos pos) {
		shrinkSlot(world, pos, 0, 1);
		shrinkSlot(world, pos, 1, 1);
		shrinkSlot(world, pos, 2, 1);
	}

	private static void shrinkSlot(LevelAccessor world, BlockPos pos, int slot, int amount) {
		if (world instanceof ILevelExtension ext &&
			ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable handler) {

			ItemStack stk = handler.getStackInSlot(slot).copy();
			stk.shrink(amount);
			handler.setStackInSlot(slot, stk);
		}
	}

	private static void extractEnergy(LevelAccessor world, BlockPos pos, int amount) {
		if (world instanceof ILevelExtension ext) {
			IEnergyStorage storage = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
			if (storage != null) storage.extractEnergy(amount, false);
		}
	}


	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null) return blockEntity.getPersistentData().getDouble(tag);
		return -1;
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null) return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}

	public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null) return energyStorage.getEnergyStored();
		}
		return 0;
	}

	private static void setIntegerBlockState(LevelAccessor world, BlockPos pos, String propName, int value) {
		BlockState bs = world.getBlockState(pos);
		if (bs.getBlock().getStateDefinition().getProperty(propName) instanceof IntegerProperty ip &&
			ip.getPossibleValues().contains(value)) {
			world.setBlock(pos, bs.setValue(ip, value), 3);
		}
	}
}
