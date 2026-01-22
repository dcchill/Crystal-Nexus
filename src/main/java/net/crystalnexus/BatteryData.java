package net.crystalnexus.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class BatteryData {
    private static final String KEY = "Energy";

    public static int getEnergy(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return 0;
        return cd.copyTag().getInt(KEY);
    }

    public static void setEnergy(ItemStack stack, int energy, int capacity) {
        int clamped = Math.max(0, Math.min(capacity, energy));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(KEY, clamped));
    }
}
