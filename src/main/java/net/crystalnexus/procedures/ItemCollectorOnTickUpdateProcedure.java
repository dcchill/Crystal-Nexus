package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModItems;

import java.util.Comparator;

public class ItemCollectorOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		String registry_name_no_namespace = "";
		String registry_name_nugget = "";
		String registry_name = "";
		double dx = 0;
		double dy = 0;
		double dz = 0;
		double dist = 0;
		double outputAmount = 0;
		double cookTime = 0;
		double rangeCount = 0;
		if ((world instanceof Level _level0 && _level0.hasNeighborSignal(BlockPos.containing(x, y, z))) == false) {
			if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy()).getItem() == CrystalnexusModItems.RANGE_UPGRADE.get()) {
				rangeCount = 25;
			} else {
				rangeCount = 12;
			}
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(rangeCount / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator instanceof ItemEntity) {
						dx = (x + 0.5) - entityiterator.getX();
						dy = (y + 0.5) - entityiterator.getY();
						dz = (z + 0.5) - entityiterator.getZ();
						dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
						entityiterator.setDeltaMovement(new Vec3(((dx / dist) * 0.5 + entityiterator.getDeltaMovement().x()), ((dy / dist) * 0.5 + entityiterator.getDeltaMovement().y()), ((dz / dist) * 0.5 + entityiterator.getDeltaMovement().z())));
					}
				}
			}
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (!(entityiterator instanceof ItemEntity itemEntity)) continue;
					ItemStack incoming = itemEntity.getItem().copy();
					ItemStack filter = itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2);
					if (!filter.isEmpty() && !ItemStack.isSameItemSameComponents(filter, incoming)) continue;
					if (!(world instanceof ILevelExtension ext)
							|| !(ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable inventory)) continue;
					ItemStack current = inventory.getStackInSlot(0).copy();
					if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, incoming)) continue;
					int currentCount = current.getCount();
					int maxStackSize = current.isEmpty() ? incoming.getMaxStackSize() : current.getMaxStackSize();
					int moved = Math.min(incoming.getCount(), Math.max(0, maxStackSize - currentCount));
					if (moved <= 0) continue;
					ItemStack collected = current.isEmpty() ? incoming.copy() : current;
					collected.setCount(currentCount + moved);
					inventory.setStackInSlot(0, collected);
					incoming.shrink(moved);
					if (incoming.isEmpty()) itemEntity.discard();
					else itemEntity.setItem(incoming);
				}
			}
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
