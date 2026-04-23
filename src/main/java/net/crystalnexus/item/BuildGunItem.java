package net.crystalnexus.item;

import net.minecraft.core.component.DataComponents;
import net.crystalnexus.schematic.BuildgunSchematicManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class BuildGunItem extends Item {
	public BuildGunItem() {
		super(new Item.Properties());
	}

	public static boolean isVisualActive(ItemStack stack) {
		if (!(stack.getItem() instanceof BuildGunItem)) {
			return false;
		}
		var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.getBoolean("buildgunPlacementActive") || tag.getBoolean("buildgunBuildingActive");
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		ItemStack stack = context.getItemInHand();
		if (player.isShiftKeyDown()) {
			BuildgunSchematicManager.beginPlacement(serverPlayer, stack);
			return InteractionResult.CONSUME;
		}
		if (BuildgunSchematicManager.isPlacementActive(stack)) {
			BuildgunSchematicManager.tryBuild(serverPlayer, stack);
			return InteractionResult.CONSUME;
		}

		return InteractionResult.PASS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResultHolder.success(stack);
		}

		if (player.isShiftKeyDown()) {
			BuildgunSchematicManager.beginPlacement(serverPlayer, stack);
			return InteractionResultHolder.consume(stack);
		}
		if (BuildgunSchematicManager.isPlacementActive(stack)) {
			BuildgunSchematicManager.tryBuild(serverPlayer, stack);
			return InteractionResultHolder.consume(stack);
		}

		return InteractionResultHolder.pass(stack);
	}
}
