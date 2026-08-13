package net.crystalnexus.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TitaniumIngotItem extends Item {
	public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/titanium"));

	public TitaniumIngotItem() {
		super(new Item.Properties());
	}
}
