package net.crystalnexus.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModEntities;
import net.crystalnexus.entity.LaserBeamEntity;

public class LaserPistolRightclickedProcedure {
	private static final int FE_PER_SHOT = 512;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;

		double cooldown = 0;
		double damage = 0;

		if (!entity.isShiftKeyDown()) {
			damage = 5;
			cooldown = 10;

			if (CrystalnexusModItems.MINING_LASER.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
				if ((entity instanceof Player _plrCldRem4 ? _plrCldRem4.getCooldowns().getCooldownPercent(itemstack.getItem(), 0f) * 100 : 0) == 0) {
					if (entity instanceof LivingEntity _livEnt5 && _livEnt5.isBlocking()) {

						// --- FE GATE + DRAIN (survival) ---
						boolean creative = (entity instanceof Player _plr ? _plr.getAbilities().instabuild : false);
						if (!creative) {
							if (!(entity instanceof Player p))
								return;

							// Only allow firing if we can extract 512 FE from batteries in inventory
							if (!canExtractFromInventoryBatteries(p, FE_PER_SHOT))
								return;
						}

						// Spawn + consume only on server
						Entity _shootFrom = entity;
						Level projectileLevel = _shootFrom.level();
						if (!projectileLevel.isClientSide()) {

							// Actually drain FE now (only if not creative)
							if (!creative && _shootFrom instanceof Player p) {
								// If for some reason it fails now, abort
								if (!extractFromInventoryBatteries(p, FE_PER_SHOT))
									return;
							}

							Projectile _entityToSpawn = initArrowProjectile(
									new LaserBeamEntity(CrystalnexusModEntities.LASER_BEAM.get(), 0, 0, 0, projectileLevel, createArrowWeaponItemStack(projectileLevel, 0, (byte) 255)),
									entity, (float) damage, true, false, false, AbstractArrow.Pickup.DISALLOWED
							);
							_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
							_entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 12, 0);
							projectileLevel.addFreshEntity(_entityToSpawn);

							if (entity instanceof Player _player)
								_player.getCooldowns().addCooldown(itemstack.getItem(), (int) cooldown);

							if (!creative) {
								if (world instanceof ServerLevel _level) {
									(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).hurtAndBreak(1, _level, null, _stkprov -> {});
								}
							}
						}

						// Sound (client + server safe)
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y, z),
										BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.warden.sonic_boom")),
										SoundSource.PLAYERS, 0.4f, 2f);
							} else {
								_level.playLocalSound(x, y, z,
										BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.warden.sonic_boom")),
										SoundSource.PLAYERS, 0.4f, 2f, false);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Returns true if the player's inventory contains batteries/items that can
	 * collectively extract at least `feNeeded` FE (simulation).
	 */
	private static boolean canExtractFromInventoryBatteries(Player player, int feNeeded) {
		int remaining = feNeeded;

		for (ItemStack stack : player.getInventory().items) {
			if (stack.isEmpty())
				continue;

			IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
			if (energy == null)
				continue;

			int canTake = energy.extractEnergy(remaining, true); // simulate
			if (canTake > 0) {
				remaining -= canTake;
				if (remaining <= 0)
					return true;
			}
		}

		// (Optional) also check offhand slot as a "battery" if you want:
		// ItemStack offhand = player.getOffhandItem(); ...

		return false;
	}

	/**
	 * Actually extracts `feNeeded` FE from batteries/items in the player's inventory.
	 * Returns true if it successfully drained the full amount.
	 */
	private static boolean extractFromInventoryBatteries(Player player, int feNeeded) {
		int remaining = feNeeded;

		for (ItemStack stack : player.getInventory().items) {
			if (stack.isEmpty())
				continue;

			IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
			if (energy == null)
				continue;

			int taken = energy.extractEnergy(remaining, false); // real drain
			if (taken > 0) {
				remaining -= taken;
				if (remaining <= 0)
					return true;
			}
		}

		return false;
	}

	private static AbstractArrow initArrowProjectile(AbstractArrow entityToSpawn, Entity shooter, float damage, boolean silent, boolean fire, boolean particles, AbstractArrow.Pickup pickup) {
		entityToSpawn.setOwner(shooter);
		entityToSpawn.setBaseDamage(damage);
		if (silent)
			entityToSpawn.setSilent(true);
		if (fire)
			entityToSpawn.igniteForSeconds(100);
		if (particles)
			entityToSpawn.setCritArrow(true);
		entityToSpawn.pickup = pickup;
		return entityToSpawn;
	}

	private static ItemStack createArrowWeaponItemStack(Level level, int knockback, byte piercing) {
		ItemStack weapon = new ItemStack(Items.ARROW);
		if (knockback > 0)
			weapon.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK), knockback);
		if (piercing > 0)
			weapon.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PIERCING), piercing);
		return weapon;
	}
}
