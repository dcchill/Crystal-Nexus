package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BucketItem;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class ReactorFluidInputOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockPos portPos = BlockPos.containing(x, y, z);
		if (Blocks.WATER == ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() instanceof BucketItem _bucket ? _bucket.content.defaultFluidState().createLegacyBlock() : Blocks.AIR.defaultBlockState()).getBlock()) {
			if (16 > itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount()) {
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					int _slotid = 0;
					ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
					_stk.shrink(1);
					_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
				}
				if (world instanceof ILevelExtension _ext) {
					IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
					if (_fluidHandler != null)
						_fluidHandler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(Items.BUCKET).copy();
					_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() + 1);
					_itemHandlerModifiable.setStackInSlot(1, _setstack);
				}
			}
		}
		CenteredMultiblockValidator.Link controller = CenteredMultiblockValidator.validateFromPort(world, portPos,
				CrystalnexusModBlocks.REACTOR_CORE.get(), CrystalnexusModBlocks.REACTOR_COMPUTER.get(), BlocksCheckerProcedure.CASING);
		if (controller == null || !(world instanceof ILevelExtension ext)) {
			return;
		}
		IFluidHandler source = ext.getCapability(Capabilities.FluidHandler.BLOCK, portPos, null);
		IFluidHandler destination = ext.getCapability(Capabilities.FluidHandler.BLOCK, controller.pos(), null);
		if (source == null || destination == null) {
			return;
		}
		FluidStack available = source.drain(100, IFluidHandler.FluidAction.SIMULATE);
		int accepted = destination.fill(available, IFluidHandler.FluidAction.SIMULATE);
		if (accepted > 0) {
			FluidStack drained = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
			destination.fill(drained, IFluidHandler.FluidAction.EXECUTE);
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
