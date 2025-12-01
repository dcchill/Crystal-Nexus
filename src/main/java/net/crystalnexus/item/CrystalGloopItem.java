package net.crystalnexus.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;

import net.crystalnexus.init.CrystalnexusModFluids;

public class CrystalGloopItem extends BucketItem {
	public CrystalGloopItem() {
		super(CrystalnexusModFluids.CRYSTAL_GLOOP.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)

		);
	}
}