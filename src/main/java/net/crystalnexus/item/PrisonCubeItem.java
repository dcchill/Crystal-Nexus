package net.crystalnexus.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PrisonCubeItem extends Item {
	private static final String STORED_ENTITY = "prisonCubeEntity";

	public PrisonCubeItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		return capture(stack, player, target);
	}

	public InteractionResult capture(ItemStack stack, Player player, Entity target) {
		if (target instanceof Player || !target.isAlive() || hasStoredEntity(stack)) {
			return InteractionResult.PASS;
		}
		if (!player.level().isClientSide) {
			net.crystalnexus.events.PrisonCubeCapture.start(stack, player, target);
		}
		return InteractionResult.sidedSuccess(player.level().isClientSide);
	}

	public static void finishCapture(ItemStack stack, Entity target) {
		CompoundTag entityData = new CompoundTag();
		target.saveWithoutId(entityData);
		prepareForStorage(entityData, EntityType.getKey(target.getType()).toString());
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(STORED_ENTITY, entityData));
		target.stopRiding();
		target.ejectPassengers();
		target.discard();
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		if (player == null || !player.isShiftKeyDown() || !hasStoredEntity(stack)) {
			return InteractionResult.PASS;
		}
		if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		return release(stack, player, serverLevel, context.getClickedPos(), context.getClickedFace());
	}

	public InteractionResult release(ItemStack stack, Player player, ServerLevel serverLevel, BlockPos clickedPos, Direction clickedFace) {
		if (!player.isShiftKeyDown() || !hasStoredEntity(stack)) {
			return InteractionResult.PASS;
		}
		CompoundTag entityData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(STORED_ENTITY);
		Entity entity = EntityType.loadEntityRecursive(entityData, serverLevel, loaded -> loaded);
		if (entity == null) {
			return InteractionResult.FAIL;
		}

		Vec3 spawnPos = Vec3.atBottomCenterOf(clickedPos.relative(clickedFace));
		boolean canSpawn = false;
		for (int yOffset = 0; yOffset < 4; yOffset++) {
			entity.moveTo(spawnPos.x, spawnPos.y + yOffset, spawnPos.z, player.getYRot(), 0f);
			if (serverLevel.noCollision(entity, entity.getBoundingBox())) {
				canSpawn = true;
				break;
			}
		}
		if (!canSpawn) {
			return InteractionResult.FAIL;
		}
		serverLevel.addFreshEntityWithPassengers(entity);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(STORED_ENTITY));
		return InteractionResult.CONSUME;
	}

	public static boolean hasStoredEntity(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(STORED_ENTITY);
	}

	public static boolean hasStoredEntity(ItemStack stack, net.minecraft.resources.ResourceLocation entityType) {
		CompoundTag entityData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(STORED_ENTITY);
		return entityType.toString().equals(entityData.getString("id"));
	}

	public static void clearStoredEntity(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(STORED_ENTITY));
	}

	public static void setStoredEntityType(ItemStack stack, net.minecraft.resources.ResourceLocation entityType) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag entityData = new CompoundTag();
			entityData.putString("id", entityType.toString());
			tag.put(STORED_ENTITY, entityData);
		});
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		CompoundTag entityData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(STORED_ENTITY);
		if (entityData.contains("id")) {
			EntityType.by(entityData).ifPresent(type -> tooltip.add(net.minecraft.network.chat.Component.translatable(
					"item.crystalnexus.prison_cube.mob", type.getDescription())));
		}
	}

	static CompoundTag prepareForStorage(CompoundTag entityData, String entityType) {
		PrisonCubeData.TRANSIENT_ENTITY_FIELDS.forEach(entityData::remove);
		entityData.putString("id", entityType);
		return entityData;
	}
}
