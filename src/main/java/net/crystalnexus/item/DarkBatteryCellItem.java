package net.crystalnexus.item;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class DarkBatteryCellItem extends Item {
    public static final int CAPACITY = 102_400_000;
    public static final int MAX_IO = 102_400;

    public DarkBatteryCellItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // --- tooltip / bar stuff (no capability code needed here) ---
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int energy = BatteryData.getEnergy(stack);
        return Math.round(13f * energy / (float) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xAA00AA ;
    }

@Override
public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    int energy = BatteryData.getEnergy(stack);
    tooltip.add(Component.literal("Energy: " + String.format("%,d", energy) + " / " + String.format("%,d", CAPACITY) + " FE").withStyle(ChatFormatting.GREEN));
}

}
