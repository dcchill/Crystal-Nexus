package net.crystalnexus.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;

import net.crystalnexus.init.CrystalnexusModFluids;

public class CrudeOilItem extends BucketItem {
	public CrudeOilItem() {
		super(CrystalnexusModFluids.CRUDE_OIL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)

		);
	}
}