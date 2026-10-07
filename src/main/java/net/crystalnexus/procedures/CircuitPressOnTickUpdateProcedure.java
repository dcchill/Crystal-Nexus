package net.crystalnexus.procedures;

import net.crystalnexus.util.MachineUpgradeHelper;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
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

import net.crystalnexus.jei_recipes.CircuitPressingRecipe;
import net.crystalnexus.jei_recipes.TitaniumCarbideCircuitPressRecipe;
import net.crystalnexus.block.entity.CircuitPressBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.block.CircuitPressBlock;
import net.crystalnexus.processing.MachineTier;


public class CircuitPressOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
        var machine = world.getBlockEntity(net.minecraft.core.BlockPos.containing(x, y, z));
        net.crystalnexus.util.MachineUpgradeHelper.processParallel(machine, net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(net.crystalnexus.util.MachineUpgradeHelper.upgrades(machine, 3, 4)), () -> executeSingle(world, x, y, z));
    }

    private static boolean executeSingle(LevelAccessor world, double x, double y, double z) {
        boolean completed = false;
        if (!(world instanceof Level level) || level.isClientSide()) return false;
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!(level.getBlockEntity(pos) instanceof net.crystalnexus.block.entity.CircuitPressBlockEntity machine)) return false;
        IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        IEnergyStorage energy = machine.getEnergyStorage();
        if (inventory == null) return false;
        ItemStack inputStack = inventory.getStackInSlot(0);
        ItemStack outputStack = inventory.getStackInSlot(1);
        ItemStack materialStack = inventory.getStackInSlot(2);

        var upgrade = MachineUpgradeHelper.upgrades(machine, 3, 4);
		double cookTime = 0;
		double outputAmount = 0;
		outputAmount = 1;
		BlockPos pressPos = pos;
		boolean batchPress = world.getBlockState(pressPos).is(CrystalnexusModBlocks.TITANIUM_CARBIDE_CIRCUIT_PRESS.get());
		boolean craftingThisTick = false;
		int batchSize = 1;
		outputAmount = batchSize;

		if (!batchPress && net.crystalnexus.util.MachineAnimationHelper.shouldIdle(world, pos, machine.getPersistentData().getDouble("progress"))) {
			{
				int _value = 1;
				BlockPos _pos = pos;
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value) && _bs.getValue(_integerProp) != _value)
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		} else if (!batchPress) {
			{
				int _value = 2;
				BlockPos _pos = pos;
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value) && _bs.getValue(_integerProp) != _value)
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		}

		cookTime = MachineUpgradeHelper.processingTime(upgrade, 100, 75, 50);

		
        int energyCost = MachineUpgradeHelper.energyCost(machine.getBlockState(), upgrade, 2048);
        cookTime = MachineUpgradeHelper.cookTime(upgrade, cookTime);
		cookTime = MachineTier.from(world.getBlockState(pressPos)).processingTime(cookTime);

		if (cookTime < 1)
			cookTime = 1;

		machine.machineSync.setDouble("maxProgress", cookTime);

		var assigned = net.crystalnexus.assembly.AssemblyLineMachine.assignedRecipe(machine);
		TitaniumCarbideCircuitPressRecipe advancedRecipe = null;
		if (batchPress) {
			for (var holder : level.getRecipeManager().getAllRecipesFor(TitaniumCarbideCircuitPressRecipe.Type.INSTANCE)) {
				var recipe = holder.value();
				if (recipe.itemInput(0).isPresent() && recipe.itemInput(0).get().test(inputStack)
						&& recipe.itemInput(1).map(ingredient -> ingredient.test(materialStack)).orElse(true)
						&& recipe.fluidInput(0).isPresent()
						&& recipe.fluidInput(0).get().matches(machine.getNitrogenTank().getFluid())) {
					advancedRecipe = recipe;
					break;
				}
			}
		}
		ItemStack _cn_result = ItemStack.EMPTY;
		if (advancedRecipe != null) {
			_cn_result = advancedRecipe.getResultItem(null).copy();
		} else {
			for (var holder : level.getRecipeManager().getAllRecipesFor(CircuitPressingRecipe.Type.INSTANCE)) {
				if (assigned != null && !assigned.equals(holder.id())) continue;
				var ingredients = holder.value().getIngredients();
				if (ingredients.get(0).test(inputStack) && ingredients.get(1).test(materialStack)) {
					_cn_result = holder.value().getResultItem(null).copy();
					break;
				}
			}
		}
		if (advancedRecipe != null) {
			outputAmount = 1;
		} else if (batchPress) {
			batchSize = 8;
			outputAmount = batchSize;
		}

		if (Blocks.AIR.asItem() == _cn_result.getItem()) {
			setBatchPressState(world, pressPos, batchPress, 1);
			return false;
		}

		
		int resultCount = _cn_result.getCount();
        int out = (int) Math.floor(outputAmount * resultCount);
		if (out < 0)
			out = 0;

		int realMax = net.crystalnexus.util.MachineItemStorage.slotLimit(machine, 1, _cn_result);

		int current = outputStack.getCount();
		int spaceLeft = Math.max(0, realMax - current);

		if (batchPress && advancedRecipe == null) {
			batchSize = CircuitPressBatching.basicBatchSize(inputStack.getCount(),
				materialStack.getCount(), spaceLeft, resultCount);
			out = batchSize * resultCount;
		} else if (out > spaceLeft)
            return false;


		if (!(Blocks.AIR.asItem() == _cn_result.getItem())) {
			ItemStack _cn_input = inputStack;
			ItemStack _cn_material = materialStack;
			int requiredInput = advancedRecipe == null ? batchSize : advancedRecipe.itemInputCount(0);
			int requiredMaterial = advancedRecipe == null ? batchSize
				: advancedRecipe.itemInput(1).isPresent() ? advancedRecipe.itemInputCount(1) : 0;
			boolean hasAdvancedFluid = advancedRecipe == null || machine.getNitrogenTank().getFluid().getAmount() >= advancedRecipe.fluidInput(0).get().amount();
			if (_cn_input.getCount() >= requiredInput && _cn_material.getCount() >= requiredMaterial
					&& hasAdvancedFluid
					&& (assigned != null
						? energyCost
						: Math.min(energyCost, net.crystalnexus.config.CrystalnexusConfig.MACHINES.CIRCUIT_PRESS.maxExtract())) <= energy.getEnergyStored()) {

				if (out > 0) {
					if (ItemStack.isSameItemSameComponents(outputStack, _cn_result)
							|| outputStack.getItem() == Blocks.AIR.asItem()) {

						if (machine.getPersistentData().getDouble("progress") < cookTime) {
							setBatchPressState(world, pressPos, batchPress, 2);
							craftingThisTick = true;
							machine.machineSync.setDouble("progress", (machine.getPersistentData().getDouble("progress") + 1));
							if (world instanceof ServerLevel _level && net.crystalnexus.util.MachineSync.isUpdateTick(_level.getGameTime(), pos))
								_level.sendParticles(ParticleTypes.DRAGON_BREATH, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.25, 0, 0.25, 0);
						}

						if (machine.getPersistentData().getDouble("progress") >= cookTime) {

							if (world instanceof ILevelExtension _ext && inventory instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int current2 = outputStack.getCount();

								int realMax2 = net.crystalnexus.util.MachineItemStorage.slotLimit(machine, 1, _cn_result);

								int newCount = Math.min(current2 + out, realMax2);

								ItemStack _setstack = _cn_result.copy();
								_setstack.setCount(newCount);
								_itemHandlerModifiable.setStackInSlot(1, _setstack);
							}

							if (world instanceof ILevelExtension _ext && inventory instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int _slotid = 0;
								ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
							_stk.shrink(requiredInput);
								_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
							}
							if (advancedRecipe != null) {
								machine.getNitrogenTank().drain(advancedRecipe.fluidInput(0).get().amount(), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
							}
							if (requiredMaterial > 0 && world instanceof ILevelExtension _ext && inventory instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int _slotid = 2;
								ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
								_stk.shrink(requiredMaterial);
								_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
							}

							machine.machineSync.setDouble("progress", 0);
        completed = true;

							if (world instanceof ILevelExtension _ext) {
								IEnergyStorage _entityStorage = energy;
								if (_entityStorage != null) {
                                    int cost = energyCost;
                                    if (assigned != null)
                                        net.crystalnexus.assembly.AssemblyLineMachine.consumeAssignedEnergy(_entityStorage, cost);
                                    else _entityStorage.extractEnergy(cost, false);
                                }
							}
						}
					}
				}
			}
		}
		if (!craftingThisTick)
			setBatchPressState(world, pressPos, batchPress, 1);

		return completed;
    }

	private static void setBatchPressState(LevelAccessor world, BlockPos pos, boolean batchPress, int value) {
		if (!batchPress || world.isClientSide()) return;
		BlockState state = world.getBlockState(pos);
		if (state.getValue(CircuitPressBlock.BLOCKSTATE) != value)
			world.setBlock(pos, state.setValue(CircuitPressBlock.BLOCKSTATE, value), 3);
	}

	public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.getEnergyStored();
		}
		return 0;
	}
}
