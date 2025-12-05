package net.crystalnexus.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;

import net.crystalnexus.procedures.LaserPistolRightclickedProcedure;
import net.crystalnexus.init.CrystalnexusModItems;

public class MiningLaserItem extends ShieldItem {
	public MiningLaserItem() {
		super(new Item.Properties().durability(2048));
	}

	@Override
	public boolean isValidRepairItem(ItemStack itemstack, ItemStack repairitem) {
		return Ingredient.of(new ItemStack(CrystalnexusModItems.ULTIMATE_CRYSTAL.get()), new ItemStack(CrystalnexusModItems.BLUTONIUM_CRYSTAL.get())).test(repairitem);
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			LaserPistolRightclickedProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity, itemstack);
	}
}