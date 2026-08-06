package net.crystalnexus.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SiliconWaferItem extends Item {
	public SiliconWaferItem() {
		super(new Item.Properties().setNoRepair().rarity(net.minecraft.world.item.Rarity.UNCOMMON));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
