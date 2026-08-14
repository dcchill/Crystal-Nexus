package net.crystalnexus.procedures;

import net.crystalnexus.util.CrushingRecipeSupport;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.processing.MachineTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
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

import java.text.DecimalFormat;

public class CrystalCrusherOnTickUpdateProcedure {
	private static final int ENERGY_PER_OPERATION = 4096;
	private static final int MAX_OUTPUT = 16;

	public static String execute(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		setMachineState(world, pos, getBlockNBTNumber(world, pos, "progress") == 0 ? 1 : 2);

		ItemStack upgrade = itemFromBlockInventory(world, pos, 2);
		MachineTier machineTier = MachineTier.from(world.getBlockState(pos));
		double baseCookTime = upgrade.is(net.crystalnexus.init.CrystalnexusModItems.ACCELERATION_UPGRADE.get()) ? 75
				: upgrade.is(net.crystalnexus.init.CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) ? 50 : 100;
		double cookTime = machineTier.processingTime(MachineUpgradeHelper.cookTime(upgrade, baseCookTime));
		int energyCost = machineTier.energyCost(MachineUpgradeHelper.energyCost(upgrade, ENERGY_PER_OPERATION));
		setBlockNBTNumber(world, pos, "maxProgress", cookTime);

		if (!(world instanceof Level level))
			return energyText(world, pos);

		ItemStack input = itemFromBlockInventory(world, pos, 0);
		ItemStack result = CrushingRecipeSupport.findResult(level, input, machineTier);
		int outputCount = Math.min(MAX_OUTPUT, result.getCount());
		ItemStack currentOutput = itemFromBlockInventory(world, pos, 1);
		boolean outputFits = outputCount > 0
				&& (currentOutput.isEmpty() || ItemStack.isSameItemSameComponents(currentOutput, result))
				&& currentOutput.getCount() + outputCount <= result.getMaxStackSize();

		if (result.isEmpty() || getEnergyStored(world, pos, null) < energyCost || !outputFits)
			return energyText(world, pos);

		double progress = getBlockNBTNumber(world, pos, "progress") + 1;
		setBlockNBTNumber(world, pos, "progress", progress);
		if (world instanceof ServerLevel serverLevel)
			serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
					x + 0.5, y + 0.5, z + 0.5, 1, 0.25, 0, 0.25, 0);

		if (progress >= cookTime && world instanceof ILevelExtension extension
				&& extension.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable inventory) {
			ItemStack produced = result.copy();
			produced.setCount(currentOutput.getCount() + outputCount);
			inventory.setStackInSlot(1, produced);
			ItemStack remainingInput = inventory.getStackInSlot(0).copy();
			remainingInput.shrink(1);
			inventory.setStackInSlot(0, remainingInput);
			setBlockNBTNumber(world, pos, "progress", 0);

			IEnergyStorage energy = extension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
			if (energy != null)
				energy.extractEnergy(energyCost, false);
		}

		return energyText(world, pos);
	}

	private static void setMachineState(LevelAccessor world, BlockPos pos, int value) {
		BlockState state = world.getBlockState(pos);
		if (state.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty property
				&& property.getPossibleValues().contains(value) && state.getValue(property) != value)
			world.setBlock(pos, state.setValue(property, value), 3);
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity == null ? -1 : blockEntity.getPersistentData().getDouble(tag);
	}

	private static void setBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag, double value) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null || blockEntity.getPersistentData().getDouble(tag) == value)
			return;
		blockEntity.getPersistentData().putDouble(tag, value);
		BlockState state = world.getBlockState(pos);
		if (world instanceof Level level)
			level.sendBlockUpdated(pos, state, state, 3);
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension extension) {
			IItemHandler inventory = extension.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (inventory != null)
				return inventory.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}

	private static String energyText(LevelAccessor world, BlockPos pos) {
		return new DecimalFormat("FE: ##.##").format(getEnergyStored(world, pos, null));
	}

	public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension extension) {
			IEnergyStorage energy = extension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energy != null)
				return energy.getEnergyStored();
		}
		return 0;
	}
}
