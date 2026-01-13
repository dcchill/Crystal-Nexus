package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class ItemElevatorOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		// Only mutate inventories on the server
		if (world instanceof Level lvl && lvl.isClientSide) return;

		BlockPos pos = BlockPos.containing(x, y, z);
		BlockPos upPos = pos.above();
		BlockPos downPos = pos.below();
		ItemStack here = itemFromBlockInventory(world, pos, 0).copy();
		if (!here.isEmpty()
				&& world.getBlockState(pos).getBlock() == world.getBlockState(upPos).getBlock()) {

			ItemStack above = itemFromBlockInventory(world, upPos, 0).copy();

			boolean canMerge = above.isEmpty() || ItemStack.isSameItemSameComponents(above, here);
			int aboveCount = above.isEmpty() ? 0 : above.getCount();
			int max = here.getMaxStackSize();

			if (canMerge && aboveCount < max) {
				if (world instanceof ILevelExtension ext
						&& ext.getCapability(Capabilities.ItemHandler.BLOCK, upPos, null) instanceof IItemHandlerModifiable upInv
						&& ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable inv) {

					// Insert 1 into above
					ItemStack newAbove = above.isEmpty() ? here.copy() : above.copy();
					newAbove.setCount(aboveCount + 1);
					upInv.setStackInSlot(0, newAbove);

					// Remove 1 from here
					ItemStack newHere = inv.getStackInSlot(0).copy();
					newHere.shrink(1);
					inv.setStackInSlot(0, newHere);
				}
			}
		}

		if (world.getBlockState(pos).getBlock() == CrystalnexusModBlocks.ITEM_ELEVATOR_DOWN.get()
				&& world.getBlockState(downPos).getBlock() == CrystalnexusModBlocks.ITEM_ELEVATOR_DOWN.get()) {

			ItemStack here2 = itemFromBlockInventory(world, pos, 0).copy();
			if (!here2.isEmpty()) {
				ItemStack below = itemFromBlockInventory(world, downPos, 0).copy();

				boolean canMerge = below.isEmpty() || ItemStack.isSameItemSameComponents(below, here2);
				int belowCount = below.isEmpty() ? 0 : below.getCount();
				int max = here2.getMaxStackSize();

				if (canMerge && belowCount < max) {
					if (world instanceof ILevelExtension ext
							&& ext.getCapability(Capabilities.ItemHandler.BLOCK, downPos, null) instanceof IItemHandlerModifiable downInv
							&& ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable inv) {

						// Insert 1 into below
						ItemStack newBelow = below.isEmpty() ? here2.copy() : below.copy();
						newBelow.setCount(belowCount + 1);
						downInv.setStackInSlot(0, newBelow);

						// Remove 1 from here
						ItemStack newHere = inv.getStackInSlot(0).copy();
						newHere.shrink(1);
						inv.setStackInSlot(0, newHere);
					}
				}
			}
		}

		if (world.getBlockState(pos).getBlock() == CrystalnexusModBlocks.ITEM_ELEVATOR.get()
				&& world.getBlockState(upPos).getBlock() == Blocks.AIR) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.DUST_PLUME, (x + 0.5), (y + 0.6), (z + 0.5), 1, 0, 1, 0, 0.05);
		}

		if (world.getBlockState(pos).getBlock() == CrystalnexusModBlocks.ITEM_ELEVATOR_DOWN.get()
				&& world.getBlockState(downPos).getBlock() == Blocks.AIR) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.DUST_PLUME, (x + 0.5), (y - 0.6), (z + 0.5), 1, 0, 1, 0, 0.05);
		}
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null && slot >= 0 && slot < itemHandler.getSlots())
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}
}
