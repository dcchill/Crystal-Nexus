package net.crystalnexus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** FE powered all-purpose mining tool. */
public final class SolarisItem extends CompoundPickaxeItem {
    private static final String SPEED_LEVEL = "SolarisSpeedLevel";
    private static final String RADIUS = "SolarisRadius";

    @Override
    public Component getName(ItemStack stack) {
        return GradientItemName.yellowToOrange(super.getName(stack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide()) {
            int next = speedLevel(stack) % 5 + 1;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SPEED_LEVEL, next));
            player.displayClientMessage(chargeStatus(stack), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static int speedLevel(ItemStack stack) {
        int stored = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(SPEED_LEVEL);
        return Math.max(1, Math.min(5, stored));
    }

    public static int radius(ItemStack stack) {
        int stored = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(RADIUS);
        return Math.max(0, Math.min(4, stored));
    }

    public static void adjustRadius(ServerPlayer player, int steps) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SolarisItem) || steps == 0) return;
        int next = Math.max(0, Math.min(4, radius(stack) + Integer.signum(steps)));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(RADIUS, next));
        player.displayClientMessage(chargeStatus(stack), true);
    }

    public static Component chargeStatus(ItemStack stack) {
        int size = radius(stack) * 2 + 1;
        return Component.literal("Solaris ").withStyle(ChatFormatting.GRAY)
            .append(MeteorSwordItem.chargeMeter(speedLevel(stack), false))
            .append(Component.literal("Level " + speedLevel(stack)).withStyle(ChatFormatting.RED))
            .append(Component.literal("  " + size + "x" + size).withStyle(ChatFormatting.GREEN));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int green = 136 + Math.round(119.0F * Math.min(ToolEnergy.CAPACITY, BatteryData.getEnergy(stack)) / ToolEnergy.CAPACITY);
        return 0xFF0000 | (green << 8);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return switch (speedLevel(stack)) {
            case 1 -> 9.0F;            // Netherite
            case 2 -> 10.8F;           // Netherite with Haste I
            case 3 -> 12.6F;           // Netherite with Haste II
            case 4 -> 36.0F;
            default -> Float.MAX_VALUE;
        };
    }
}
