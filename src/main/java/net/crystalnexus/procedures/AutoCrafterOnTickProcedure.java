package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.processing.MachineTier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoCrafterOnTickProcedure {

    private static boolean stacksMatch(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem();
    }

    public static void execute(LevelAccessor world, double x, double y, double z) {
        boolean crafting = false; 

        if (!(world instanceof ILevelExtension ext)) return;
        BlockPos pos = BlockPos.containing(x, y, z);
		boolean crystalFactory = world.getBlockState(pos).is(CrystalnexusModBlocks.CRYSTAL_CRAFTING_FACTORY.get());
		boolean azurineFactory = world.getBlockState(pos).is(CrystalnexusModBlocks.TITANIUM_CRAFTING_FACTORY.get());
		boolean hyperFactory = world.getBlockState(pos).is(CrystalnexusModBlocks.HYPER_CRAFTING_FACTORY.get());
		int craftTime = hyperFactory ? 5 : azurineFactory ? 10 : crystalFactory ? 25 : 50;
        var upgrades = net.crystalnexus.util.MachineUpgradeHelper.upgrades(world.getBlockEntity(pos), 11, 12);
        craftTime = (int) Math.ceil(net.crystalnexus.util.MachineUpgradeHelper.cookTime(upgrades,
            net.crystalnexus.util.MachineUpgradeHelper.processingTime(upgrades, craftTime, craftTime * 0.75, craftTime * 0.5)));
        int energyPerCraft = net.crystalnexus.util.MachineUpgradeHelper.energyCost(world.getBlockState(pos), upgrades, 512);
		setMaxProgress(world, pos, craftTime);

        var cap = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (!(cap instanceof IItemHandlerModifiable inv)) return;

        ItemStack[] grid = new ItemStack[9];
        boolean empty = true;
        for (int i = 0; i < 9; i++) {
            grid[i] = inv.getStackInSlot(i).copy();
            if (!grid[i].isEmpty()) empty = false;
        }
        if (empty) {
			setProgress(world, pos, 0);
            updateBlockState(world, pos, crafting);
            return;
        }

        if (!(world instanceof ServerLevel serverLevel)) return;

        ItemStack filter = inv.getStackInSlot(10);
        if (filter.isEmpty()) {
			setProgress(world, pos, 0);
            updateBlockState(world, pos, crafting);
            return;
        }

        RecipeManager manager = serverLevel.getRecipeManager();
        List<RecipeHolder<CraftingRecipe>> recipes = manager.getAllRecipesFor(RecipeType.CRAFTING);

        ItemStack result = ItemStack.EMPTY;
        int[] consumption = null;
        for (RecipeHolder<CraftingRecipe> holder : recipes) {
            CraftingRecipe r = holder.value();
            ItemStack candidateResult = r.getResultItem(serverLevel.registryAccess());
            if (!candidateResult.isEmpty() && stacksMatch(candidateResult, filter)
                    && (consumption = consumptionPlan(r, grid)) != null) {
                result = candidateResult;
                break;
            }
        }
        if (consumption == null) {
			setProgress(world, pos, 0);
            updateBlockState(world, pos, crafting);
            return;
        }

        IEnergyStorage energy = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
		int payableEnergy = Math.min(energyPerCraft, net.crystalnexus.config.CrystalnexusConfig.MACHINES.CRAFTING_FACTORY.maxExtract());
		boolean canCraftEnergy = energy == null || energy.getEnergyStored() >= payableEnergy;
        if (!canCraftEnergy) {
            updateBlockState(world, pos, crafting);
            return;
        }

        
        ItemStack output = inv.getStackInSlot(9);
		int maxStack = net.crystalnexus.util.MachineItemStorage.slotLimit((net.minecraft.world.Container) world.getBlockEntity(pos), 9, result);

        if (result.getCount() > maxStack) return;
        if (!output.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(output, result)) {
                updateBlockState(world, pos, crafting);
                return; 
            }
            if (output.getCount() + result.getCount() > maxStack) {
                updateBlockState(world, pos, crafting);
                return; 
            }
        }

		int progress = getProgress(world, pos);
		if (progress + 1 < craftTime) {
			setProgress(world, pos, progress + 1);
			updateBlockState(world, pos, true);
			return;
		}

		int craftCount = 1;
        int parallelCrafts = net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(upgrades);
		if (hyperFactory || parallelCrafts > 1) {
			int outputRoom = output.isEmpty() ? maxStack : maxStack - output.getCount();
			craftCount = outputRoom / result.getCount();
			for (int i = 0; i < consumption.length; i++) {
				if (consumption[i] > 0) craftCount = Math.min(craftCount, inv.getStackInSlot(i).getCount() / consumption[i]);
			}
			if (energy != null) craftCount = Math.min(craftCount, energy.getEnergyStored() / payableEnergy);
			craftCount = Math.min(craftCount, hyperFactory ? 64 : parallelCrafts);
		}
		if (craftCount <= 0) {
			updateBlockState(world, pos, crafting);
			return;
		}

		for (int i = 0; i < consumption.length; i++) {
			inv.getStackInSlot(i).shrink(consumption[i] * craftCount);
		}

        if (output.isEmpty()) {
            ItemStack toInsert = result.copy();
			toInsert.setCount(Math.min(toInsert.getCount() * craftCount, maxStack));
			inv.setStackInSlot(9, toInsert);
		} else {
			output.grow(result.getCount() * craftCount);
        }

		if (energy != null) energy.extractEnergy(energyPerCraft * craftCount, false);
		setProgress(world, pos, 0);

        crafting = true;

        updateBlockState(world, pos, crafting);
    }

    private static int[] consumptionPlan(CraftingRecipe recipe, ItemStack[] grid) {
        List<Ingredient> ingredients = recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList();
        List<Integer> units = new ArrayList<>();
        for (int slot = 0; slot < grid.length; slot++) {
            for (int count = Math.min(grid[slot].getCount(), ingredients.size()); count > 0; count--) units.add(slot);
        }
        if (units.size() < ingredients.size()) return null;

        int[] assignments = new int[units.size()];
        Arrays.fill(assignments, -1);
        for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
            if (!assignIngredient(ingredient, ingredients, grid, units, assignments, new boolean[units.size()])) return null;
        }

        int[] consumption = new int[grid.length];
        for (int unit = 0; unit < units.size(); unit++) {
            if (assignments[unit] >= 0) consumption[units.get(unit)]++;
        }
        return consumption;
    }

    private static boolean assignIngredient(int ingredient, List<Ingredient> ingredients, ItemStack[] grid,
            List<Integer> units, int[] assignments, boolean[] visited) {
        for (int unit = 0; unit < units.size(); unit++) {
            if (visited[unit] || !ingredients.get(ingredient).test(grid[units.get(unit)])) continue;
            visited[unit] = true;
            int displaced = assignments[unit];
            if (displaced < 0 || assignIngredient(displaced, ingredients, grid, units, assignments, visited)) {
                assignments[unit] = ingredient;
                return true;
            }
        }
        return false;
    }

    private static void updateBlockState(LevelAccessor world, BlockPos pos, boolean crafting) {
        int value = crafting ? 2 : 1;
        BlockState bs = world.getBlockState(pos);
        if (bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty prop
                && prop.getPossibleValues().contains(value)) {
            world.setBlock(pos, bs.setValue(prop, value), 3);
        }
    }

	private static int getProgress(LevelAccessor world, BlockPos pos) {
		BlockEntity entity = world.getBlockEntity(pos);
		return entity == null ? 0 : entity.getPersistentData().getInt("progress");
	}

	private static void setProgress(LevelAccessor world, BlockPos pos, int progress) {
		BlockEntity entity = world.getBlockEntity(pos);
		if (entity != null) {
			entity.getPersistentData().putInt("progress", progress);
			entity.setChanged();
			if (world instanceof Level level)
				level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
		}
	}

	private static void setMaxProgress(LevelAccessor world, BlockPos pos, int progress) {
		BlockEntity entity = world.getBlockEntity(pos);
		if (entity != null) {
			entity.getPersistentData().putInt("maxProgress", progress);
			entity.setChanged();
		}
	}

    public static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
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
}
