package net.crystalnexus.procedures;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import net.crystalnexus.init.CrystalnexusModItems;

public class CrystalPurifierOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		if (world.isClientSide())
			return energyText(world, pos);

		IItemHandlerModifiable inventory = itemHandler(world, pos);
		if (inventory == null)
			return energyText(world, pos);

		int cookTime = cookTime(inventory.getStackInSlot(3));
		ItemStack input = inventory.getStackInSlot(0);
		ItemStack secondary = inventory.getStackInSlot(2);
		ItemStack result;
		int resultCount;
		int secondaryCost;
		int energyCost;

		if (input.is(CrystalnexusModItems.ANCIENT_CRYSTAL.get()) && secondary.is(Items.IRON_INGOT)) {
			result = new ItemStack(CrystalnexusModItems.CRYSTALIZED_ALLOY.get());
			resultCount = 2;
			secondaryCost = 1;
			energyCost = 512;
		} else if (input.is(Blocks.BONE_BLOCK.asItem())) {
			result = new ItemStack(CrystalnexusModItems.NITRILE.get());
			resultCount = 1;
			secondaryCost = 0;
			energyCost = 64;
		} else {
			reset(world, pos, cookTime);
			return energyText(world, pos);
		}

		ItemStack output = inventory.getStackInSlot(1);
		if ((!output.isEmpty() && !ItemStack.isSameItemSameComponents(output, result)) || output.getCount() + resultCount > result.getMaxStackSize()) {
			reset(world, pos, cookTime);
			return energyText(world, pos);
		}

		double progress = progress(world, pos) + 1;
		update(world, pos, progress, cookTime, 2);
		IEnergyStorage energy = energyStorage(world, pos, null);
		if (energy != null)
			energy.extractEnergy(energyCost, false);
		if (world instanceof ServerLevel level)
			level.sendParticles(ParticleTypes.DRAGON_BREATH, x + 0.5, y + 0.5, z + 0.5, 1, 0.25, 0, 0.25, 0);

		if (progress >= cookTime) {
			input.shrink(1);
			if (secondaryCost > 0)
				secondary.shrink(secondaryCost);
			result.setCount(output.getCount() + resultCount);
			inventory.setStackInSlot(0, input);
			inventory.setStackInSlot(1, result);
			inventory.setStackInSlot(2, secondary);
			update(world, pos, 0, cookTime, 1);
		}

		return energyText(world, pos);
	}

	private static int cookTime(ItemStack upgrade) {
		if (upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()))
			return 150;
		if (upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get()))
			return 200;
		return 300;
	}

	private static void reset(LevelAccessor world, BlockPos pos, int cookTime) {
		update(world, pos, 0, cookTime, 1);
	}

	private static void update(LevelAccessor world, BlockPos pos, double progress, int cookTime, int state) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null) {
			blockEntity.getPersistentData().putDouble("progress", progress);
			blockEntity.getPersistentData().putDouble("maxProgress", cookTime);
			blockEntity.setChanged();
		}
		BlockState blockState = world.getBlockState(pos);
		if (blockState.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty property && property.getPossibleValues().contains(state))
			world.setBlock(pos, blockState.setValue(property, state), 3);
		if (world instanceof Level level)
			level.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
	}

	private static double progress(LevelAccessor world, BlockPos pos) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity == null ? 0 : blockEntity.getPersistentData().getDouble("progress");
	}

	private static IItemHandlerModifiable itemHandler(LevelAccessor world, BlockPos pos) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler handler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (handler instanceof IItemHandlerModifiable modifiable)
				return modifiable;
		}
		return null;
	}

	private static IEnergyStorage energyStorage(LevelAccessor world, BlockPos pos, Direction direction) {
		return world instanceof ILevelExtension ext ? ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction) : null;
	}

	private static String energyText(LevelAccessor world, BlockPos pos) {
		IEnergyStorage energy = energyStorage(world, pos, null);
		return new java.text.DecimalFormat("FE: ##.##").format(energy == null ? 0 : energy.getEnergyStored());
	}
}
