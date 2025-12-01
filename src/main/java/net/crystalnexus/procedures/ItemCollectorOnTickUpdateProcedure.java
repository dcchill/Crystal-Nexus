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
					if (entityiterator instanceof ItemEntity) {
						if ((entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy()).getItem()) {
							if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == Blocks.AIR.asItem()
									|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == (entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem()) {
								if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() + (entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getCount() <= 64) {
									if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
										ItemStack _setstack = (entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).copy();
										_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() + (entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getCount());
										_itemHandlerModifiable.setStackInSlot(0, _setstack);
									}
									if (!entityiterator.level().isClientSide())
										entityiterator.discard();
								}
							}
						} else if (Blocks.AIR.asItem() == (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).copy()).getItem()) {
							if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == Blocks.AIR.asItem()
									|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == (entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem()) {
								if (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() + (entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getCount() <= 64) {
									if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
										ItemStack _setstack = (entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).copy();
										_setstack.setCount(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).getCount() + (entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getCount());
										_itemHandlerModifiable.setStackInSlot(0, _setstack);
									}
									if (!entityiterator.level().isClientSide())
										entityiterator.discard();
								}
							}
						}
					}
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