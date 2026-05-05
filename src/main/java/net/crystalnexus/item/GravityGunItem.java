package net.crystalnexus.item;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.GeoItem;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.crystalnexus.item.renderer.GravityGunItemRenderer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GravityGunItem extends Item implements GeoItem {
	private static final double PICKUP_RANGE = 8.0D;
	private static final double DEFAULT_HOLD_DISTANCE = 4.0D;
	private static final double MIN_HOLD_DISTANCE = 2.0D;
	private static final double MAX_HOLD_DISTANCE = 12.0D;
	private static final double HOLD_DISTANCE_STEP = 0.5D;
	private static final double HOLD_STRENGTH = 0.45D;
	private static final double THROW_SPEED = 2.8D;
	private static final double PROJECTILE_DAMAGE = 9.0D;
	private static final double PROJECTILE_KNOCKBACK = 1.15D;
	private static final int PROJECTILE_MAX_DAMAGE = 20;
	private static final int PROJECTILE_LIFETIME_TICKS = 80;
	private static final int GRAB_ANIMATION_TICKS = 10;
	private static final Map<UUID, UUID> HELD_ENTITIES = new ConcurrentHashMap<>();
	private static final Set<UUID> HELD_FALLING_BLOCKS = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, ThrownBlockProjectile> THROWN_BLOCKS = new ConcurrentHashMap<>();
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public String animationprocedure = "empty";

	public GravityGunItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		ItemStack stack = context.getItemInHand();
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS_NO_ITEM_USED;
		}

		UUID playerId = serverPlayer.getUUID();
		Entity held = getHeldEntity(serverLevel, playerId);
		if (held != null) {
			releaseHeldEntity(playerId, held);
			setHoldingState(stack, false, null);
			return InteractionResult.SUCCESS_NO_ITEM_USED;
		}

		FallingBlockEntity fallingBlock = tryGrabBlockAt(serverPlayer, serverLevel, context.getClickedPos());
		if (fallingBlock != null) {
			grabEntity(serverPlayer, fallingBlock, stack);
			setHoldingState(stack, true, fallingBlock.getUUID());
			return InteractionResult.SUCCESS_NO_ITEM_USED;
		}

		return InteractionResult.PASS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return noSwingResult(stack);
		}

		UUID playerId = serverPlayer.getUUID();
		Entity held = getHeldEntity(serverLevel, playerId);
		if (held != null) {
			releaseHeldEntity(playerId, held);
			setHoldingState(stack, false, null);
			return noSwingResult(stack);
		}

		Entity targetEntity = findTargetEntity(serverPlayer);
		if (targetEntity != null) {
			grabEntity(serverPlayer, targetEntity, stack);
			setHoldingState(stack, true, targetEntity.getUUID());
			return noSwingResult(stack);
		}

		FallingBlockEntity fallingBlock = tryGrabTargetBlock(serverPlayer, serverLevel);
		if (fallingBlock != null) {
			grabEntity(serverPlayer, fallingBlock, stack);
			setHoldingState(stack, true, fallingBlock.getUUID());
			return noSwingResult(stack);
		}

		return InteractionResultHolder.pass(stack);
	}

	private static InteractionResultHolder<ItemStack> noSwingResult(ItemStack stack) {
		return new InteractionResultHolder<>(InteractionResult.SUCCESS_NO_ITEM_USED, stack);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, level, entity, slot, selected);
		if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
			return;
		}

		UUID playerId = player.getUUID();
		Entity held = getHeldEntity(serverLevel, playerId);
		if (held == null) {
			UUID heldEntityId = HELD_ENTITIES.remove(playerId);
			if (heldEntityId != null) {
				HELD_FALLING_BLOCKS.remove(heldEntityId);
			}
			setHoldingState(stack, false, null);
			return;
		}

		if (!selected || player.isSpectator() || player.isDeadOrDying()) {
			releaseHeldEntity(playerId, held);
			setHoldingState(stack, false, null);
			return;
		}

		moveHeldEntity(player, held, stack);
		tickGrabAnimation(stack);
	}

	private static void grabEntity(ServerPlayer player, Entity target, ItemStack stack) {
		target.setNoGravity(true);
		target.setOnGround(false);
		target.fallDistance = 0.0F;
		HELD_ENTITIES.put(player.getUUID(), target.getUUID());
		if (target instanceof FallingBlockEntity) {
			HELD_FALLING_BLOCKS.add(target.getUUID());
		}
		moveHeldEntity(player, target, stack);
	}

	private static void moveHeldEntity(ServerPlayer player, Entity held, ItemStack stack) {
		Vec3 targetPos = player.getEyePosition().add(player.getLookAngle().normalize().scale(getHoldDistance(stack)));
		if (held instanceof FallingBlockEntity) {
			targetPos = collisionFreeHoldTarget(held, targetPos);
			held.setNoGravity(true);
			held.setOnGround(false);
		}
		Vec3 heldCenter = held.position().add(0.0D, held.getBbHeight() * 0.5D, 0.0D);
		Vec3 velocity = targetPos.subtract(heldCenter).scale(HOLD_STRENGTH);
		held.setDeltaMovement(velocity);
		held.hurtMarked = true;
		held.fallDistance = 0.0F;
	}

	private static void releaseHeldEntity(UUID playerId, Entity held) {
		HELD_ENTITIES.remove(playerId);
		HELD_FALLING_BLOCKS.remove(held.getUUID());
		held.setNoGravity(false);
		held.fallDistance = 0.0F;
	}

	public static boolean shootHeldEntity(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return false;
		}

		Entity held = getHeldEntity(serverLevel, player.getUUID());
		if (held == null) {
			return false;
		}

		throwHeldEntity(player, held);
		ItemStack stack = heldGravityGun(player);
		if (!stack.isEmpty()) {
			setHoldingState(stack, false, null);
		}
		return true;
	}

	public static boolean adjustHoldDistance(ServerPlayer player, int steps) {
		if (steps == 0 || !(player.level() instanceof ServerLevel serverLevel)) {
			return false;
		}

		if (getHeldEntity(serverLevel, player.getUUID()) == null) {
			return false;
		}

		ItemStack stack = heldGravityGun(player);
		if (stack.isEmpty()) {
			return false;
		}

		double distance = clampHoldDistance(getHoldDistance(stack) + steps * HOLD_DISTANCE_STEP);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putDouble("gravityGunHoldDistance", distance));
		return true;
	}

	private static void throwHeldEntity(ServerPlayer player, Entity held) {
		HELD_ENTITIES.remove(player.getUUID());
		HELD_FALLING_BLOCKS.remove(held.getUUID());
		held.setNoGravity(false);
		held.setDeltaMovement(player.getLookAngle().normalize().scale(THROW_SPEED));
		held.hurtMarked = true;
		held.fallDistance = 0.0F;
		if (held instanceof FallingBlockEntity fallingBlock) {
			fallingBlock.setHurtsEntities((float) PROJECTILE_DAMAGE, PROJECTILE_MAX_DAMAGE);
			THROWN_BLOCKS.put(fallingBlock.getUUID(), new ThrownBlockProjectile(player.getUUID()));
		}
	}

	public static void tickThrownBlockProjectile(FallingBlockEntity fallingBlock) {
		ThrownBlockProjectile projectile = THROWN_BLOCKS.get(fallingBlock.getUUID());
		if (projectile == null) {
			return;
		}

		if (fallingBlock.level().isClientSide() || fallingBlock.isRemoved() || projectile.ticksLeft-- <= 0) {
			THROWN_BLOCKS.remove(fallingBlock.getUUID());
			return;
		}

		Vec3 motion = fallingBlock.getDeltaMovement();
		if (fallingBlock.onGround() || motion.lengthSqr() < 0.03D) {
			THROWN_BLOCKS.remove(fallingBlock.getUUID());
			return;
		}

		AABB hitBox = fallingBlock.getBoundingBox().expandTowards(motion).inflate(0.35D);
		for (LivingEntity target : fallingBlock.level().getEntitiesOfClass(LivingEntity.class, hitBox, entity -> canProjectileDamage(projectile, entity))) {
			if (target.hurt(fallingBlock.damageSources().fallingBlock(fallingBlock), (float) projectileDamage(motion))) {
				projectile.damagedEntities.add(target.getUUID());
				Vec3 knockback = motion.normalize().scale(PROJECTILE_KNOCKBACK);
				target.push(knockback.x, Math.max(0.2D, knockback.y + 0.25D), knockback.z);
				target.hurtMarked = true;
			}
		}

		if (!projectile.damagedEntities.isEmpty()) {
			fallingBlock.setNoGravity(false);
			fallingBlock.setDeltaMovement(motion.scale(0.25D));
			fallingBlock.hurtMarked = true;
			THROWN_BLOCKS.remove(fallingBlock.getUUID());
		}
	}

	public static void prepareHeldFallingBlock(FallingBlockEntity fallingBlock) {
		if (!HELD_FALLING_BLOCKS.contains(fallingBlock.getUUID())) {
			return;
		}

		fallingBlock.setNoGravity(true);
		fallingBlock.setOnGround(false);
		fallingBlock.fallDistance = 0.0F;
	}

	private static boolean canProjectileDamage(ThrownBlockProjectile projectile, LivingEntity entity) {
		return entity.isAlive()
				&& !entity.isSpectator()
				&& !entity.getUUID().equals(projectile.ownerId)
				&& !projectile.damagedEntities.contains(entity.getUUID());
	}

	private static double projectileDamage(Vec3 motion) {
		return Math.min(PROJECTILE_MAX_DAMAGE, PROJECTILE_DAMAGE + motion.length() * 2.0D);
	}

	private static double getHoldDistance(ItemStack stack) {
		double distance = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("gravityGunHoldDistance");
		return distance <= 0.0D ? DEFAULT_HOLD_DISTANCE : clampHoldDistance(distance);
	}

	private static double clampHoldDistance(double distance) {
		return Math.max(MIN_HOLD_DISTANCE, Math.min(MAX_HOLD_DISTANCE, distance));
	}

	private static Vec3 collisionFreeHoldTarget(Entity held, Vec3 targetPos) {
		Vec3 heldCenter = held.position().add(0.0D, held.getBbHeight() * 0.5D, 0.0D);
		Vec3 adjustedTarget = targetPos;
		for (int i = 0; i < 8; i++) {
			AABB targetBox = held.getBoundingBox().move(adjustedTarget.subtract(heldCenter));
			if (held.level().noCollision(held, targetBox)) {
				return adjustedTarget;
			}
			adjustedTarget = adjustedTarget.add(0.0D, 0.25D, 0.0D);
		}
		return adjustedTarget;
	}

	private static Entity getHeldEntity(ServerLevel level, UUID playerId) {
		UUID entityId = HELD_ENTITIES.get(playerId);
		return entityId == null ? null : level.getEntity(entityId);
	}

	private static ItemStack heldGravityGun(Player player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof GravityGunItem) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static Entity findTargetEntity(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(PICKUP_RANGE));
		AABB searchBox = player.getBoundingBox().expandTowards(player.getLookAngle().scale(PICKUP_RANGE)).inflate(1.0D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, searchBox,
				entity -> entity.isPickable() && entity != player && !entity.isSpectator(), PICKUP_RANGE * PICKUP_RANGE);
		return hit == null ? null : hit.getEntity();
	}

	private static FallingBlockEntity tryGrabTargetBlock(ServerPlayer player, ServerLevel level) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(PICKUP_RANGE));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.MISS) {
			return null;
		}

		BlockPos pos = hit.getBlockPos();
		return tryGrabBlockAt(player, level, pos);
	}

	private static FallingBlockEntity tryGrabBlockAt(ServerPlayer player, ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0.0F || !player.mayBuild()) {
			return null;
		}

		FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level, pos, state);
		fallingBlock.dropItem = true;
		fallingBlock.setNoGravity(true);
		fallingBlock.setDeltaMovement(Vec3.ZERO);
		level.levelEvent(2001, pos, Block.getId(state));
		return fallingBlock;
	}

	private static void setHoldingState(ItemStack stack, boolean holding, UUID heldEntityId) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putBoolean("gravityGunHolding", holding);
			if (tag.getDouble("gravityGunHoldDistance") <= 0.0D) {
				tag.putDouble("gravityGunHoldDistance", DEFAULT_HOLD_DISTANCE);
			}
			if (holding && heldEntityId != null) {
				tag.putString("gravityGunHeldEntity", heldEntityId.toString());
				tag.putString("geckoAnim", "grab");
				tag.putInt("gravityGunGrabTicks", GRAB_ANIMATION_TICKS);
			} else {
				tag.remove("gravityGunHeldEntity");
				tag.remove("gravityGunGrabTicks");
				tag.putString("geckoAnim", "");
			}
		});
	}

	private static void tickGrabAnimation(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			int ticks = tag.getInt("gravityGunGrabTicks");
			if (ticks > 0) {
				tag.putInt("gravityGunGrabTicks", ticks - 1);
			}
		});
	}

	@Override
	public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
		consumer.accept(new GeoRenderProvider() {
			private GravityGunItemRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
				if (this.renderer == null)
					this.renderer = new GravityGunItemRenderer();
				return this.renderer;
			}
		});
	}

	public static final EnumProxy<HumanoidModel.ArmPose> ARM_POSE = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) (model, entity, arm) -> {
		float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		model.rightArm.yRot = -0.3F + model.head.yRot;
		model.leftArm.yRot = 0.6F + model.head.yRot;
		model.rightArm.xRot = -1.35F + model.head.xRot + 0.1F;
		model.leftArm.xRot = -1.25F + model.head.xRot;
		if (arm == HumanoidArm.LEFT) {
			model.leftArm.yRot = 0.3F + model.head.yRot;
			model.rightArm.yRot = -0.6F + model.head.yRot;
		}
		model.rightArm.zRot = side * 0.05F;
		model.leftArm.zRot = -side * 0.05F;
	});

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		super.initializeClient(consumer);
		consumer.accept(new IClientItemExtensions() {
			@Override
			public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
				if (!itemStack.isEmpty()) {
					if (itemStack.getItem() instanceof GravityGunItem) {
						return (HumanoidModel.ArmPose) ARM_POSE.getValue();
					}
				}
				return HumanoidModel.ArmPose.EMPTY;
			}
		});
	}

	private PlayState idlePredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
			return PlayState.CONTINUE;
		}
		return PlayState.STOP;
	}

	String prevAnim = "empty";

	private PlayState procedurePredicate(AnimationState event) {
		if (!this.animationprocedure.equals("empty") && event.getController().getAnimationState() == AnimationController.State.STOPPED || (!this.animationprocedure.equals(prevAnim) && !this.animationprocedure.equals("empty"))) {
			if (!this.animationprocedure.equals(prevAnim))
				event.getController().forceAnimationReset();
			if (this.animationprocedure.equals("holding")) {
				event.getController().setAnimation(RawAnimation.begin().thenLoop("holding"));
			} else {
				event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));
			}
			if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
				this.animationprocedure = "empty";
				event.getController().forceAnimationReset();
			}
		} else if (this.animationprocedure.equals("empty")) {
			prevAnim = "empty";
			return PlayState.STOP;
		}
		prevAnim = this.animationprocedure;
		return PlayState.CONTINUE;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		AnimationController procedureController = new AnimationController(this, "procedureController", 0, this::procedurePredicate);
		data.add(procedureController);
		AnimationController idleController = new AnimationController(this, "idleController", 0, this::idlePredicate);
		data.add(idleController);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	private static final class ThrownBlockProjectile {
		private final UUID ownerId;
		private final Set<UUID> damagedEntities = ConcurrentHashMap.newKeySet();
		private int ticksLeft = PROJECTILE_LIFETIME_TICKS;

		private ThrownBlockProjectile(UUID ownerId) {
			this.ownerId = ownerId;
		}
	}
}
