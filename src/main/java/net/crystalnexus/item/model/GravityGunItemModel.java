package net.crystalnexus.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import net.crystalnexus.item.GravityGunItem;

public class GravityGunItemModel extends GeoModel<GravityGunItem> {
	@Override
	public ResourceLocation getAnimationResource(GravityGunItem animatable) {
		return ResourceLocation.parse("crystalnexus:animations/gravity_gun.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(GravityGunItem animatable) {
		return ResourceLocation.parse("crystalnexus:geo/gravity_gun.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GravityGunItem animatable) {
		return ResourceLocation.parse("crystalnexus:textures/item/g_gun_texture.png");
	}
}