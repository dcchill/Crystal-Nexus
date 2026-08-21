package net.crystalnexus.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ResourceSingularityItem extends Item {
	public static final int ITEM_CAPACITY = 102_400;

	public ResourceSingularityItem() {
		this(ITEM_CAPACITY, Rarity.RARE);
	}

	public static ResourceSingularityItem withCapacity(int capacity) {
		return new ResourceSingularityItem(capacity, Rarity.RARE);
	}

	protected ResourceSingularityItem(int capacity, Rarity rarity) {
		super(new Item.Properties().durability(capacity).rarity(rarity));
	}
}
