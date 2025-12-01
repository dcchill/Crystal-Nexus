package net.crystalnexus.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import net.crystalnexus.init.CrystalnexusModItems;

public class FlamethrowerRightclickedOnBlockProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		double particleRadius = 0;
		double particleAmount = 0;
		if (CrystalnexusModItems.FLAMETHROWER.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
			if (itemstack.getDamageValue() < 2047) {
				if (entity instanceof LivingEntity _livEnt4 && _livEnt4.isBlocking()) {
					for (int index0 = 0; index0 < 5; index0++) {
						world.addParticle(ParticleTypes.FLAME, (x + entity.getLookAngle().x), (y + 1.4 + entity.getLookAngle().y), (z + entity.getLookAngle().z), (entity.getLookAngle().x * 4 + Mth.nextDouble(RandomSource.create(), -1, 1)),
								(entity.getLookAngle().y * 4 + Mth.nextDouble(RandomSource.create(), -1, 1)), (entity.getLookAngle().z * 4 + Mth.nextDouble(RandomSource.create(), -1, 1)));
					}
					for (int index1 = 0; index1 < 3; index1++) {
						if (world instanceof ServerLevel projectileLevel) {
							Projectile _entityToSpawn = initProjectileProperties(new SmallFireball(EntityType.SMALL_FIREBALL, projectileLevel), entity, new Vec3((entity.getLookAngle().x), (entity.getLookAngle().y), (entity.getLookAngle().z)));
							_entityToSpawn.setPos((x + entity.getLookAngle().x), (y + 1.4 + entity.getLookAngle().y), (z + entity.getLookAngle().z));
							_entityToSpawn.shoot((entity.getLookAngle().x + Mth.nextDouble(RandomSource.create(), -0.001, 0.001)), (entity.getLookAngle().y + Mth.nextDouble(RandomSource.create(), -0.001, 0.001)),
									(entity.getLookAngle().z + Mth.nextDouble(RandomSource.create(), -0.001, 0.001)), 3, 1);
							projectileLevel.addFreshEntity(_entityToSpawn);
						}
					}
					if (!(entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
						if (world instanceof ServerLevel _level) {
							itemstack.hurtAndBreak(1, _level, null, _stkprov -> {
							});
						}
					}
				}
			}
		}
	}

	private static Projectile initProjectileProperties(Projectile entityToSpawn, Entity shooter, Vec3 acceleration) {
		entityToSpawn.setOwner(shooter);
		if (!Vec3.ZERO.equals(acceleration)) {
			entityToSpawn.setDeltaMovement(acceleration);
			entityToSpawn.hasImpulse = true;
		}
		return entityToSpawn;
	}
}