package net.crystalnexus.events;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.network.payload.S2C_BlackHoleVisual;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public class DarkMatterBlackHoleEvents {
	private static final int BASE_DURATION_TICKS = 1200;
	private static final int MAX_DURATION_BONUS_TICKS = 180;
	private static final double BASE_RADIUS = 64.0D;
	private static final double MAX_RADIUS_BONUS = 10.0D;
	private static final double CENTER_CONSUME_DISTANCE = 2.75D;
	private static final int BLOCK_PULL_ATTEMPTS_PER_TICK = 40;
	private static final float FALLING_BLOCK_SPAWN_CHANCE = 1.00F;
	private static final int MIN_BLOCK_DECAY_STEPS_PER_TICK = 900;
	private static final double FULL_BREAK_RADIUS = CENTER_CONSUME_DISTANCE * 2.0D;
	private static final double EDGE_BREAK_CHANCE = 0.06D;
	private static final List<BlackHole> BLACK_HOLES = new ArrayList<>();

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (!(event.getEntity() instanceof ItemEntity) || !(event.getLevel() instanceof ServerLevel level)) {
			return;
		}

		if (isInsideActiveBlackHole(level, event.getEntity().position())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (BLACK_HOLES.isEmpty()) {
			return;
		}

		Iterator<BlackHole> iterator = BLACK_HOLES.iterator();
		while (iterator.hasNext()) {
			BlackHole blackHole = iterator.next();
			ServerLevel level = event.getServer().getLevel(blackHole.dimension());
			if (level == null || blackHole.age() >= blackHole.duration()) {
				iterator.remove();
				continue;
			}

			blackHole.tick();
			tickBlackHole(level, blackHole);
		}
	}

	public static void spawnBlackHole(ServerLevel level, Vec3 center) {
		spawnBlackHole(level, center, 8);
	}

	private static void spawnBlackHole(ServerLevel level, Vec3 center, int stackSize) {
		int duration = BASE_DURATION_TICKS + Math.min(MAX_DURATION_BONUS_TICKS, stackSize * 12);
		double radius = BASE_RADIUS + Math.min(MAX_RADIUS_BONUS, stackSize * 0.75D);
		BLACK_HOLES.add(new BlackHole(level.dimension(), center, radius, duration));

		PacketDistributor.sendToPlayersInDimension(level, new S2C_BlackHoleVisual(center.x, center.y, center.z, radius, duration));
		level.playSound(null, BlockPos.containing(center), SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 3.0F, 0.45F);
		level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
	}

	private static void tickBlackHole(ServerLevel level, BlackHole blackHole) {
		Vec3 center = blackHole.center();
		double radius = blackHole.radius();

		pullEntities(level, center, radius);
		pullBlocks(level, blackHole);
		eatBlocks(level, blackHole);
	}

	private static void pullEntities(ServerLevel level, Vec3 center, double radius) {
		AABB area = new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
		for (Entity entity : level.getEntitiesOfClass(Entity.class, area, entity -> !entity.isRemoved())) {
			if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
				continue;
			}

			Vec3 target = center.add(0.0D, 0.35D, 0.0D);
			Vec3 offset = target.subtract(entity.position());
			double distance = Math.max(0.25D, offset.length());
			if (distance > radius) {
				continue;
			}

			if (distance < CENTER_CONSUME_DISTANCE) {
				if (entity instanceof FallingBlockEntity fallingBlock) {
					consumeFallingBlock(level, fallingBlock);
				} else if (entity instanceof ItemEntity) {
					entity.discard();
				} else {
					entity.hurt(level.damageSources().magic(), 8.0F);
				}
				continue;
			}

			double pull = 0.17D + (1.0D - distance / radius) * 0.34D;
			Vec3 velocity = offset.normalize().scale(pull);
			entity.setDeltaMovement(entity.getDeltaMovement().scale(0.55D).add(velocity));
			entity.hurtMarked = true;
			entity.fallDistance = 0.0F;

			if (entity instanceof LivingEntity && entity.tickCount % 20 == 0 && distance < radius * 0.72D) {
				entity.hurt(level.damageSources().magic(), 2.0F);
			}
		}
	}

	private static void pullBlocks(ServerLevel level, BlackHole blackHole) {
		Vec3 center = blackHole.center();
		for (int i = 0; i < BLOCK_PULL_ATTEMPTS_PER_TICK; i++) {
			BlockPos pos = blackHole.nextPullBlockPos();
			if (pos == null) {
				return;
			}

			if (!level.isLoaded(pos)) {
				continue;
			}

			BlockState state = level.getBlockState(pos);
			if (!canEatBlock(level, pos, state) || state.hasBlockEntity() || level.random.nextFloat() >= FALLING_BLOCK_SPAWN_CHANCE) {
				continue;
			}

			FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level, pos, state);
			fallingBlock.dropItem = false;
			fallingBlock.disableDrop();
			fallingBlock.setNoGravity(true);
			fallingBlock.setDeltaMovement(center.add(0.0D, 0.35D, 0.0D).subtract(fallingBlock.position()).normalize().scale(0.45D));
			fallingBlock.hurtMarked = true;
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 3, 0.18D, 0.18D, 0.18D, 0.04D);
		}
	}

	private static void eatBlocks(ServerLevel level, BlackHole blackHole) {
		Vec3 center = blackHole.center();
		double radius = blackHole.radius();
		int steps = Math.max(MIN_BLOCK_DECAY_STEPS_PER_TICK, (int) Math.round(radius * 12.0D));
		for (int i = 0; i < steps; i++) {
			BlockPos pos = blackHole.nextDecayBlockPos();
			if (pos == null) {
				return;
			}

			if (!level.isLoaded(pos)) {
				continue;
			}

			BlockState state = level.getBlockState(pos);
			if (!canEatBlock(level, pos, state)) {
				continue;
			}

			double distance = Math.sqrt(pos.distToCenterSqr(center.x, center.y, center.z));
			double eatChance = blockBreakChance(distance, radius);
			if (eatChance < 1.0D && level.random.nextDouble() > eatChance) {
				continue;
			}

			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 4, 0.18D, 0.18D, 0.18D, 0.06D);
			level.removeBlock(pos, false);
		}
	}

	private static void consumeFallingBlock(ServerLevel level, FallingBlockEntity fallingBlock) {
		BlockState state = fallingBlock.getBlockState();
		if (!state.isAir()) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), fallingBlock.getX(), fallingBlock.getY() + 0.5D, fallingBlock.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.1D);
		}
		fallingBlock.discard();
	}

	private static boolean canEatBlock(ServerLevel level, BlockPos pos, BlockState state) {
		return !state.isAir() && state.getDestroySpeed(level, pos) >= 0.0F;
	}

	private static double blockBreakChance(double distance, double radius) {
		if (distance <= FULL_BREAK_RADIUS) {
			return 1.0D;
		}

		double falloffRange = Math.max(1.0D, radius - FULL_BREAK_RADIUS);
		double fromCoreToEdge = Math.min(1.0D, Math.max(0.0D, (distance - FULL_BREAK_RADIUS) / falloffRange));
		double eased = fromCoreToEdge * fromCoreToEdge;
		return EDGE_BREAK_CHANCE + (1.0D - EDGE_BREAK_CHANCE) * (1.0D - eased);
	}

	private static boolean isInsideActiveBlackHole(ServerLevel level, Vec3 pos) {
		for (BlackHole blackHole : BLACK_HOLES) {
			if (blackHole.dimension() == level.dimension() && blackHole.center().distanceToSqr(pos) <= blackHole.radius() * blackHole.radius()) {
				return true;
			}
		}
		return false;
	}

	private static class BlackHole {
		private final ResourceKey<Level> dimension;
		private final Vec3 center;
		private final double radius;
		private final int duration;
		private final ShellScanner pullScanner = new ShellScanner();
		private final ShellScanner decayScanner = new ShellScanner();
		private int age;

		private BlackHole(ResourceKey<Level> dimension, Vec3 center, double radius, int duration) {
			this.dimension = dimension;
			this.center = center;
			this.radius = radius;
			this.duration = duration;
		}

		private ResourceKey<Level> dimension() {
			return dimension;
		}

		private Vec3 center() {
			return center;
		}

		private double radius() {
			return radius;
		}

		private int duration() {
			return duration;
		}

		private int age() {
			return age;
		}

		private void tick() {
			age++;
		}

		private BlockPos nextPullBlockPos() {
			return pullScanner.next(center, radius);
		}

		private BlockPos nextDecayBlockPos() {
			return decayScanner.next(center, radius);
		}
	}

	private static class ShellScanner {
		private int shell;
		private int x;
		private int y;
		private int z;

		private BlockPos next(Vec3 center, double radius) {
			int maxShell = (int) Math.ceil(radius);
			BlockPos origin = BlockPos.containing(center);

			while (shell <= maxShell) {
				if (shell == 0) {
					shell = 1;
					resetCursor();
					return origin;
				}

				int currentShell = shell;
				int currentX = x;
				int currentY = y;
				int currentZ = z;
				advanceCursor(maxShell);

				int offsetDistanceSqr = currentX * currentX + currentY * currentY + currentZ * currentZ;
				int previousShellSqr = (currentShell - 1) * (currentShell - 1);
				int currentShellSqr = currentShell * currentShell;
				if (offsetDistanceSqr <= previousShellSqr || offsetDistanceSqr > currentShellSqr) {
					continue;
				}

				BlockPos pos = origin.offset(currentX, currentY, currentZ);
				if (pos.distToCenterSqr(center.x, center.y, center.z) <= radius * radius) {
					return pos;
				}
			}

			return null;
		}

		private void advanceCursor(int maxShell) {
			x++;
			if (x <= shell) {
				return;
			}

			x = -shell;
			z++;
			if (z <= shell) {
				return;
			}

			z = -shell;
			y++;
			if (y <= shell) {
				return;
			}

			shell++;
			if (shell <= maxShell) {
				resetCursor();
			}
		}

		private void resetCursor() {
			x = -shell;
			y = -shell;
			z = -shell;
		}
	}
}
