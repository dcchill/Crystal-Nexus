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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;


public class CrystalSmelterOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof Level level) || level.isClientSide()) return;
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!(level.getBlockEntity(pos) instanceof net.crystalnexus.block.entity.CrystalSmelterBlockEntity machine)) return;
        machine.processing = false;
        IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        IEnergyStorage energy = machine.getEnergyStorage();
        if (inventory == null) return;
        ItemStack inputStack = inventory.getStackInSlot(0);
        ItemStack outputStack = inventory.getStackInSlot(1);

        var upgrade = MachineUpgradeHelper.upgrades(machine, 2, 3);
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,
            new SingleRecipeInput(inputStack), level);
        ItemStack recipeResult = recipe.map(holder ->
            holder.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
		double outputAmount = 0;
		double cookTime = 0;
		outputAmount = 1;
		if (net.crystalnexus.util.MachineAnimationHelper.shouldIdle(world, pos, machine.getPersistentData().getDouble("progress"))) {
			{
				int _value = 1;
				BlockPos _pos = pos;
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value) && _bs.getValue(_integerProp) != _value)
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		} else {
			{
				int _value = 2;
				BlockPos _pos = pos;
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value) && _bs.getValue(_integerProp) != _value)
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		}
		cookTime = MachineUpgradeHelper.processingTime(upgrade, 75, 50, 35);
        int energyCost = MachineUpgradeHelper.energyCost(machine.getBlockState(), upgrade, 2048);
        cookTime = MachineUpgradeHelper.cookTime(upgrade, cookTime);
		double MACHINE_MAX_OUTPUT = 4; 
		if (outputAmount > MACHINE_MAX_OUTPUT)
			outputAmount = MACHINE_MAX_OUTPUT;
		double _cn_currentCount = outputStack.getCount();
		double _cn_spaceLeft = net.crystalnexus.util.MachineItemStorage.slotLimit(machine, 1, recipeResult) - _cn_currentCount; 
		if (outputAmount > _cn_spaceLeft)
			outputAmount = _cn_spaceLeft;
		if (outputAmount < 0)
			outputAmount = 0;
		int perCraftOutput = (MachineUpgradeHelper.parallelCraftCount(upgrade) > 1)
				? Math.max(1, recipeResult.getCount()) : 1;

		int outputSpace = net.crystalnexus.util.MachineItemStorage.slotLimit(machine, 1, recipeResult) - outputStack.getCount();
		if (!outputStack.isEmpty() && !ItemStack.isSameItemSameComponents(outputStack, recipeResult))
			outputSpace = 0;
		int batchCrafts = Math.min(net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(upgrade), inputStack.getCount());
		batchCrafts = Math.min(batchCrafts, Math.max(0, outputSpace) / perCraftOutput);
		int energyPerCraft = Math.max(1, Math.min(energyCost, net.crystalnexus.config.CrystalnexusConfig.MACHINES.CRYSTAL_SMELTER.maxExtract()));
		batchCrafts = Math.min(batchCrafts, energy.getEnergyStored() / energyPerCraft);
		outputAmount = batchCrafts * perCraftOutput;
		if (cookTime < 1)
			cookTime = 1;
		machine.machineSync.setDouble("maxProgress", cookTime);
		if (recipe.isPresent() && batchCrafts > 0) {
			if (Math.min(energyCost, net.crystalnexus.config.CrystalnexusConfig.MACHINES.CRYSTAL_SMELTER.maxExtract()) <= energy.getEnergyStored()) {
				if (outputSpace > 0 && (outputStack.getItem() == (recipeResult).getItem() || outputStack.getItem() == Blocks.AIR.asItem())) {
					if (machine.getPersistentData().getDouble("progress") < cookTime) {
						machine.machineSync.setDouble("progress", (machine.getPersistentData().getDouble("progress") + 1));
						machine.processing = true;
						if (world instanceof ServerLevel _level && net.crystalnexus.util.MachineSync.isUpdateTick(_level.getGameTime(), pos))
							_level.sendParticles(ParticleTypes.DRAGON_BREATH, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.25, 0, 0.25, 0);
					}
					if (machine.getPersistentData().getDouble("progress") >= cookTime) {
						if (world instanceof ILevelExtension _ext && inventory instanceof IItemHandlerModifiable _itemHandlerModifiable) {
							ItemStack _setstack = (recipeResult).copy();
							_setstack.setCount((int) (outputStack.getCount() + outputAmount));
							_itemHandlerModifiable.setStackInSlot(1, _setstack);
						}
						if (world instanceof ILevelExtension _ext && inventory instanceof IItemHandlerModifiable _itemHandlerModifiable) {
							int _slotid = 0;
							ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
							_stk.shrink(batchCrafts);
							_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
						}
						machine.machineSync.setDouble("progress", 0);
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = energy;
							if (_entityStorage != null)
								for (int craftIndex = 0; craftIndex < batchCrafts; craftIndex++)
									_entityStorage.extractEnergy(energyPerCraft, false);
						}
					}
				}
			}
		}
		return;
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
