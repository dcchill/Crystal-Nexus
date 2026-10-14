package net.crystalnexus.item;

import net.crystalnexus.network.payload.S2C_RailgunBeam;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

public class BlackHoleRpgItem extends Item {
    public static final int SHOT_COST = 25_000;
    public static final int COOLDOWN_TICKS = 60;
    public static final int RANGE = 256;
    public static final float DAMAGE = 100;
    public static final int MIN_CHARGE_TICKS = 5;
    public static final int FULL_CHARGE_TICKS = 60;
    private static final int USE_DURATION = 72_000;

    public static float charge(int ticks) {
        return Math.clamp((ticks - MIN_CHARGE_TICKS) / (float) (FULL_CHARGE_TICKS - MIN_CHARGE_TICKS), 0, 1);
    }

    public static float damage(float charge) { return DAMAGE * (1 + 3 * charge); }

    public BlackHoleRpgItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(this)
                || !ToolEnergy.consume(player, stack, SHOT_COST, true)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return USE_DURATION; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        });
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel server) || !(user instanceof Player player)) return;
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(this)
                || !ToolEnergy.consume(player, stack, SHOT_COST, true)) {
            player.stopUsingItem();
            return;
        }
        int ticks = USE_DURATION - remainingUseDuration;
        float charge = charge(ticks);
        if (ticks % 4 == 0) {
            Vec3 muzzle = player.getEyePosition().add(0, 0.2, 0).add(player.getLookAngle().scale(0.8));
            double radius = 0.25 + charge * 0.5;
            Vec3 forward = player.getLookAngle();
            Vec3 axis = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 side = forward.cross(axis).normalize();
            Vec3 up = side.cross(forward).normalize();
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8 + ticks * 0.2;
                Vec3 offset = side.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
                // The shooter renders smaller local sparks and hides them while scoped.
                for (var viewer : server.players()) {
                    if (viewer == player) continue;
                    server.sendParticles(viewer, ParticleTypes.END_ROD, false,
                            muzzle.x + offset.x, muzzle.y + offset.y, muzzle.z + offset.z,
                            0, -offset.x, -offset.y, -offset.z, 0.15 + charge * 0.2);
                }
            }
        }
        if (ticks % 8 == 0) level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.3f + charge * 0.4f, 0.5f + charge * 1.5f);
        if (ticks == FULL_CHARGE_TICKS) level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS, 1f, 1.5f);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(level instanceof ServerLevel server) || !(user instanceof Player player)
                || player.getUseItem() != stack || player.isSpectator() || player.getCooldowns().isOnCooldown(this)) return;
        int ticks = USE_DURATION - timeLeft;
        if (ticks < MIN_CHARGE_TICKS || !ToolEnergy.consume(player, stack, SHOT_COST, false)) return;
        float charge = charge(ticks);
        fireBeam(server, player, charge);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS,
                1f + charge, 1.5f - charge * 0.8f);
    }

    private static void fireBeam(ServerLevel level, Player player, float charge) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start;
        // Trace short segments so a long-range shot never loads chunks beyond the visible world.
        for (int distance = 1; distance <= RANGE; distance++) {
            Vec3 next = start.add(direction.scale(distance));
            BlockPos pos = BlockPos.containing(next);
            if (level.isOutsideBuildHeight(pos)
                    || !level.hasChunksAt(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) break;
            var blockHit = level.clip(new ClipContext(end, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            end = blockHit.getLocation();
            if (blockHit.getType() == HitResult.Type.BLOCK) break;
        }
        var entityHit = ProjectileUtil.getEntityHitResult(player, start, end, new AABB(start, end).inflate(1),
                target -> target instanceof LivingEntity && target.isAlive() && !target.isSpectator() && target.isPickable(),
                start.distanceToSqr(end));
        if (entityHit != null) {
            end = entityHit.getLocation();
            entityHit.getEntity().hurt(level.damageSources().sonicBoom(player), damage(charge));
        }
        PacketDistributor.sendToPlayersInDimension(level, new S2C_RailgunBeam(player.getId(), start, end, charge));
        level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        double burst = 0.2 + charge * 1.5;
        for (var viewer : level.players()) {
            level.sendParticles(viewer, ParticleTypes.END_ROD, true, end.x, end.y, end.z,
                    12 + (int) (charge * 60), burst, burst, burst, 0.05 + charge * 0.25);
        }
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return ToolEnergy.barWidth(stack); }
    @Override public int getBarColor(ItemStack stack) { return 0x00FF00; }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) { return slotChanged; }
}
