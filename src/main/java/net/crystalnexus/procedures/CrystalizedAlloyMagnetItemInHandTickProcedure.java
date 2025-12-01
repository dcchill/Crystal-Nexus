package net.crystalnexus.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;

public class CrystalizedAlloyMagnetItemInHandTickProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double dx = 0;
		double dy = 0;
		double dz = 0;
		double dist = 0;
		{
			final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
			for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(13 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
				if (entityiterator instanceof ItemEntity) {
					dx = entity.getX() - entityiterator.getX();
					dy = (entity.getY() + 0.5) - entityiterator.getY();
					dz = entity.getZ() - entityiterator.getZ();
					dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
					entityiterator.setDeltaMovement(
							new Vec3((((dx / dist) * 0.5 + entityiterator.getDeltaMovement().x()) * 0.5), (((dy / dist) * 0.5 + entityiterator.getDeltaMovement().y()) * 0.5), (((dz / dist) * 0.5 + entityiterator.getDeltaMovement().z()) * 0.5)));
				}
			}
		}
	}
}