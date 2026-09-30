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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
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
        if (!(world instanceof Level level) || level.isClientSide()) return;
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!(level.getBlockEntity(pos) instanceof net.crystalnexus.block.entity.CircuitPressBlockEntity machine)) return;
        IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        IEnergyStorage energy = machine.getEnergyStorage();
        if (inventory == null) return;
        ItemStack inputStack = inventory.getStackInSlot(0);
        ItemStack outputStack = inventory.getStackInSlot(1);
        ItemStack materialStack = inventory.getStackInSlot(2);

        ItemStack upgrade = inventory.getStackInSlot(3);
		double cookTime = 0;
		double outputAmount = 0;
		outputAmount = 1;
		BlockPos pressPos = pos;
		boolean batchPress = world.getBlockState(pressPos).is(CrystalnexusModBlocks.TITANIUM_CARBIDE_CIRCUIT_PRESS.get());
		boolean craftingThisTick = false;
		int batchSize = 1;
		outputAmount = batchSize;

		if (!batchPress && net.crystalnexus.util.MachineAnimationHelper.shouldIdle(machine, machine.getBlockState(), machine.getPersistentData().getDouble("progress"))) {
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

		
		double _cn_cookMult = 1.0;
		boolean _cn_hasKeys = false;
		ItemStack _cn_upg = upgrade;
        int energyCost = MachineUpgradeHelper.energyCost(machine.getBlockState(), upgrade, 2048);
		CompoundTag _cn_data = null;

		if (!_cn_upg.isEmpty() && _cn_upg.has(DataComponents.CUSTOM_DATA)) {
			CustomData _cn_cd = _cn_upg.get(DataComponents.CUSTOM_DATA);
			if (_cn_cd != null)
				_cn_data = _cn_cd.copyTag();
		}

		if (_cn_data != null && _cn_data.contains("cook_mult")) {
			_cn_hasKeys = true;
			if (_cn_data.contains("cook_mult"))
				_cn_cookMult = _cn_data.getDouble("cook_mult");
		}

		if (_cn_hasKeys) {
			_cn_cookMult = Math.max(0.05, Math.min(_cn_cookMult, 10.0));
			cookTime = cookTime * _cn_cookMult;
		}
		cookTime = MachineTier.from(world.getBlockState(pressPos)).processingTime(cookTime);

		if (cookTime < 1)
			cookTime = 1;

		machine.machineSync.setDouble("maxProgress", cookTime);

		var assigned = net.crystalnexus.assembly.AssemblyLineMachine.assignedRecipe(machine);
		var recipeMatch = machine.recipeCache.find(level, inputStack, materialStack,
			machine.getNitrogenTank().getFluid(), batchPress, assigned);
		TitaniumCarbideCircuitPressRecipe advancedRecipe = recipeMatch.advanced();
		ItemStack _cn_result = recipeMatch.result();
		if (advancedRecipe != null) {
			outputAmount = 1;
		} else if (batchPress) {
			batchSize = 8;
			outputAmount = batchSize;
		}

		if (Blocks.AIR.asItem() == _cn_result.getItem()) {
			setBatchPressState(world, pressPos, batchPress, 1);
			return;
		}

		
		int out = (int) Math.floor(outputAmount * _cn_result.getCount());
		if (out < 0)
			out = 0;

		int slotMax = 64; 
		if (world instanceof ILevelExtension _ext) {
			IItemHandler _ih = inventory;
			if (_ih != null) {
				slotMax = _ih.getSlotLimit(1);
			}
		}

		int itemMax = Math.min(_cn_result.getMaxStackSize(), 64);
		int realMax = Math.min(slotMax, itemMax);

		int current = outputStack.getCount();
		int spaceLeft = Math.max(0, realMax - current);

		if (batchPress && advancedRecipe == null) {
			batchSize = CircuitPressBatching.basicBatchSize(inputStack.getCount(),
				materialStack.getCount(), spaceLeft, _cn_result.getCount());
			out = batchSize * _cn_result.getCount();
		} else if (out > spaceLeft)
			out = spaceLeft;


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
					if (outputStack.getItem() == _cn_result.getItem()
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

								int slotMax2 = 64;
								int realMax2 = Math.min(_cn_result.getMaxStackSize(), 64);
								IItemHandler _ih2 = inventory;
								if (_ih2 != null) {
									slotMax2 = _ih2.getSlotLimit(1);
									realMax2 = Math.min(realMax2, slotMax2);
								}

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

		return;
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
