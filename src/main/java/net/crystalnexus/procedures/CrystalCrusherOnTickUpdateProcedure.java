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

import net.crystalnexus.jei_recipes.OreCrushingJeiRecipe;
import net.crystalnexus.init.CrystalnexusModItems;

import java.util.stream.Collectors;
import java.util.List;

public class CrystalCrusherOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
		double cookTime = 0;
		double outputAmount = 0;
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
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy()).getItem() == CrystalnexusModItems.EFFICIENCY_UPGRADE.get()) {
			outputAmount = 3;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy()).getItem() == CrystalnexusModItems.CARBON_EFFICIENCY_UPGRADE.get()) {
			outputAmount = 4;
		} else {
			outputAmount = 2;
		}
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy()).getItem() == CrystalnexusModItems.ACCELERATION_UPGRADE.get()) {
			cookTime = 75;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy()).getItem() == CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) {
			cookTime = 50;
		} else {
			cookTime = 100;
		}
		double _cn_cookMult = 1.0;
		double _cn_outputMult = 1.0;
		boolean _cn_hasKeys = false;
		ItemStack _cn_upg = itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy();
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
		// 2) Apply multipliers (STACK onto existing values)
		if (_cn_hasKeys) {
			_cn_cookMult = Math.max(0.05, Math.min(_cn_cookMult, 10.0));
			_cn_outputMult = Math.max(0.0, Math.min(_cn_outputMult, 10.0));
			cookTime = cookTime * _cn_cookMult;
			outputAmount = outputAmount * _cn_outputMult;
		}
		// 3) Output caps (machine cap + slot space cap)
		double MACHINE_MAX_OUTPUT = 16; // set per machine
		if (outputAmount > MACHINE_MAX_OUTPUT)
			outputAmount = MACHINE_MAX_OUTPUT;
		if (outputAmount < 0)
			outputAmount = 0;
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
		if (!(Blocks.AIR.asItem() == (new Object() {
			public ItemStack getResult() {
				if (world instanceof Level _lvl) {
					net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
					List<OreCrushingJeiRecipe> recipes = rm.getAllRecipesFor(OreCrushingJeiRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
					for (OreCrushingJeiRecipe recipe : recipes) {
						NonNullList<Ingredient> ingredients = recipe.getIngredients();
						if (!ingredients.get(0).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())))
							continue;
						return recipe.getResultItem(null);
					}
				}
				return ItemStack.EMPTY;
			}
		}.getResult()).getItem())) {
			if (4096 <= getEnergyStored(world, BlockPos.containing(x, y, z), null)) {
				if (64 >= itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() + outputAmount) {
					if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy()).getItem() == (new Object() {
						public ItemStack getResult() {
							if (world instanceof Level _lvl) {
								net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
								List<OreCrushingJeiRecipe> recipes = rm.getAllRecipesFor(OreCrushingJeiRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
								for (OreCrushingJeiRecipe recipe : recipes) {
									NonNullList<Ingredient> ingredients = recipe.getIngredients();
									if (!ingredients.get(0).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())))
										continue;
									return recipe.getResultItem(null);
								}
							}
							return ItemStack.EMPTY;
						}
					}.getResult()).getItem() || (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy()).getItem() == Blocks.AIR.asItem()) {
						if (CrystalnexusModItems.NETHERITE_SCRAP_DUST.get() == (new Object() {
							public ItemStack getResult() {
								if (world instanceof Level _lvl) {
									net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
									List<OreCrushingJeiRecipe> recipes = rm.getAllRecipesFor(OreCrushingJeiRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
									for (OreCrushingJeiRecipe recipe : recipes) {
										NonNullList<Ingredient> ingredients = recipe.getIngredients();
										if (!ingredients.get(0).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())))
											continue;
										return recipe.getResultItem(null);
									}
								}
								return ItemStack.EMPTY;
							}
						}.getResult()).getItem()) {
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
							if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= cookTime) {
								if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
									ItemStack _setstack = (new Object() {
										public ItemStack getResult() {
											if (world instanceof Level _lvl) {
												net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
												List<OreCrushingJeiRecipe> recipes = rm.getAllRecipesFor(OreCrushingJeiRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
												for (OreCrushingJeiRecipe recipe : recipes) {
													NonNullList<Ingredient> ingredients = recipe.getIngredients();
													if (!ingredients.get(0).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())))
														continue;
													return recipe.getResultItem(null);
												}
											}
											return ItemStack.EMPTY;
										}
									}.getResult()).copy();
									_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() + 4);
									_itemHandlerModifiable.setStackInSlot(1, _setstack);
								}
								if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
									int _slotid = 0;
									ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
									_stk.shrink(1);
									_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
								}
								if (!world.isClientSide()) {
									BlockPos _bp = BlockPos.containing(x, y, z);
									BlockEntity _blockEntity = world.getBlockEntity(_bp);
									BlockState _bs = world.getBlockState(_bp);
									if (_blockEntity != null)
										_blockEntity.getPersistentData().putDouble("progress", 0);
									if (world instanceof Level _level)
										_level.sendBlockUpdated(_bp, _bs, _bs, 3);
								}
								if (world instanceof ILevelExtension _ext) {
									IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
									if (_entityStorage != null)
										_entityStorage.extractEnergy(4096, false);
								}
							}
						} else {
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
							if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= cookTime) {
								if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
									ItemStack _setstack = (new Object() {
										public ItemStack getResult() {
											if (world instanceof Level _lvl) {
												net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
												List<OreCrushingJeiRecipe> recipes = rm.getAllRecipesFor(OreCrushingJeiRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
												for (OreCrushingJeiRecipe recipe : recipes) {
													NonNullList<Ingredient> ingredients = recipe.getIngredients();
													if (!ingredients.get(0).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())))
														continue;
													return recipe.getResultItem(null);
												}
											}
											return ItemStack.EMPTY;
										}
									}.getResult()).copy();
									_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() + outputAmount));
									_itemHandlerModifiable.setStackInSlot(1, _setstack);
								}
								if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
									int _slotid = 0;
									ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
									_stk.shrink(1);
									_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
								}
								if (!world.isClientSide()) {
									BlockPos _bp = BlockPos.containing(x, y, z);
									BlockEntity _blockEntity = world.getBlockEntity(_bp);
									BlockState _bs = world.getBlockState(_bp);
									if (_blockEntity != null)
										_blockEntity.getPersistentData().putDouble("progress", 0);
									if (world instanceof Level _level)
										_level.sendBlockUpdated(_bp, _bs, _bs, 3);
								}
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