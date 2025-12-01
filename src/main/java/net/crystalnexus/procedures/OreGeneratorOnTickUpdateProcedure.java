package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModItems;

public class OreGeneratorOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
		String registry_name_no_namespace = "";
		String registry_name_nugget = "";
		String registry_name = "";
		double outputAmount = 0;
		double cookTime = 0;
		double xOffset = 0;
		double yOffset = 0;
		double zOffset = 0;
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.ACCELERATION_UPGRADE.get()) {
			cookTime = 5;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) {
			cookTime = 1;
		} else {
			cookTime = 10;
		}
		if (("up").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = 1;
			zOffset = 0;
		}
		if (("down").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = -1;
			zOffset = 0;
		}
		if (("north").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = 0;
			zOffset = -1;
		}
		if (("south").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = 0;
			zOffset = 1;
		}
		if (("east").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 1;
			yOffset = 0;
			zOffset = 0;
		}
		if (("west").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = -1;
			yOffset = 0;
			zOffset = 0;
		}
		if (1 == Mth.nextInt(RandomSource.create(), 1, (int) cookTime)) {
			if ((world.getBlockState(BlockPos.containing(x + xOffset, y + yOffset, z + zOffset))).getBlock() == Blocks.AIR) {
				if (512 <= getEnergyStored(world, BlockPos.containing(x, y, z), null)) {
					if (250 <= getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null)) {
						world.setBlock(BlockPos.containing(x + xOffset, y + yOffset, z + zOffset),
								(BuiltInRegistries.BLOCK.getOrCreateTag(BlockTags.create(ResourceLocation.parse("c:ores"))).getRandomElement(RandomSource.create()).orElseGet(() -> BuiltInRegistries.BLOCK.wrapAsHolder(Blocks.AIR)).value())
										.defaultBlockState(),
								3);
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy(512, false);
						}
						if (world instanceof ILevelExtension _ext) {
							IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
							if (_fluidHandler != null)
								_fluidHandler.drain(250, IFluidHandler.FluidAction.EXECUTE);
						}
					} else if (250 > getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null)) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("running", 0);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
				} else if (512 > getEnergyStored(world, BlockPos.containing(x, y, z), null)) {
					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x, y, z);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putDouble("running", 0);
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
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

	private static String getBlockNBTString(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getString(tag);
		return "";
	}

	public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.getEnergyStored();
		}
		return 0;
	}

	private static int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.getFluidInTank(tank).getAmount();
		}
		return 0;
	}
}