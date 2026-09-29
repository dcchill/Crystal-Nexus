package net.crystalnexus.procedures;

import net.crystalnexus.util.CrushingRecipeSupport;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.config.CrystalnexusConfig;
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
import net.crystalnexus.block.entity.CrystalCrusherBlockEntity;

public class CrystalCrusherOnTickUpdateProcedure {
	private static final int ENERGY_PER_OPERATION = 4096;
	private static final int MAX_OUTPUT = 16;

	public static String execute(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		if (!(world.getBlockEntity(pos) instanceof CrystalCrusherBlockEntity crusher)) return "FE: 0";
		setMachineState(world, pos, net.crystalnexus.util.MachineAnimationHelper.shouldIdle(world, pos, getBlockNBTNumber(world, pos, "progress")) ? 1 : 2);

		ItemStack upgrade = crusher.getItem(2);
		MachineTier machineTier = MachineTier.from(world.getBlockState(pos));
		double baseCookTime = MachineUpgradeHelper.processingTime(upgrade, 100, 75, 50);
		double cookTime = machineTier.processingTime(MachineUpgradeHelper.cookTime(upgrade, baseCookTime));
		int energyCost = machineTier.energyCost(MachineUpgradeHelper.energyCost(upgrade, ENERGY_PER_OPERATION));
		setBlockNBTNumber(world, pos, "maxProgress", cookTime);

		if (!(world instanceof Level level))
			return energyText(world, pos);

		ItemStack input = crusher.getItem(0);
        var assigned = net.crystalnexus.assembly.AssemblyLineMachine.assignedRecipe(level, pos);
		ItemStack result = crusher.crushingResult(level, input, machineTier, assigned);
		int outputCount = Math.min(MAX_OUTPUT, result.getCount());
		ItemStack currentOutput = crusher.getItem(1);
		boolean outputFits = outputCount > 0
				&& (currentOutput.isEmpty() || ItemStack.isSameItemSameComponents(currentOutput, result))
				&& currentOutput.getCount() + outputCount <= result.getMaxStackSize();

		int requiredEnergy = assigned != null ? energyCost : Math.min(energyCost, CrystalnexusConfig.MACHINES.CRYSTAL_CRUSHER.maxExtract());
		if (result.isEmpty() || crusher.getEnergyStorage().getEnergyStored() < requiredEnergy || !outputFits)
			return energyText(world, pos);

		double progress = getBlockNBTNumber(world, pos, "progress") + 1;
		setBlockNBTNumber(world, pos, "progress", progress);
		if (world instanceof ServerLevel serverLevel)
			serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
					x + 0.5, y + 0.5, z + 0.5, 1, 0.25, 0, 0.25, 0);

		if (progress >= cookTime) {
			ItemStack produced = result.copy();
			produced.setCount(currentOutput.getCount() + outputCount);
			crusher.setItem(1, produced);
			ItemStack remainingInput = crusher.getItem(0).copy();
			remainingInput.shrink(1);
			crusher.setItem(0, remainingInput);
			setBlockNBTNumber(world, pos, "progress", 0);

			IEnergyStorage energy = crusher.getEnergyStorage();
            if (assigned != null) net.crystalnexus.assembly.AssemblyLineMachine.consumeAssignedEnergy(energy, energyCost);
            else energy.extractEnergy(energyCost, false);
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
		blockEntity.setChanged();
		BlockState state = world.getBlockState(pos);
		if (world instanceof Level level && (!"progress".equals(tag) || value == 0 || level.getGameTime() % 5 == 0))
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
