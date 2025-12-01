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

import java.util.stream.Collectors;
import java.util.List;

public class MatterTransmutationTableOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
		double craftCount = 0;
		double outputAmount = 0;
		double cookTime = 0;
		String registry_name_no_namespace = "";
		String registry_name_nugget = "";
		String registry_name = "";
		outputAmount = (new Object() {
			public ItemStack getResult() {
				if (world instanceof Level _lvl) {
					net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
					List<MatterTransmutationRecipe> recipes = rm.getAllRecipesFor(MatterTransmutationRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
					for (MatterTransmutationRecipe recipe : recipes) {
						NonNullList<Ingredient> ingredients = recipe.getIngredients();
						if (!ingredients.get(0).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())))
							continue;
						if (!ingredients.get(1).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy())))
							continue;
						if (!ingredients.get(2).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy())))
							continue;
						if (!ingredients.get(3).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).copy())))
							continue;
						if (!ingredients.get(4).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 4).copy())))
							continue;
						if (!ingredients.get(5).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 5).copy())))
							continue;
						if (!ingredients.get(6).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 6).copy())))
							continue;
						if (!ingredients.get(7).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 7).copy())))
							continue;
						return recipe.getResultItem(null);
					}
				}
				return ItemStack.EMPTY;
			}
		}.getResult()).getCount();
		cookTime = 100;
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
					List<MatterTransmutationRecipe> recipes = rm.getAllRecipesFor(MatterTransmutationRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
					for (MatterTransmutationRecipe recipe : recipes) {
						NonNullList<Ingredient> ingredients = recipe.getIngredients();
						if (!ingredients.get(0).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())))
							continue;
						if (!ingredients.get(1).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy())))
							continue;
						if (!ingredients.get(2).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy())))
							continue;
						if (!ingredients.get(3).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).copy())))
							continue;
						if (!ingredients.get(4).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 4).copy())))
							continue;
						if (!ingredients.get(5).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 5).copy())))
							continue;
						if (!ingredients.get(6).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 6).copy())))
							continue;
						if (!ingredients.get(7).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 7).copy())))
							continue;
						return recipe.getResultItem(null);
					}
				}
				return ItemStack.EMPTY;
			}
		}.getResult()).getItem())) {
			if (1024 <= getEnergyStored(world, BlockPos.containing(x, y, z), null)) {
				if (64 >= itemFromBlockInventory(world, BlockPos.containing(x, y, z), 8).getCount() + (new Object() {
					public ItemStack getResult() {
						if (world instanceof Level _lvl) {
							net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
							List<MatterTransmutationRecipe> recipes = rm.getAllRecipesFor(MatterTransmutationRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
							for (MatterTransmutationRecipe recipe : recipes) {
								NonNullList<Ingredient> ingredients = recipe.getIngredients();
								if (!ingredients.get(0).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())))
									continue;
								if (!ingredients.get(1).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy())))
									continue;
								if (!ingredients.get(2).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy())))
									continue;
								if (!ingredients.get(3).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).copy())))
									continue;
								if (!ingredients.get(4).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 4).copy())))
									continue;
								if (!ingredients.get(5).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 5).copy())))
									continue;
								if (!ingredients.get(6).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 6).copy())))
									continue;
								if (!ingredients.get(7).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 7).copy())))
									continue;
								return recipe.getResultItem(null);
							}
						}
						return ItemStack.EMPTY;
					}
				}.getResult()).getCount()) {
					if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 8).copy()).getItem() == (new Object() {
						public ItemStack getResult() {
							if (world instanceof Level _lvl) {
								net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
								List<MatterTransmutationRecipe> recipes = rm.getAllRecipesFor(MatterTransmutationRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
								for (MatterTransmutationRecipe recipe : recipes) {
									NonNullList<Ingredient> ingredients = recipe.getIngredients();
									if (!ingredients.get(0).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())))
										continue;
									if (!ingredients.get(1).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy())))
										continue;
									if (!ingredients.get(2).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy())))
										continue;
									if (!ingredients.get(3).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).copy())))
										continue;
									if (!ingredients.get(4).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 4).copy())))
										continue;
									if (!ingredients.get(5).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 5).copy())))
										continue;
									if (!ingredients.get(6).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 6).copy())))
										continue;
									if (!ingredients.get(7).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 7).copy())))
										continue;
									return recipe.getResultItem(null);
								}
							}
							return ItemStack.EMPTY;
						}
					}.getResult()).getItem() || (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 8).copy()).getItem() == Blocks.AIR.asItem()) {
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
								_level.sendParticles(ParticleTypes.ELECTRIC_SPARK, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.25, 0, 0.25, 0);
						}
						if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= cookTime) {
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								ItemStack _setstack = (new Object() {
									public ItemStack getResult() {
										if (world instanceof Level _lvl) {
											net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
											List<MatterTransmutationRecipe> recipes = rm.getAllRecipesFor(MatterTransmutationRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
											for (MatterTransmutationRecipe recipe : recipes) {
												NonNullList<Ingredient> ingredients = recipe.getIngredients();
												if (!ingredients.get(0).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())))
													continue;
												if (!ingredients.get(1).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy())))
													continue;
												if (!ingredients.get(2).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy())))
													continue;
												if (!ingredients.get(3).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 3).copy())))
													continue;
												if (!ingredients.get(4).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 4).copy())))
													continue;
												if (!ingredients.get(5).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 5).copy())))
													continue;
												if (!ingredients.get(6).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 6).copy())))
													continue;
												if (!ingredients.get(7).test((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 7).copy())))
													continue;
												return recipe.getResultItem(null);
											}
										}
										return ItemStack.EMPTY;
									}
								}.getResult()).copy();
								_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 8).getCount() + outputAmount));
								_itemHandlerModifiable.setStackInSlot(8, _setstack);
							}
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int _slotid = 0;
								ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
								_stk.shrink(1);
								_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
							}
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int _slotid = 1;
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
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int _slotid = 3;
								ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
								_stk.shrink(1);
								_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
							}
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int _slotid = 4;
								ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
								_stk.shrink(1);
								_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
							}
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int _slotid = 5;
								ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
								_stk.shrink(1);
								_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
							}
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int _slotid = 6;
								ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
								_stk.shrink(1);
								_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
							}
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								int _slotid = 7;
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
									_entityStorage.extractEnergy(1024, false);
							}
						}
					}
				}
			}
		}
		return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, BlockPos.containing(x, y, z), null));
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