package net.crystalnexus.procedures;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public final class UltimaSmelterOnTickUpdateProcedure {
	private static final int ENERGY_PER_BATCH = 8192;
	private static final int[] INPUT_SLOTS = {0, 3, 5, 6};
	private static final int[] OUTPUT_SLOTS = {1, 7, 9, 10};

	private UltimaSmelterOnTickUpdateProcedure() {
	}

	public static String execute(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		checkMachineCore(world, pos);
		if (!(world instanceof Level level) || level.isClientSide() || !getBlockNBTLogic(world, pos, "canOpenInventory")) {
			return energyText(world, pos);
		}
		IItemHandler handler = itemHandler(world, pos);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (!(handler instanceof IItemHandlerModifiable inventory) || blockEntity == null) {
			return energyText(world, pos);
		}

		ItemStack upgrade = inventory.getStackInSlot(2);
		double baseCookTime = upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get()) ? 25
				: upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) ? 10 : 50;
		double cookTime = MachineUpgradeHelper.cookTime(upgrade, baseCookTime);
		blockEntity.getPersistentData().putDouble("maxProgress", cookTime);

		ItemStack[] results = new ItemStack[INPUT_SLOTS.length];
		boolean hasWork = false;
		for (int lane = 0; lane < INPUT_SLOTS.length; lane++) {
			results[lane] = smeltingResult(level, inventory.getStackInSlot(INPUT_SLOTS[lane]));
			if (!results[lane].isEmpty()) {
				results[lane].setCount(results[lane].getCount() * 2);
				if (!fits(inventory.getStackInSlot(OUTPUT_SLOTS[lane]), results[lane])) {
					results[lane] = ItemStack.EMPTY;
				} else {
					hasWork = true;
				}
			}
		}

		IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
		int energyCost = MachineUpgradeHelper.energyCost(upgrade, ENERGY_PER_BATCH);
		if (!hasWork) {
			blockEntity.getPersistentData().putDouble("progress", 0);
			setActive(level, pos, false);
		} else if (energy != null && energy.getEnergyStored() >= energyCost) {
			double progress = blockEntity.getPersistentData().getDouble("progress") + 1;
			blockEntity.getPersistentData().putDouble("progress", progress);
			setActive(level, pos, true);
			if (level instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, pos.getX() + 0.5, pos.getY() + 0.5,
						pos.getZ() + 0.5, 1, 0.25, 0, 0.25, 0);
			}
			if (progress >= cookTime) {
				for (int lane = 0; lane < INPUT_SLOTS.length; lane++) {
					if (results[lane].isEmpty()) {
						continue;
					}
					inventory.setStackInSlot(OUTPUT_SLOTS[lane], merged(inventory.getStackInSlot(OUTPUT_SLOTS[lane]), results[lane]));
					ItemStack input = inventory.getStackInSlot(INPUT_SLOTS[lane]).copy();
					input.shrink(1);
					inventory.setStackInSlot(INPUT_SLOTS[lane], input);
				}
				energy.extractEnergy(energyCost, false);
				blockEntity.getPersistentData().putDouble("progress", 0);
			}
		}

		blockEntity.setChanged();
		level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
		return energyText(world, pos);
	}

	private static ItemStack smeltingResult(Level level, ItemStack input) {
		if (input.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
				.map(recipe -> recipe.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
	}

	private static boolean fits(ItemStack current, ItemStack result) {
		return current.isEmpty() || ItemStack.isSameItemSameComponents(current, result)
				&& current.getCount() + result.getCount() <= current.getMaxStackSize();
	}

	private static ItemStack merged(ItemStack current, ItemStack result) {
		ItemStack merged = result.copy();
		merged.setCount(current.getCount() + result.getCount());
		return merged;
	}

	private static void checkMachineCore(LevelAccessor world, BlockPos pos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos core = pos.relative(direction);
			if (world.getBlockState(core).is(CrystalnexusModBlocks.MACHINE_CORE.get())) {
				MachineBlocksCheckerProcedure.execute(world, core.getX(), core.getY(), core.getZ());
				return;
			}
		}
	}

	private static void setActive(Level level, BlockPos pos, boolean active) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty property) {
			int value = active ? 2 : 1;
			if (property.getPossibleValues().contains(value) && state.getValue(property) != value) {
				level.setBlock(pos, state.setValue(property, value), 3);
			}
		}
	}

	private static boolean getBlockNBTLogic(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity != null && blockEntity.getPersistentData().getBoolean(tag);
	}

	private static IItemHandler itemHandler(LevelAccessor world, BlockPos pos) {
		return world instanceof ILevelExtension extension
				? extension.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) : null;
	}

	private static String energyText(LevelAccessor world, BlockPos pos) {
		IEnergyStorage energy = world instanceof ILevelExtension extension
				? extension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) : null;
		return new java.text.DecimalFormat("FE: ##.##").format(energy == null ? 0 : energy.getEnergyStored());
	}
}
