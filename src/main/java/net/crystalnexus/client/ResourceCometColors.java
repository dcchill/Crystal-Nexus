package net.crystalnexus.client;

import net.crystalnexus.item.ResourceCometItem;
import net.crystalnexus.processing.SlurryColorMath;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sample the target item's texture, including textures supplied by resource packs. */
public final class ResourceCometColors {
    private static final Map<Item, Integer> CACHE = new ConcurrentHashMap<>();
    private ResourceCometColors() {}

    public static int tint(ItemStack comet, int tintIndex) {
        if (tintIndex != 0) return 0xffffffff;
        ItemStack material = ResourceCometItem.material(comet);
        if (material.isEmpty()) return 0xffffffff;
        return CACHE.computeIfAbsent(material.getItem(), ignored -> color(material));
    }

    private static int color(ItemStack material) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            var image = minecraft.getItemRenderer().getModel(material, minecraft.level, null, 0)
                .getParticleIcon().contents().getOriginalImage();
            return SlurryColorMath.averageAbgr(image.getPixelsRGBA(), 0xffffffff);
        } catch (RuntimeException ignored) {
            return 0xffffffff;
        }
    }

    public static void clear() { CACHE.clear(); }
}
