package net.crystalnexus.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;

import net.crystalnexus.init.CrystalnexusModFluids;

public class GasolineItem extends BucketItem {
	public GasolineItem() {
		super(CrystalnexusModFluids.GASOLINE.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)

		);
	}
}