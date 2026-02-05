package net.crystalnexus.procedures;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class HoverPackChestplateTickEventProcedure {

	// Costs
	private static final int FE_IDLE_PER_TICK = 4;   // holding hover
	private static final int FE_RISE_PER_TICK = 8;   // extra while rising
	private static final int FE_SPRINT_PER_TICK = 12; // extra while sprint-boosting (tweak)

	// Base Horizontal feel
	private static final double BASE_HOVER_ACCEL = 0.12;
	private static final double BASE_HOVER_DRAG = 0.94;
	private static final double BASE_MAX_HORIZ_SPEED = 0.65;

	// Thruster offsets (derived from your hover_pack.java parts)
	// X/Z are in blocks (pixels/16). Y is a world height from feet; tweak until it lines up perfectly.
	private static final double THRUSTER_SIDE = 7.5 / 16.0;  // ~0.46875
	private static final double THRUSTER_BACK = 8.5 / 16.0;  // ~0.53125
	private static final double THRUSTER_Y    = -0.5;        // backpack height (tweak)

	
	// Base Vertical feel
	private static final double BASE_RISE_TARGET_SPEED = 0.40;
	private static final double BASE_MAX_UP_SPEED = 0.75;
	private static final double BASE_MAX_DOWN_SPEED = -0.22;

	// Sprint boost multipliers
	private static final double SPRINT_ACCEL_MULT = 1.85;     // accel boost
	private static final double SPRINT_MAXSPEED_MULT = 1.75;  // top speed boost
	private static final double SPRINT_RISE_MULT = 1.75;      // faster climb while rising
	private static final double SPRINT_UPSPEED_MULT = 1.55;   // higher vertical speed cap

	// PersistentData keys
	private static final String PD_ACTIVE = "cn_hoverpack_active";
	private static final String PD_TARGETY = "cn_hoverpack_targetY";
	private static final String PD_RISE = "cn_hoverpack_rise";

	// Movement key booleans you set via MCreator keybinds
	private static final String PD_FWD = "cn_hp_fwd";
	private static final String PD_BACK = "cn_hp_back";
	private static final String PD_LEFT = "cn_hp_left";
	private static final String PD_RIGHT = "cn_hp_right";

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!entity.getPersistentData().contains("cn_hoverpack_enabled")) {
	entity.getPersistentData().putBoolean("cn_hoverpack_enabled", true);
}

		if (!entity.getPersistentData().getBoolean("cn_hoverpack_enabled")) {
	entity.getPersistentData().putBoolean("cn_hoverpack_active", false);
	return;
}

		if (entity == null) return;
		if (!(world instanceof Level level) || level.isClientSide) return;
		if (!(entity instanceof LivingEntity living)) return;

		// Disable while sneaking
		if (entity.isShiftKeyDown()) {
			entity.getPersistentData().putBoolean(PD_ACTIVE, false);
			return;
		}

		// No hover in fluids
		if (living.isInWater() || living.isInLava()) {
			entity.getPersistentData().putBoolean(PD_ACTIVE, false);
			return;
		}

		// Only while airborne
		if (living.onGround()) {
			entity.getPersistentData().putBoolean(PD_ACTIVE, false);
			return;
		}

		boolean riseHeld = entity.getPersistentData().getBoolean(PD_RISE);
		boolean sprintBoost = (entity instanceof Player p) && p.isSprinting();

		// Apply sprint-scaled parameters
		double hoverAccel = BASE_HOVER_ACCEL * (sprintBoost ? SPRINT_ACCEL_MULT : 1.0);
		double hoverDrag = BASE_HOVER_DRAG;
		double maxHorizSpeed = BASE_MAX_HORIZ_SPEED * (sprintBoost ? SPRINT_MAXSPEED_MULT : 1.0);

		double riseTargetSpeed = BASE_RISE_TARGET_SPEED * (sprintBoost ? SPRINT_RISE_MULT : 1.0);
		double maxUpSpeed = BASE_MAX_UP_SPEED * (sprintBoost ? SPRINT_UPSPEED_MULT : 1.0);
		double maxDownSpeed = BASE_MAX_DOWN_SPEED;

		int costThisTick = FE_IDLE_PER_TICK
				+ (riseHeld ? FE_RISE_PER_TICK : 0)
				+ (sprintBoost ? FE_SPRINT_PER_TICK : 0);

		// If we can't pay, shut off hover
		if (!canPayEnergy(entity, costThisTick)) {
			entity.getPersistentData().putBoolean(PD_ACTIVE, false);
			return;
		}

		// Init target altitude on first powered tick
		if (!entity.getPersistentData().getBoolean(PD_ACTIVE)) {
			entity.getPersistentData().putBoolean(PD_ACTIVE, true);
			entity.getPersistentData().putDouble(PD_TARGETY, entity.getY());
		}

		// Update target altitude while rising
		double targetY = entity.getPersistentData().getDouble(PD_TARGETY);
		if (riseHeld) {
			targetY += riseTargetSpeed;
			entity.getPersistentData().putDouble(PD_TARGETY, targetY);
		}

		// Altitude hold
		double error = targetY - entity.getY();
		double desiredVy = clamp(error * 0.25, maxDownSpeed, maxUpSpeed);

		// ----- Horizontal control using keybind booleans -----
		double forward = 0.0;
		double strafe = 0.0;

		if (entity.getPersistentData().getBoolean(PD_FWD)) forward += 1.0;
		if (entity.getPersistentData().getBoolean(PD_BACK)) forward -= 1.0;
		if (entity.getPersistentData().getBoolean(PD_LEFT)) strafe -= 1.0;
		if (entity.getPersistentData().getBoolean(PD_RIGHT)) strafe += 1.0;

		// Normalize diagonal input
		double len = Math.sqrt(forward * forward + strafe * strafe);
		if (len > 1e-6) {
			forward /= len;
			strafe /= len;
		}

		Vec3 vel = entity.getDeltaMovement();

		// Apply thrust in look-yaw space
		if (forward != 0.0 || strafe != 0.0) {
			float yawRad = (float) Math.toRadians(entity.getYRot());
			double sin = Mth.sin(yawRad);
			double cos = Mth.cos(yawRad);

			double ax = ((-sin) * forward + (cos) * strafe) * hoverAccel;
			double az = ((cos) * forward + (sin) * strafe) * hoverAccel;

			vel = vel.add(ax, 0.0, az);
		}

		// Drag (glide)
		vel = new Vec3(vel.x * hoverDrag, vel.y, vel.z * hoverDrag);

		// Clamp horizontal speed
		double hspeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
		if (hspeed > maxHorizSpeed) {
			double s = maxHorizSpeed / hspeed;
			vel = new Vec3(vel.x * s, vel.y, vel.z * s);
		}

		// Apply ONE final velocity (keep altitude hold on Y)
		entity.setDeltaMovement(vel.x, desiredVy, vel.z);
		entity.hurtMarked = true;
		entity.fallDistance = 0;

		// Wind particles out the bottom while powered
		spawnHoverpackWind(level, living, riseHeld, sprintBoost);

		// Drain energy after applying
		drainEnergy(entity, costThisTick);
	}

