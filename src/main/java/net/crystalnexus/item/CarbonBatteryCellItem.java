package net.crystalnexus.item;

import net.crystalnexus.config.CrystalnexusConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class CarbonBatteryCellItem extends Item {
    public static int capacity() {
        return CrystalnexusConfig.ITEMS.CARBON_BATTERY_CELL.capacity();
    }

    public static int maxReceive() {
        return CrystalnexusConfig.ITEMS.CARBON_BATTERY_CELL.maxReceive();
    }

    public static int maxExtract() {
        return CrystalnexusConfig.ITEMS.CARBON_BATTERY_CELL.maxExtract();
    }

    public CarbonBatteryCellItem() {
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
        return Math.round(13f * Math.min(energy, capacity()) / (float) capacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FFAA;
    }

@Override
public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    int energy = BatteryData.getEnergy(stack);
    tooltip.add(Component.literal("Energy: " + String.format("%,d", Math.min(energy, capacity())) + " / " + String.format("%,d", capacity()) + " FE").withStyle(ChatFormatting.GREEN));
}

}
