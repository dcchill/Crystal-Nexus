package net.crystalnexus.entity;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public class BlackHoleProjectileEntity extends Projectile {
    public static final int LIFETIME = 120;
    public static final int BLOCKS_PER_TICK = 32;
    public static final double RADIUS = 5;
    private static final double HOMING_RANGE = 24;
    private static final double MIN_HOMING_ALIGNMENT = 0.85;
    private static final double HOMING_TURN_RATE = 0.08;
    private int age;
    private boolean lockOn = true;

    public BlackHoleProjectileEntity(EntityType<? extends BlackHoleProjectileEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override public void tick() {
        if (isRemoved()) return;
        if (!level().isClientSide() && lockOn) steerTowardHostile();
        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement());
        // Check before the base entity tick, which reads terrain for fluid and fire effects.
        BlockPos destination = BlockPos.containing(end);
        if (!level().isClientSide() && (!level().hasChunksAt(blockPosition().offset(-1, 0, -1), blockPosition().offset(1, 0, 1))
                || !level().hasChunksAt(destination.offset(-1, 0, -1), destination.offset(1, 0, 1)) || level().isOutsideBuildHeight(destination))) {
            discard();
            return;
        }
        super.tick();
        if (level().isClientSide()) {
            setPos(position().add(getDeltaMovement()));
            Vec3 offset = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(4);
            level().addParticle(ParticleTypes.PORTAL, getX() + offset.x, getY() + offset.y, getZ() + offset.z,
                    -offset.x, -offset.y, -offset.z);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Vec3 hit = null;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().expandTowards(getDeltaMovement()).inflate(1),
                mob -> mob.isAlive() && mob != getOwner())) {
            var bounds = mob.getBoundingBox().inflate(0.3);
            Vec3 contact = bounds.contains(start) ? start : bounds.clip(start, end).orElse(null);
            if (contact != null && (hit == null || start.distanceToSqr(contact) < start.distanceToSqr(hit))) hit = contact;
        }
        setPos(hit == null ? end : hit);
        if (hit != null || ++age >= LIFETIME) {
            detonate();
            return;
        }
        for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(RADIUS), mob -> mob.isAlive() && mob != getOwner())) {
            Vec3 offset = position().subtract(mob.getBoundingBox().getCenter());
            double distance = offset.length();
            if (distance > RADIUS) continue;
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.6).add(offset.normalize().scale(0.15 + 0.65 * (1 - distance / RADIUS))));
            mob.hurtMarked = true;
            mob.fallDistance = 0;
        }
        var blocks = BlockPos.betweenClosedStream(blockPosition().offset(-5, -5, -5), blockPosition().offset(5, 5, 5))
                .filter(pos -> pos.distToCenterSqr(position()) <= RADIUS * RADIUS).map(BlockPos::immutable)
                .sorted(Comparator.comparingDouble(pos -> pos.distToCenterSqr(position()))).toList();
        int broken = 0;
        for (BlockPos pos : blocks) {
            if (breakBlock(pos, true) && ++broken >= BLOCKS_PER_TICK) break;
        }
    }

    private void steerTowardHostile() {
        Vec3 velocity = getDeltaMovement();
        double speed = velocity.length();
        if (speed < 1.0E-6) return;

        Vec3 aimDirection = velocity.normalize();
        Monster target = level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(HOMING_RANGE),
                mob -> mob.isAlive() && mob != getOwner() && !mob.isAlliedTo(this)
                    && mob.getBoundingBox().getCenter().subtract(position()).normalize().dot(aimDirection) >= MIN_HOMING_ALIGNMENT)
            .stream()
            .min(Comparator.<Monster>comparingDouble(mob -> -mob.getBoundingBox().getCenter().subtract(position()).normalize().dot(aimDirection))
                .thenComparingDouble(mob -> mob.distanceToSqr(this)))
            .orElse(null);
        if (target == null) return;

        Vec3 desiredDirection = target.getBoundingBox().getCenter().subtract(position()).normalize();
        Vec3 curvedDirection = velocity.normalize().scale(1 - HOMING_TURN_RATE)
                .add(desiredDirection.scale(HOMING_TURN_RATE)).normalize();
        setDeltaMovement(curvedDirection.scale(speed));
    }

    public void setLockOn(boolean lockOn) { this.lockOn = lockOn; }
    public boolean isLockOn() { return lockOn; }

    public boolean canBreakBlock(BlockPos pos) {
        if (!(getOwner() instanceof Player player) || !player.mayBuild() || player.isSpectator()
                || !level().hasChunksAt(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))
                || !level().getWorldBorder().isWithinBounds(pos) || !level().mayInteract(player, pos)) return false;
        BlockState state = level().getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && state.getDestroySpeed(level(), pos) >= 0;
    }

    public boolean breakBlock(BlockPos pos, boolean particles) {
        if (!(level() instanceof ServerLevel level) || !canBreakBlock(pos)) return false;
        BlockState state = level.getBlockState(pos);
        Player player = (Player) getOwner();
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) return false;
        // Roll for loot only after a successful break, with a diamond harvest tool so ordinary ores retain their loot.
        if (!level.removeBlock(pos, false)) return false;
        if (level.random.nextFloat() < 0.1F) {
            Block.dropResources(state, level, pos, null, player, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
        }
        if (particles) {
            Vec3 velocity = position().subtract(Vec3.atCenterOf(pos)).normalize().scale(0.3);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    0, velocity.x, velocity.y, velocity.z, 1);
        }
        return true;
    }

    private void detonate() {
        // Vanilla explosion rays and exposure checks read chunks; never let a blast load terrain.
        if (level().hasChunksAt(blockPosition().offset(-17, 0, -17), blockPosition().offset(17, 0, 17))) {
            level().explode(this, getX(), getY(), getZ(), 8, false, Level.ExplosionInteraction.BLOCK);
        }
        discard();
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BlackHoleAge", age);
        tag.putBoolean("BlackHoleLockOn", lockOn);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        age = Math.max(0, tag.getInt("BlackHoleAge"));
        lockOn = !tag.contains("BlackHoleLockOn") || tag.getBoolean("BlackHoleLockOn");
    }
}
