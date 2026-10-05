package net.crystalnexus.item;

import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public final class DemonCoreItem extends Item {
	private static final String OPEN_KEY = "DemonCoreOpen";

	public DemonCoreItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, level, entity, slot, selected);
		if (!level.isClientSide())
			net.crystalnexus.procedures.BlutoniumIngotItemInInventoryTickProcedure.execute(level, entity, isClosed(stack) ? 9 : 4);
	}

	@Override
	public Component getName(ItemStack stack) {
		return GradientItemName.gradient(super.getName(stack), GradientItemName.Palette.BLUTONIUM);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("item.crystalnexus.demon_core.description"));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
		setOpen(stack, !isOpen(stack), level.isClientSide());
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
		ItemStack stack = context.getItemInHand();
		setOpen(stack, !isOpen(stack), context.getLevel().isClientSide());
		return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
	}

	private static boolean isOpen(ItemStack stack) {
		var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return !tag.contains(OPEN_KEY) || tag.getBoolean(OPEN_KEY);
	}

	public static boolean isClosed(ItemStack stack) {
		return stack.getItem() instanceof DemonCoreItem && !isOpen(stack);
	}

	public static ItemStack createClosedStack() {
		ItemStack stack = new ItemStack(CrystalnexusModItems.DEMON_CORE.get());
		setOpen(stack, false, false);
		return stack;
	}

	private static void setOpen(ItemStack stack, boolean open, boolean clientSide) {
		if (clientSide) return;
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(OPEN_KEY, open));
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(open ? 0 : 1));
	}
}