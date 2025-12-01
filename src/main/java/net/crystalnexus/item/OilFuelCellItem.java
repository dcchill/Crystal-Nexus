package net.crystalnexus.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

import net.crystalnexus.init.CrystalnexusModItems;

public class OilFuelCellItem extends Item {
	public OilFuelCellItem() {
		super(new Item.Properties());
	}

	@Override
	public boolean hasCraftingRemainingItem(ItemStack stack) {
		return true;
	}

	@Override
	public ItemStack getCraftingRemainingItem(ItemStack itemstack) {
		return new ItemStack(CrystalnexusModItems.EMPTY_FUEL_CELL.get());
	}
}