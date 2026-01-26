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

import net.crystalnexus.jei_recipes.EnergyExtractionRecipe;
import net.crystalnexus.init.CrystalnexusModItems;

import java.util.stream.Collectors;
import java.util.List;

public class EnergyExtractorOnTickUpdateProcedure {

	public static String execute(LevelAccessor world, double x, double y, double z) {

		BlockPos pos = BlockPos.containing(x, y, z);

		double cookTime;
		double energyBase;
		double outputAmount = 1;

		// ======================
		// VISUAL STATE
		// ======================
		int state = getBlockNBTNumber(world, pos, "progress") == 0 ? 1 : 2;
		BlockState bs = world.getBlockState(pos);
		if (bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty prop
				&& prop.getPossibleValues().contains(state)) {
			world.setBlock(pos, bs.setValue(prop, state), 3);
		}

		// ======================
		// UPGRADES
		// ======================
		ItemStack upgrade = itemFromBlockInventory(world, pos, 1);

		energyBase = 4096000;
		cookTime = 200;

		if (upgrade.getItem() == CrystalnexusModItems.ACCELERATION_UPGRADE.get())
			cookTime = 175;
		else if (upgrade.getItem() == CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get())
			cookTime = 100;

		// ======================
		// STORE + SYNC MAX PROGRESS
		// ======================
		setNBTAndSync(world, pos, "maxProgress", cookTime);

		// ======================
		// RECIPE LOOKUP
		// ======================
		ItemStack recipeResult = getRecipeResult(world, pos);
		boolean hasRecipe = !recipeResult.isEmpty() && recipeResult.getItem() != Blocks.AIR.asItem();

		// =========================================================
		// PRIMARY MODE – RECIPE → FE
		// =========================================================
		if (hasRecipe) {

			if (getEnergyStored(world, pos, null) < getMaxEnergyStored(world, pos, null)) {

				// output slot room + match
				if (64 != itemFromBlockInventory(world, pos, 2).getCount()) {

					ItemStack outSlot = itemFromBlockInventory(world, pos, 2).copy();
					if (outSlot.isEmpty() || outSlot.getItem() == recipeResult.getItem()) {

						double prog = getBlockNBTNumber(world, pos, "progress");

						if (prog < cookTime) {
							setNBTAndSync(world, pos, "progress", prog + 1);

							if (world instanceof ServerLevel lvl)
								lvl.sendParticles(ParticleTypes.DRAGON_BREATH,
										x + 0.5, y + 0.5, z + 0.5,
										1, 0.25, 0, 0.25, 0);
						}

						if (getBlockNBTNumber(world, pos, "progress") >= cookTime) {

							// output item + consume input
							if (world instanceof ILevelExtension ext
									&& ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable inv) {

								ItemStack out = inv.getStackInSlot(2);
								ItemStack newOut = recipeResult.copy();
								newOut.setCount(out.getCount() + (int) outputAmount);
								inv.setStackInSlot(2, newOut);

								ItemStack in = inv.getStackInSlot(0);
								in.shrink(1);
								inv.setStackInSlot(0, in);
							}

							// charge internal FE
							if (world instanceof ILevelExtension ext) {
								IEnergyStorage es = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
								if (es != null)
									es.receiveEnergy((int) energyBase, false);
							}

							// reset progress and sync
							setNBTAndSync(world, pos, "progress", 0);
						}
					}
				}
			}
		}

		// =========================================================
		// SECONDARY MODE – BATTERY → FE (drains real stack + syncs)
		// =========================================================
		else {

			if (world instanceof ILevelExtension ext
					&& ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable inv) {

				// REAL stack (no copy)
				ItemStack batteryStack = inv.getStackInSlot(0);
				IEnergyStorage battery = batteryStack.getCapability(Capabilities.EnergyStorage.ITEM);

				if (battery != null && battery.canExtract()) {

					int max = getMaxEnergyStored(world, pos, null);
					int stored = getEnergyStored(world, pos, null);

					if (stored < max) {
						int rate = (int) Math.max(1, energyBase / 8);
						int room = max - stored;
						int request = Math.min(rate, room);

						int simPull = battery.extractEnergy(request, true);
						int simPush = receiveEnergySimulate(world, pos, simPull, null);
						int move = Math.min(simPull, simPush);

						if (move > 0) {
							int pulled = battery.extractEnergy(move, false);

							if (pulled > 0) {
								IEnergyStorage blockES = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
								if (blockES != null)
									blockES.receiveEnergy(pulled, false);

								// IMPORTANT: re-set stack to force save/sync
								inv.setStackInSlot(0, batteryStack);

								if (world instanceof ServerLevel lvl)
									lvl.sendParticles(ParticleTypes.END_ROD,
											x + 0.5, y + 0.6, z + 0.5,
											1, 0.1, 0.1, 0.1, 0);
							}
						}
					}
				}
			}

			// battery mode keeps progress 0 and syncs it
			if (getBlockNBTNumber(world, pos, "progress") != 0)
				setNBTAndSync(world, pos, "progress", 0);
		}

		return new java.text.DecimalFormat("FE: ##.##")
				.format(getEnergyStored(world, pos, null));
	}

	// =========================================================
	// HELPERS
	// =========================================================

	private static ItemStack getRecipeResult(LevelAccessor world, BlockPos pos) {
		if (world instanceof Level lvl) {
			ItemStack in = itemFromBlockInventory(world, pos, 0).copy();
			List<EnergyExtractionRecipe> recipes = lvl.getRecipeManager()
					.getAllRecipesFor(EnergyExtractionRecipe.Type.INSTANCE)
					.stream().map(RecipeHolder::value).collect(Collectors.toList());

			for (EnergyExtractionRecipe r : recipes) {
				NonNullList<Ingredient> ing = r.getIngredients();
				if (!ing.get(0).test(in))
					continue;
				return r.getResultItem(null);
			}
		}
		return ItemStack.EMPTY;
	}

	/** Write persistent NBT AND sync to client with sendBlockUpdated. */
	private static void setNBTAndSync(LevelAccessor world, BlockPos pos, String key, double value) {
		if (world.isClientSide())
			return;

		BlockEntity be = world.getBlockEntity(pos);
		if (be == null)
			return;

		double old = be.getPersistentData().getDouble(key);
		if (old == value)
			return; // avoid spam updates

		be.getPersistentData().putDouble(key, value);

		if (world instanceof Level lvl) {
			BlockState bs = world.getBlockState(pos);
			lvl.sendBlockUpdated(pos, bs, bs, 3);
		}
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity be = world.getBlockEntity(pos);
		return be != null ? be.getPersistentData().getDouble(tag) : 0;
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler h = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (h != null)
				return h.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}

	private static int getEnergyStored(LevelAccessor world, BlockPos pos, Direction dir) {
		if (world instanceof ILevelExtension ext) {
			IEnergyStorage es = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, dir);
			if (es != null)
				return es.getEnergyStored();
		}
		return 0;
	}

	private static int getMaxEnergyStored(LevelAccessor world, BlockPos pos, Direction dir) {
		if (world instanceof ILevelExtension ext) {
			IEnergyStorage es = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, dir);
			if (es != null)
				return es.getMaxEnergyStored();
		}
		return 0;
	}

	private static int receiveEnergySimulate(LevelAccessor world, BlockPos pos, int amount, Direction dir) {
		if (world instanceof ILevelExtension ext) {
			IEnergyStorage es = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, dir);
			if (es != null)
				return es.receiveEnergy(amount, true);
		}
		return 0;
	}
}
