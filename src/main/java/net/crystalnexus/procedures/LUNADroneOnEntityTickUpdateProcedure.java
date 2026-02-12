package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import net.crystalnexus.CrystalnexusMod;

public class LUNADroneOnEntityTickUpdateProcedure {
	public static boolean execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return false;
		boolean goHome = false;
		boolean goDest = false;
		double slotnumbercheck = 0;
		slotnumbercheck = 0;
		if (entity.getPersistentData().getBoolean("goDest")) {
			if (entity instanceof Mob _entity)
				_entity.getNavigation().moveTo((entity.getPersistentData().getDouble("destX")), (entity.getPersistentData().getDouble("destY") + 1), (entity.getPersistentData().getDouble("destZ")), 1);
			if (1.75 >= Math.abs(entity.getPersistentData().getDouble("destX") - entity.getX()) && 1.75 >= Math.abs(entity.getPersistentData().getDouble("destY") - entity.getY())
					&& 1.75 >= Math.abs(entity.getPersistentData().getDouble("destZ") - entity.getZ())) {
				for (int index0 = 0; index0 < 6; index0++) {
					if (!((itemFromBlockInventory(world, BlockPos.containing(entity.getPersistentData().getDouble("homeX"), entity.getPersistentData().getDouble("homeY"), entity.getPersistentData().getDouble("homeZ")), (int) slotnumbercheck).copy())
							.getItem() == Blocks.AIR.asItem())) {
						if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
							ItemStack _setstack = (itemFromBlockInventory(world, BlockPos.containing(entity.getPersistentData().getDouble("homeX"), entity.getPersistentData().getDouble("homeY"), entity.getPersistentData().getDouble("homeZ")),
									(int) slotnumbercheck).copy()).copy();
							_setstack.setCount(
									itemFromBlockInventory(world, BlockPos.containing(entity.getPersistentData().getDouble("homeX"), entity.getPersistentData().getDouble("homeY"), entity.getPersistentData().getDouble("homeZ")), (int) slotnumbercheck)
											.getCount());
							_modHandler.setStackInSlot(0, _setstack);
						}
						if (world instanceof ILevelExtension _ext
								&& _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(entity.getPersistentData().getDouble("homeX"), entity.getPersistentData().getDouble("homeY"), entity.getPersistentData().getDouble("homeZ")),
										null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
							int _slotid = (int) slotnumbercheck;
							ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
							_stk.shrink(
									itemFromBlockInventory(world, BlockPos.containing(entity.getPersistentData().getDouble("homeX"), entity.getPersistentData().getDouble("homeY"), entity.getPersistentData().getDouble("homeZ")), (int) slotnumbercheck)
											.getCount());
							_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
						}
						if (0 < (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler33 ? _modHandler33.getStackInSlot(0).copy() : ItemStack.EMPTY).getCount()) {
							return true;
						}
						break;
					} else {
						slotnumbercheck = 1 + slotnumbercheck;
					}
				}
				CrystalnexusMod.LOGGER.info("Dest");
				entity.getPersistentData().putBoolean("goDest", false);
				entity.getPersistentData().putBoolean("goHome", true);
			}
		}
		slotnumbercheck = 0;
		if (entity.getPersistentData().getBoolean("goHome")) {
			if (entity instanceof Mob _entity)
				_entity.getNavigation().moveTo((entity.getPersistentData().getDouble("homeX")), (entity.getPersistentData().getDouble("homeY") + 1), (entity.getPersistentData().getDouble("homeZ")), 1);
			if (1.75 >= Math.abs(entity.getPersistentData().getDouble("homeX") - entity.getX()) && 1.75 >= Math.abs(entity.getPersistentData().getDouble("homeY") - entity.getY())
					&& 1.75 >= Math.abs(entity.getPersistentData().getDouble("homeZ") - entity.getZ())) {
				for (int index1 = 0; index1 < 6; index1++) {
					if ((itemFromBlockInventory(world, BlockPos.containing(entity.getPersistentData().getDouble("destX"), entity.getPersistentData().getDouble("destY"), entity.getPersistentData().getDouble("destZ")), (int) slotnumbercheck).copy())
							.getItem() == Blocks.AIR.asItem()) {
						if (world instanceof ILevelExtension _ext
								&& _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(entity.getPersistentData().getDouble("destX"), entity.getPersistentData().getDouble("destY"), entity.getPersistentData().getDouble("destZ")),
										null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
							ItemStack _setstack = (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler58 ? _modHandler58.getStackInSlot(0).copy() : ItemStack.EMPTY).copy();
							_setstack.setCount((entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler56 ? _modHandler56.getStackInSlot(0).copy() : ItemStack.EMPTY).getCount());
							_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
						}
						if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
							ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
							_setstack.setCount(1);
							_modHandler.setStackInSlot(0, _setstack);
						}
						if (0 > (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler61 ? _modHandler61.getStackInSlot(0).copy() : ItemStack.EMPTY).getCount()) {
							return false;
						}
						break;
					} else {
						slotnumbercheck = 1 + slotnumbercheck;
					}
				}
				CrystalnexusMod.LOGGER.info("Home");
				entity.getPersistentData().putBoolean("goHome", false);
				entity.getPersistentData().putBoolean("goDest", true);
			}
		}
		return false;
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