package net.crystalnexus.client;

import net.crystalnexus.item.GeneratedSingularityItem;
import net.minecraft.world.item.ItemStack;
import java.awt.Color;

public final class GeneratedSingularityColors {
    private GeneratedSingularityColors() {}

    public static int tint(ItemStack singularity, int tintIndex) {
        if (tintIndex != 1) return 0xffffffff;
        int color = ResourceCometColors.colorFor(GeneratedSingularityItem.material(singularity));
        float[] hsb = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
        return Color.HSBtoRGB(hsb[0], Math.min(1.0f, hsb[1] * 1.35f),
            Math.min(1.0f, hsb[2] * 1.25f));
    }
}
