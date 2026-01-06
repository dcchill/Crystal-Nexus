package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class ItemElevatorDownOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (!((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == Blocks.AIR.asItem())
				&& (world.getBlockState(BlockPos.containing(x, y, z))).getBlock() == (world.getBlockState(BlockPos.containing(x, y - 1, z))).getBlock()
				&& ((itemFromBlockInventory(world, BlockPos.containing(x, y - 1, z), 0).copy()).getItem() == Blocks.AIR.asItem()
						|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == (itemFromBlockInventory(world, BlockPos.containing(x, y - 1, z), 0).copy()).getItem())) {
			if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y - 1, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
				ItemStack _setstack = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).copy();
				_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() + 1);
				_itemHandlerModifiable.setStackInSlot(0, _setstack);
			}
			if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
				int _slotid = 0;
				ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
				_stk.shrink(1);
				_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
			}
		}
		if ((world.getBlockState(BlockPos.containing(x, y, z))).getBlock() == CrystalnexusModBlocks.ITEM_ELEVATOR.get() && (world.getBlockState(BlockPos.containing(x, y + 1, z))).getBlock() == Blocks.AIR) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.DUST_PLUME, (x + 0.5), (y + 0.6), (z + 0.5), 1, 0, 0.5, 0, 0.05);
		}
		if ((world.getBlockState(BlockPos.containing(x, y, z))).getBlock() == CrystalnexusModBlocks.ITEM_ELEVATOR_DOWN.get() && (world.getBlockState(BlockPos.containing(x, y - 1, z))).getBlock() == Blocks.AIR) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.DUST_PLUME, (x + 0.5), (y - 0.6), (z + 0.5), 1, 0, 0.5, 0, (-0.05));
		}
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