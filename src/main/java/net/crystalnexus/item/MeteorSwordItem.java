package net.crystalnexus.item;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.List;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class MeteorSwordItem extends SwordItem {
	public static final int ENERGY_PER_HIT = 500;
	private static final int MAX_CHARGE = 5;
	private static final int TICKS_PER_CHARGE = 20;
	private static final int CHARGE_USE_DURATION = MAX_CHARGE * TICKS_PER_CHARGE + 1;
	private static final String CHARGE_KEY = "MeteorSwordCharge";
	private static final ThreadLocal<Boolean> SWEEPING = ThreadLocal.withInitial(() -> false);
	private static final Tier TIER = new Tier() {
		@Override public int getUses() { return 0; }
		@Override public float getSpeed() { return 1.0F; }
		@Override public float getAttackDamageBonus() { return 0; }
		@Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
		@Override public int getEnchantmentValue() { return 20; }
		@Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
	};
	private static final ItemAttributeModifiers ATTRIBUTES = ItemAttributeModifiers.builder()
		.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 15.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
		.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
		.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "meteor_sword_reach"), 2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
		.build();

	public MeteorSwordItem() {
		super(TIER, new Item.Properties().stacksTo(1)
			.attributes(ATTRIBUTES).fireResistant());
	}

	@Override public Component getName(ItemStack stack) { return GradientItemName.yellowToOrange(super.getName(stack)); }
	@Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || oldStack.getItem() != newStack.getItem();
	}

	@Override public boolean isBarVisible(ItemStack stack) { return true; }
	@Override public int getBarWidth(ItemStack stack) { return ToolEnergy.barWidth(stack); }
	@Override public int getBarColor(ItemStack stack) {
		int green = 136 + Math.round(119.0F * Math.min(ToolEnergy.CAPACITY, BatteryData.getEnergy(stack)) / ToolEnergy.CAPACITY);
		return 0xFF0000 | (green << 8);
	}

	@Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
		if (getCharge(stack) == MAX_CHARGE) return InteractionResultHolder.consume(stack);
		setCharge(stack, 0);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return CHARGE_USE_DURATION; }
	@Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

	@Override public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
		int ticks = getUseDuration(stack, user) - remainingUseDuration;
		int charge = Math.min(MAX_CHARGE, ticks / TICKS_PER_CHARGE);
		if (getCharge(stack) != charge) setCharge(stack, charge);
		if (user instanceof Player player && level.isClientSide() && player.tickCount % 4 == 0) {
			boolean partial = ticks % TICKS_PER_CHARGE >= TICKS_PER_CHARGE / 2 && charge < MAX_CHARGE;
			player.displayClientMessage(chargeStatus("Meteor charge ", charge, partial), true);
		}
	}

	@Override public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
		if (level.isClientSide()) return;
		int ticks = getUseDuration(stack, user) - timeLeft;
		int charge = Math.min(MAX_CHARGE, ticks / TICKS_PER_CHARGE);
		setCharge(stack, charge);
		if (user instanceof Player player) {
			if (charge == 0) player.displayClientMessage(Component.literal("Hold to charge the Meteor Sword"), true);
			else player.displayClientMessage(chargeStatus("Charged ", charge, false), true);
		}
	}

	private static Component chargeStatus(String label, int charge, boolean partial) {
		return Component.literal(label).withStyle(ChatFormatting.GRAY).append(chargeMeter(charge, partial))
			.append(Component.literal("x" + multiplier(charge)).withStyle(ChatFormatting.RED))
			.append(Component.literal("  " + energyCost(charge) + " FE").withStyle(ChatFormatting.GREEN));
	}

	public static MutableComponent chargeMeter(int charge, boolean partial) {
		MutableComponent message = Component.literal("[");
		for (int index = 0; index < MAX_CHARGE; index++) {
			int green = 136 + Math.round(119.0F * index / (MAX_CHARGE - 1));
			int color = 0xFF0000 | (green << 8);
			boolean filled = index < charge || partial && index == charge;
			message = message.append(Component.literal(filled ? Character.toString(0x25A0) : Character.toString(0x25A1)).withStyle(style -> style.withColor(color)));
		}
		return message.append(Component.literal("] "));
	}

	private static int getCharge(ItemStack stack) {
		return Math.max(0, Math.min(MAX_CHARGE,
			stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(CHARGE_KEY)));
	}

	public static int chargeLevel(ItemStack stack) { return getCharge(stack); }
	public static Component chargeStatus(ItemStack stack) { return chargeStatus("Meteor charge ", getCharge(stack), false); }

	private static void setCharge(ItemStack stack, int charge) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(CHARGE_KEY, Math.max(0, Math.min(MAX_CHARGE, charge))));
	}

	private static int multiplier(int charge) { return 1 << Math.max(0, Math.min(MAX_CHARGE, charge)); }
	private static int energyCost(int charge) { return ENERGY_PER_HIT * multiplier(charge); }

	@SubscribeEvent
	public static void onAttack(AttackEntityEvent event) {
		if (event.getEntity().level().isClientSide()) return;
		var player = event.getEntity();
		ItemStack weapon = player.getMainHandItem();
		if (!weapon.is(CrystalnexusModItems.METEOR_SWORD.get())) return;
		if (!ToolEnergy.consume(player, weapon, energyCost(getCharge(weapon)), true)) {
			event.setCanceled(true);
			if (player.tickCount % 10 == 0)
				player.displayClientMessage(Component.literal("Meteor Sword needs " + energyCost(getCharge(weapon)) + " FE"), true);
		}
	}

	@SubscribeEvent
	public static void empower(LivingDamageEvent.Pre event) {
		if (SWEEPING.get() || !(event.getSource().getEntity() instanceof Player attacker)
				|| event.getSource().getDirectEntity() != attacker || attacker.level().isClientSide()
				|| !attacker.getMainHandItem().is(CrystalnexusModItems.METEOR_SWORD.get())) return;
		ItemStack weapon = attacker.getMainHandItem();
		int charge = getCharge(weapon);
		if (!ToolEnergy.consume(attacker, weapon, energyCost(charge), false)) {
			event.setNewDamage(0);
			return;
		}
		event.setNewDamage(event.getNewDamage() * multiplier(charge));
		if (charge > 0) setCharge(weapon, 0);
	}

	@SubscribeEvent
	public static void sweep(LivingDamageEvent.Post event) {
		if (SWEEPING.get() || !(event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player attacker)
				|| event.getSource().getDirectEntity() != attacker || attacker.level().isClientSide()
				|| event.getNewDamage() <= 0 || !attacker.getMainHandItem().is(CrystalnexusModItems.METEOR_SWORD.get())) return;

		LivingEntity primary = event.getEntity();
		Vec3 direction = primary.position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
		if (direction.lengthSqr() < 1.0e-4) direction = attacker.getLookAngle().multiply(1, 0, 1).normalize();
		final Vec3 sweepDirection = direction;
		final double radius = 4.5D;
		List<LivingEntity> targets = attacker.level().getEntitiesOfClass(LivingEntity.class,
			attacker.getBoundingBox().inflate(radius, 1.5D, radius), target -> target != attacker && target != primary
				&& target.isAlive() && !attacker.isAlliedTo(target)
				&& target.position().subtract(attacker.position()).multiply(1, 0, 1).lengthSqr() <= radius * radius
				&& sweepDirection.dot(target.position().subtract(attacker.position()).multiply(1, 0, 1).normalize()) > 0.15D);

		SWEEPING.set(true);
		try {
			float sweepRatio = (float) Math.min(1.0D, 0.4D + attacker.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * 0.4D);
			float sweepDamage = Math.max(1.0F, event.getNewDamage() * sweepRatio);
			for (LivingEntity target : targets) target.hurt(event.getSource(), sweepDamage);
		} finally {
			SWEEPING.remove();
		}

		if (attacker.level() instanceof ServerLevel level) {
			double centerAngle = Math.atan2(sweepDirection.z, sweepDirection.x);
			for (int step = -9; step <= 9; step++) {
				double angle = centerAngle + Math.toRadians(step * 9.0D);
				double distance = radius * (0.88D + (step % 2 == 0 ? 0.12D : 0.0D));
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, attacker.getX() + Math.cos(angle) * distance,
					attacker.getY() + 0.9D, attacker.getZ() + Math.sin(angle) * distance, 1, 0, 0, 0, 0);
			}
		}
	}

	@EventBusSubscriber(modid = CrystalnexusMod.MODID, bus = EventBusSubscriber.Bus.MOD)
	public static final class ModEvents {
		@SubscribeEvent
		public static void removeDurability(ModifyDefaultComponentsEvent event) {
			event.modify(CrystalnexusModItems.METEOR_SWORD.get(), builder -> builder.remove(DataComponents.MAX_DAMAGE));
		}
	}
}
