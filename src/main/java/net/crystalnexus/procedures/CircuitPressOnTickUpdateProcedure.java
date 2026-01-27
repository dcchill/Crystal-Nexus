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
import net.crystalnexus.init.CrystalnexusModItems;

import java.util.stream.Collectors;
import java.util.List;

public class CircuitPressOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
		double cookTime = 0;
		double outputAmount = 0;
		String registry_name = "";
		String registry_name_no_namespace = "";
		String registry_name_ore = "";
		String registry_name_nugget = "";
		outputAmount = 1;

		// Blockstate animation
		if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") == 0) {
			{
				int _value = 1;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		} else {
			{
				int _value = 2;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		}

		// Base cook time from upgrade item
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).copy()).getItem() == CrystalnexusModItems.ACCELERATION_UPGRADE.get()) {
			cookTime = 75;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).copy()).getItem() == CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) {
			cookTime = 50;
		} else {
			cookTime = 100;
		}

		// Optional multipliers on the upgrade stack (CUSTOM_DATA)
		double _cn_cookMult = 1.0;
		double _cn_outputMult = 1.0;
		boolean _cn_hasKeys = false;
		ItemStack _cn_upg = itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).copy();
		CompoundTag _cn_data = null;

		if (!_cn_upg.isEmpty() && _cn_upg.has(DataComponents.CUSTOM_DATA)) {
			CustomData _cn_cd = _cn_upg.get(DataComponents.CUSTOM_DATA);
			if (_cn_cd != null)
				_cn_data = _cn_cd.copyTag();
		}

		if (_cn_data != null && (_cn_data.contains("cook_mult") || _cn_data.contains("output_mult"))) {
			_cn_hasKeys = true;
			if (_cn_data.contains("cook_mult"))
				_cn_cookMult = _cn_data.getDouble("cook_mult");
			if (_cn_data.contains("output_mult"))
				_cn_outputMult = _cn_data.getDouble("output_mult");
		}

		// Apply multipliers
		if (_cn_hasKeys) {
			_cn_cookMult = Math.max(0.05, Math.min(_cn_cookMult, 10.0));
			_cn_outputMult = Math.max(0.0, Math.min(_cn_outputMult, 10.0));
			cookTime = cookTime * _cn_cookMult;
			outputAmount = outputAmount * _cn_outputMult;
		}

		// Sync maxProgress for GUI/progress bars
		if (cookTime < 1)
			cookTime = 1;

		if (!world.isClientSide()) {
			BlockPos _bp = BlockPos.containing(x, y, z);
			BlockEntity _blockEntity = world.getBlockEntity(_bp);
			BlockState _bs = world.getBlockState(_bp);
			if (_blockEntity != null)
				_blockEntity.getPersistentData().putDouble("maxProgress", cookTime);
			if (world instanceof Level _level)
				_level.sendBlockUpdated(_bp, _bs, _bs, 3);
		}

		// Resolve recipe result
		ItemStack _cn_result = (new Object() {
			public ItemStack getResult() {
				if (world instanceof Level _lvl) {
					net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
					List<CircuitPressingRecipe> recipes = rm.getAllRecipesFor(CircuitPressingRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
					for (CircuitPressingRecipe recipe : recipes) {
						NonNullList<Ingredient> ingredients = recipe.getIngredients();
						if (!ingredients.get(0).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())))
							continue;
						if (!ingredients.get(1).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy())))
							continue;
						return recipe.getResultItem(null);
					}
				}
				return ItemStack.EMPTY;
			}
		}.getResult()).copy();

		// If no valid result, do nothing
		if (Blocks.AIR.asItem() == _cn_result.getItem()) {
			return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, BlockPos.containing(x, y, z), null));
		}

		// ---- OUTPUT LIMIT FIX (respects handler slot limit + item max stack size) ----
		int MACHINE_MAX_OUTPUT = 4; // per craft
		int out = (int) Math.floor(outputAmount);
		if (out < 0)
			out = 0;
		if (out > MACHINE_MAX_OUTPUT)
			out = MACHINE_MAX_OUTPUT;

		// Slot 1 limits
		int slotMax = 64; // fallback
		if (world instanceof ILevelExtension _ext) {
			IItemHandler _ih = _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null);
			if (_ih != null) {
				slotMax = _ih.getSlotLimit(1);
			}
		}

		int itemMax = Math.min(_cn_result.getMaxStackSize(), 64);
		int realMax = Math.min(slotMax, itemMax);

		int current = itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount();
		int spaceLeft = Math.max(0, realMax - current);

		if (out > spaceLeft)
			out = spaceLeft;

		// If there's no space, we should not be able to finish a craft
		// (you can still tick progress if you want; this code only blocks completion safely)
		// ---------------------------------------------------------------------------

		// Main machine logic
		if (!(Blocks.AIR.asItem() == _cn_result.getItem())) {
			if (4096 <= getEnergyStored(world, BlockPos.containing(x, y, z), null)) {

				// Only allow processing if output slot is compatible and has space
				if (out > 0) {
					if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy()).getItem() == _cn_result.getItem()
							|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy()).getItem() == Blocks.AIR.asItem()) {

						// Progress
						if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") < cookTime) {
							if (!world.isClientSide()) {
								BlockPos _bp = BlockPos.containing(x, y, z);
								BlockEntity _blockEntity = world.getBlockEntity(_bp);
								BlockState _bs = world.getBlockState(_bp);
								if (_blockEntity != null)
									_blockEntity.getPersistentData().putDouble("progress", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") + 1));
								if (world instanceof Level _level)
									_level.sendBlockUpdated(_bp, _bs, _bs, 3);
							}
							if (world instanceof ServerLevel _level)
								_level.sendParticles(ParticleTypes.DRAGON_BREATH, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.25, 0, 0.25, 0);
						}

						// Complete craft
						if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= cookTime) {

							// Insert output (clamped to realMax)
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int current2 = itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount();

								int slotMax2 = 64;
								int realMax2 = Math.min(_cn_result.getMaxStackSize(), 64);
								IItemHandler _ih2 = _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null);
								if (_ih2 != null) {
									slotMax2 = _ih2.getSlotLimit(1);
									realMax2 = Math.min(realMax2, slotMax2);
								}

								int newCount = Math.min(current2 + out, realMax2);

								ItemStack _setstack = _cn_result.copy();
								_setstack.setCount(newCount);
								_itemHandlerModifiable.setStackInSlot(1, _setstack);
							}

							// Consume inputs
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int _slotid = 0;
								ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
								_stk.shrink(1);
								_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
							}
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int _slotid = 2;
								ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
								_stk.shrink(1);
								_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
							}

							// Reset progress
							if (!world.isClientSide()) {
								BlockPos _bp = BlockPos.containing(x, y, z);
								BlockEntity _blockEntity = world.getBlockEntity(_bp);
								BlockState _bs = world.getBlockState(_bp);
								if (_blockEntity != null)
									_blockEntity.getPersistentData().putDouble("progress", 0);
								if (world instanceof Level _level)
									_level.sendBlockUpdated(_bp, _bs, _bs, 3);
							}

							// Consume energy
							if (world instanceof ILevelExtension _ext) {
								IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
								if (_entityStorage != null)
									_entityStorage.extractEnergy(4096, false);
							}
						}
					}
				}
			}
		}

		return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, BlockPos.containing(x, y, z), null));
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
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
}
