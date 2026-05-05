package net.crystalnexus.item;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.network.payload.S2C_OrbitalStrikeBeam;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class OrbitalStrikeRemoteItem extends Item {
	private static final double STRIKE_RANGE = 180.0D;
	private static final float EXPLOSION_POWER = 25.0F;
	private static final int EXPLOSION_COUNT = 5;
	private static final double EXPLOSION_VERTICAL_SPACING = 4.0D;
	private static final float EXPLOSION_POWER_FALLOFF = 0.86F;
	private static final int COOLDOWN_TICKS = 80;
	private static final int BEAM_DURATION_TICKS = 30;
	private static final int IMPACT_DELAY_TICKS = 10;

	public OrbitalStrikeRemoteItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return new InteractionResultHolder<>(InteractionResult.SUCCESS_NO_ITEM_USED, stack);
		}

		if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
			if (callStrike(serverLevel, serverPlayer)) {
				player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
				return new InteractionResultHolder<>(InteractionResult.SUCCESS_NO_ITEM_USED, stack);
			}
		}

		return InteractionResultHolder.pass(stack);
	}

	private static boolean callStrike(ServerLevel level, ServerPlayer player) {
		BlockHitResult hit = raycast(level, player, STRIKE_RANGE);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return false;
		}

		Vec3 target = strikeTarget(level, hit);
		double skyY = Math.max(target.y + 48.0D, level.getMaxBuildHeight() - 2.0D);
		PacketDistributor.sendToPlayersInDimension(level, new S2C_OrbitalStrikeBeam(target.x, target.y, target.z, skyY, BEAM_DURATION_TICKS, IMPACT_DELAY_TICKS));

		BlockPos soundPos = BlockPos.containing(target);
		level.playSound(null, soundPos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 1.8F);
		level.playSound(null, soundPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.5F, 1.35F);
		level.sendParticles(ParticleTypes.FLASH, target.x, target.y + 0.1D, target.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		level.sendParticles(ParticleTypes.END_ROD, target.x, target.y + 1.0D, target.z, 80, 0.9D, 1.5D, 0.9D, 0.18D);

		CrystalnexusMod.queueServerWork(IMPACT_DELAY_TICKS, () -> detonate(level, player, target));
		return true;
	}

	private static BlockHitResult raycast(Level level, Player player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().normalize().scale(range));
		return level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
	}

	private static Vec3 strikeTarget(ServerLevel level, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		Vec3 location = hit.getLocation();
		double y = Math.min(level.getMaxBuildHeight() - 1.0D, pos.getY() + 1.02D);
		return new Vec3(location.x, y, location.z);
	}

	private static void detonate(ServerLevel level, ServerPlayer player, Vec3 target) {
		if (!level.hasChunkAt(BlockPos.containing(target))) {
			return;
		}

		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.x, target.y, target.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		level.sendParticles(ParticleTypes.FLASH, target.x, target.y + 0.2D, target.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		level.playSound(null, BlockPos.containing(target), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.85F);

		for (int i = 0; i < EXPLOSION_COUNT; i++) {
			double y = target.y - i * EXPLOSION_VERTICAL_SPACING;
			if (y < level.getMinBuildHeight()) {
				break;
			}

			float power = EXPLOSION_POWER * (float) Math.pow(EXPLOSION_POWER_FALLOFF, i);
			level.explode(player, target.x, y, target.z, power, Level.ExplosionInteraction.TNT);
		}
	}
}
