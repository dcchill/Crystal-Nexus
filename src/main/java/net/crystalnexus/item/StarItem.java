package net.crystalnexus.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class StarItem extends Item {
    public StarItem(int durability) {
        super(new Item.Properties().durability(durability).fireResistant().setNoRepair());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) { return false; }
}
