package net.crystalnexus.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;

import net.crystalnexus.procedures.BlutoniumIngotItemInInventoryTickProcedure;

public class BlutoniumWasteItem extends Item {
	public BlutoniumWasteItem() {
		super(new Item.Properties().fireResistant());
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		BlutoniumIngotItemInInventoryTickProcedure.execute(world, entity);
	}
}