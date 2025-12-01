package net.crystalnexus.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;

import net.crystalnexus.procedures.CrystalizedAlloyMagnetItemInHandTickProcedure;

public class CrystalizedAlloyMagnetItem extends Item {
	public CrystalizedAlloyMagnetItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			CrystalizedAlloyMagnetItemInHandTickProcedure.execute(world, entity);
	}
}