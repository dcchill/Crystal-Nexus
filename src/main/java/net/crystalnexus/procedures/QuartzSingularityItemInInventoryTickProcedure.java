package net.crystalnexus.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;

import net.crystalnexus.init.CrystalnexusModParticleTypes;

import java.util.Comparator;

public class QuartzSingularityItemInInventoryTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double T = 0;
		double Zo = 0;
		double Yo = 0;
		double Za = 0;
		double Xo = 0;
		double Ya = 0;
		double Xa = 0;
		double damage = 0;
		damage = 5;
		{
			final Vec3 _center = new Vec3(x, y, z);
			for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(9 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
				if (!(entity == entityiterator) && !(entityiterator instanceof Player)) {
					if (entityiterator instanceof Monster) {
						entityiterator.hurt(new DamageSource(world.holderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("crystalnexus:crystal_beam")))), (float) damage);
						Xo = entity.getX() - entityiterator.getX();
						Yo = entity.getY() - entityiterator.getY();
						Zo = entity.getZ() - entityiterator.getZ();
						if (Math.floor(entity.getX()) <= Math.floor(entityiterator.getX())) {
							if (Math.floor(entity.getX()) == Math.floor(entityiterator.getX())) {
								if (Math.floor(entity.getY()) == Math.floor(entityiterator.getY())) {
									if (Math.floor(entity.getZ()) <= Math.floor(entityiterator.getZ())) {
										Za = Math.floor(entity.getZ()) + 0.2;
										while (Za <= Math.floor(entityiterator.getZ())) {
											T = (Za - entity.getZ()) / Zo;
											Ya = entity.getBbHeight() / 2 + entity.getY() + Yo * T;
											Xa = entity.getX() + Xo * T;
											if (world instanceof ServerLevel _level)
												_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), Xa, Ya, Za, 1, 0, 0, 0, 0);
											Za = 0.2 + Za;
										}
									} else {
										Za = Math.floor(entityiterator.getZ()) + 0.2;
										while (Za <= Math.floor(entity.getZ())) {
											T = (Za - entity.getZ()) / Zo;
											Ya = entity.getBbHeight() / 2 + entity.getY() + Yo * T;
											Xa = entity.getX() + Xo * T;
											if (world instanceof ServerLevel _level)
												_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), Xa, Ya, Za, 1, 0, 0, 0, 0);
											Za = 0.2 + Za;
										}
									}
								} else {
									if (Math.floor(entity.getY()) <= Math.floor(entityiterator.getY())) {
										Ya = Math.floor(entity.getY()) + 0.2;
										while (Ya <= Math.floor(entityiterator.getY())) {
											T = (Ya - entity.getY()) / Yo;
											Xa = entity.getX() + Xo * T;
											Za = entity.getZ() + Zo * T;
											if (world instanceof ServerLevel _level)
												_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), Xa, Ya, Za, 1, 0, 0, 0, 0);
											Ya = 0.2 + Ya;
										}
									} else {
										Ya = Math.floor(entityiterator.getY()) + 0.2;
										while (Ya <= Math.floor(entity.getY())) {
											T = (Ya - entity.getY()) / Yo;
											Xa = entity.getX() + Xo * T;
											Za = entity.getZ() + Zo * T;
											if (world instanceof ServerLevel _level)
												_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), Xa, Ya, Za, 1, 0, 0, 0, 0);
											Ya = 0.2 + Ya;
										}
									}
								}
							} else {
								Xa = Math.floor(entity.getX()) + 0.2;
								while (Xa <= Math.floor(entityiterator.getX())) {
									T = (Xa - entity.getX()) / Xo;
									Ya = entity.getBbHeight() / 2 + entity.getY() + Yo * T;
									Za = entity.getZ() + Zo * T;
									if (world instanceof ServerLevel _level)
										_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), Xa, Ya, Za, 1, 0, 0, 0, 0);
									Xa = 0.2 + Xa;
								}
							}
						} else {
							Xa = entityiterator.getX() + 0.2;
							while (Xa < Math.floor(entity.getX())) {
								T = (Xa - entity.getX()) / Xo;
								Ya = entity.getBbHeight() / 2 + entity.getY() + Yo * T;
								Za = entity.getZ() + Zo * T;
								if (world instanceof ServerLevel _level)
									_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), Xa, Ya, Za, 1, 0, 0, 0, 0);
								Xa = 0.2 + Xa;
							}
						}
					}
				}
			}
		}
	}
}