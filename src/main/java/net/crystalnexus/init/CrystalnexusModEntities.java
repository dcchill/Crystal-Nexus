/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.Registries;

import net.crystalnexus.entity.PaintballEntity;
import net.crystalnexus.entity.LaserBeamEntity;
import net.crystalnexus.CrystalnexusMod;

public class CrystalnexusModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, CrystalnexusMod.MODID);
	public static final DeferredHolder<EntityType<?>, EntityType<PaintballEntity>> PAINTBALL = register("paintball",
			EntityType.Builder.<PaintballEntity>of(PaintballEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<LaserBeamEntity>> LASER_BEAM = register("laser_beam",
			EntityType.Builder.<LaserBeamEntity>of(LaserBeamEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));

	// Start of user code block custom entities
	// End of user code block custom entities
	private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}
}