private static void spawnHoverpackWind(Level level, LivingEntity living, boolean riseHeld, boolean sprintBoost) {
	if (!(level instanceof ServerLevel serverLevel)) return;

	// Only show “air movement” when it’s actually doing something
	Vec3 v = living.getDeltaMovement();
	double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
	boolean doingWork = riseHeld || sprintBoost || horiz > 0.06 || v.y > 0.06;
	if (!doingWork) return;

	// Use BODY yaw (torso), not head/camera yaw
	float bodyYawRad = (float) Math.toRadians(living.yBodyRot);
	double sin = Mth.sin(bodyYawRad);
	double cos = Mth.cos(bodyYawRad);

	// Right vector (XZ): (cos, sin)
	double rightX = cos;
	double rightZ = sin;

	// Back vector (XZ): (sin, -cos) (behind the torso)
	double backX = sin;
	double backZ = -cos;

	// Anchor around chest/back area based on entity height (more stable than fixed Y)
	double chestY = living.getY() + living.getBbHeight() * 0.65;

	// Base point: behind player at chest/back height
	double baseX = living.getX() + backX * THRUSTER_BACK;
	double baseY = chestY + THRUSTER_Y;
	double baseZ = living.getZ() + backZ * THRUSTER_BACK;

	// Two emitters: left and right
	double leftX = baseX - rightX * THRUSTER_SIDE;
	double leftZ = baseZ - rightZ * THRUSTER_SIDE;

	double rightXX = baseX + rightX * THRUSTER_SIDE;
	double rightZZ = baseZ + rightZ * THRUSTER_SIDE;

	// More particles when sprinting / rising
	int count = 2 + (sprintBoost ? 5 : 0) + (riseHeld ? 4 : 0);
	double spreadXZ = 0.06 + (sprintBoost ? 0.05 : 0.0);
	double spreadY  = 0.01; // VERY small vertical spread

	double speed = 0.06 + (sprintBoost ? 0.08 : 0.0);

serverLevel.sendParticles(ParticleTypes.CLOUD, leftX, baseY, leftZ, count, spreadXZ, spreadY, spreadXZ, speed);
serverLevel.sendParticles(ParticleTypes.CLOUD, rightXX, baseY, rightZZ, count, spreadXZ, spreadY, spreadXZ, speed);



	if (sprintBoost && serverLevel.getRandom().nextFloat() < 0.35f) {
		serverLevel.sendParticles(ParticleTypes.CLOUD, leftX, baseY, leftZ, 1, 0.03, 0.02, 0.03, 0.01);
		serverLevel.sendParticles(ParticleTypes.GUST, leftX, baseY, leftZ, 1, 0.03, 0.02, 0.03, 0.01);
		serverLevel.sendParticles(ParticleTypes.CLOUD, rightXX, baseY, rightZZ, 1, 0.03, 0.02, 0.03, 0.01);
		serverLevel.sendParticles(ParticleTypes.GUST, rightXX, baseY, rightZZ, 1, 0.03, 0.02, 0.03, 0.01);
	}
}



	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}

	private static boolean canPayEnergy(Entity entity, int amount) {
		if (!(entity instanceof Player player)) return false;

		int remaining = amount;

		for (ItemStack stack : player.getInventory().items) {
			if (stack.isEmpty()) continue;
			IEnergyStorage es = stack.getCapability(Capabilities.EnergyStorage.ITEM);
			if (es == null || !es.canExtract()) continue;

			int extracted = es.extractEnergy(remaining, true); // simulate
			remaining -= extracted;
			if (remaining <= 0) return true;
		}

		for (ItemStack stack : player.getInventory().offhand) {
			if (stack.isEmpty()) continue;
			IEnergyStorage es = stack.getCapability(Capabilities.EnergyStorage.ITEM);
			if (es == null || !es.canExtract()) continue;

			int extracted = es.extractEnergy(remaining, true); // simulate
			remaining -= extracted;
			if (remaining <= 0) return true;
		}

		return false;
	}

	private static void drainEnergy(Entity entity, int amount) {
		if (!(entity instanceof Player player)) return;

		int remaining = amount;

		for (ItemStack stack : player.getInventory().items) {
			if (stack.isEmpty()) continue;

			IEnergyStorage es = stack.getCapability(Capabilities.EnergyStorage.ITEM);
			if (es == null || !es.canExtract()) continue;

			int extracted = es.extractEnergy(remaining, false);
			remaining -= extracted;
			if (remaining <= 0) return;
		}

		for (ItemStack stack : player.getInventory().offhand) {
			if (stack.isEmpty()) continue;

			IEnergyStorage es = stack.getCapability(Capabilities.EnergyStorage.ITEM);
			if (es == null || !es.canExtract()) continue;

			int extracted = es.extractEnergy(remaining, false);
			remaining -= extracted;
			if (remaining <= 0) return;
		}
	}
}
