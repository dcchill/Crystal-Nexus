package net.crystalnexus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.crystalnexus.config.CrystalnexusConfig;

public class MiningLaserItem extends Item {
	public static final double DEFAULT_RANGE = 32.0D;
	private static final int USE_DURATION_TICKS = 72000;

	public static double range() {
		return CrystalnexusConfig.ITEMS.MINING_LASER.range();
	}

	private static int sustainFePerTick() {
		return CrystalnexusConfig.ITEMS.MINING_LASER.sustainFePerTick();
	}

	private static int mineFePerBlock() {
		return CrystalnexusConfig.ITEMS.MINING_LASER.mineFePerBlock();
	}

	private static int mineIntervalTicks() {
		return CrystalnexusConfig.ITEMS.MINING_LASER.mineIntervalTicks();
	}

	public MiningLaserItem() {
		super(new Item.Properties().stacksTo(1).attributes(ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build()));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) {
			player.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}

		if (!player.getAbilities().instabuild && !canExtractFromBatteries(player, sustainFePerTick())) {
			player.displayClientMessage(Component.literal("Out of battery power").withStyle(ChatFormatting.RED), true);
			return InteractionResultHolder.fail(stack);
		}

		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
		if (!(livingEntity instanceof Player player)) {
			return;
		}

		if (level.isClientSide()) {
			return;
		}

		boolean creative = player.getAbilities().instabuild;
		if (!creative && !extractFromBatteries(player, sustainFePerTick())) {
			player.stopUsingItem();
			player.displayClientMessage(Component.literal("Out of battery power").withStyle(ChatFormatting.RED), true);
			return;
		}

		int ticksUsed = USE_DURATION_TICKS - remainingUseDuration;
		if (ticksUsed % mineIntervalTicks() != 0) {
			return;
		}

		BlockHitResult hit = getLaserHit(level, player);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		BlockPos pos = hit.getBlockPos();
		if (!canMine(level, player, pos)) {
			return;
		}

		if (!creative && !extractFromBatteries(player, mineFePerBlock())) {
			player.stopUsingItem();
			player.displayClientMessage(Component.literal("Out of battery power").withStyle(ChatFormatting.RED), true);
			return;
		}

		mineBlock(level, player, stack, pos);
	}

	@Override
	public float getDestroySpeed(ItemStack itemstack, BlockState blockstate) {
		return 1.0F;
	}

	@Override
	public boolean mineBlock(ItemStack itemstack, Level world, BlockState blockstate, BlockPos pos, LivingEntity entity) {
		return true;
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		return true;
	}

	@Override
	public int getEnchantmentValue() {
		return 1;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_DURATION_TICKS;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.NONE;
	}

	public static BlockHitResult getLaserHit(Level level, Entity entity) {
		Vec3 start = entity.getEyePosition();
		Vec3 end = start.add(entity.getLookAngle().scale(range()));
		return level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
	}

	private static boolean canMine(Level level, Player player, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return !state.isAir()
				&& state.getDestroySpeed(level, pos) >= 0.0F
				&& !state.hasBlockEntity()
				&& player.mayBuild()
				&& level.mayInteract(player, pos);
	}

	private static void mineBlock(Level level, Player player, ItemStack tool, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
		Block.dropResources(state, level, pos, blockEntity, player, tool);
		level.destroyBlock(pos, false, player);
		level.levelEvent(2001, pos, Block.getId(state));

		if (level instanceof ServerLevel serverLevel) {
			SoundType soundType = state.getSoundType(level, pos, player);
			serverLevel.playSound(null, pos, soundType.getBreakSound(), SoundSource.BLOCKS, 0.35F, 1.45F);
		}
	}

	private static boolean canExtractFromBatteries(Player player, int feNeeded) {
		return countExtractableBatteryEnergy(player, feNeeded) >= feNeeded;
	}

	private static int countExtractableBatteryEnergy(Player player, int feNeeded) {
		int available = 0;
		for (ItemStack battery : player.getInventory().items) {
			available += extractFromBattery(battery, feNeeded - available, true);
			if (available >= feNeeded) {
				return available;
			}
		}
		available += extractFromBattery(player.getOffhandItem(), feNeeded - available, true);
		return available;
	}

	private static boolean extractFromBatteries(Player player, int feNeeded) {
		if (!canExtractFromBatteries(player, feNeeded)) {
			return false;
		}

		int remaining = feNeeded;
		for (ItemStack battery : player.getInventory().items) {
			remaining -= extractFromBattery(battery, remaining, false);
			if (remaining <= 0) {
				return true;
			}
		}
		remaining -= extractFromBattery(player.getOffhandItem(), remaining, false);
		return remaining <= 0;
	}

	private static int extractFromBattery(ItemStack battery, int amount, boolean simulate) {
		if (amount <= 0 || battery.isEmpty()) {
			return 0;
		}

		IEnergyStorage energy = battery.getCapability(Capabilities.EnergyStorage.ITEM, null);
		return energy == null ? 0 : energy.extractEnergy(amount, simulate);
	}
}
