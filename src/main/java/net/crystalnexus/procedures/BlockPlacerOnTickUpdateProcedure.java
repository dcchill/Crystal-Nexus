package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class BlockPlacerOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
		double yOffset = 0;
		double xOffset = 0;
		double T = 0;
		double zOffset = 0;
		double energy = 0;
		double outputAmount = 0;
		double cookTime = 0;
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
		if (getEnergyStored(world, BlockPos.containing(x, y, z), null) > 256) {
			if (Blocks.AIR == (world.getBlockState(BlockPos.containing(x + xOffset, y + yOffset, z + zOffset))).getBlock()) {
				if (!(Blocks.AIR == ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() instanceof BlockItem _bi ? _bi.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState()).getBlock())) {
					world.setBlock(BlockPos.containing(x + xOffset, y + yOffset, z + zOffset),
							((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() instanceof BlockItem _bi ? _bi.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState()), 3);
					if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						int _slotid = 0;
						ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
						_stk.shrink(1);
						_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
					}
					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy(256, false);
					}
				}
			}
		}
		return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, BlockPos.containing(x, y, z), null));
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

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}
}