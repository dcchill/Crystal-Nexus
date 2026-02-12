package net.crystalnexus.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import net.crystalnexus.item.GeigerCounterItem;

public class GeigerCounterItemModel extends GeoModel<GeigerCounterItem> {
	@Override
	public ResourceLocation getAnimationResource(GeigerCounterItem animatable) {
		return ResourceLocation.parse("crystalnexus:animations/geiger_counter.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(GeigerCounterItem animatable) {
		return ResourceLocation.parse("crystalnexus:geo/geiger_counter.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GeigerCounterItem animatable) {
		return ResourceLocation.parse("crystalnexus:textures/item/geiger_texture.png");
	}
}