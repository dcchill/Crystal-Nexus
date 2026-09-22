package net.crystalnexus.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.crystalnexus.reactor.ReactorBalance;

import java.util.List;

/** Per-cell reactor output, heat and lifetime. Other tiers can supply different values. */
public class ReactorFuelCellItem extends Item {
    private final double feMultiplier;
    private final double heatMultiplier;

    public ReactorFuelCellItem(int durability, double feMultiplier, double heatMultiplier) {
        super(new Item.Properties().durability(durability));
        this.feMultiplier = feMultiplier;
        this.heatMultiplier = heatMultiplier;
    }

    public double feMultiplier() { return feMultiplier; }
    public double heatMultiplier() { return heatMultiplier; }

    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Base output: " + Math.round(ReactorBalance.BASE_FE_PER_ROD_T * feMultiplier / 3) + " FE/t per cell"));
        lines.add(Component.literal("Base heat: " + ReactorBalance.BASE_HEAT_PER_ROD_T * heatMultiplier / 3 + " per tick"));
        lines.add(Component.literal("Fuel: " + (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage()));
    }
}
